package com.twofours.surespot.activities;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.twofours.surespot.R;
import com.twofours.surespot.SurespotApplication;
import com.twofours.surespot.backup.ExportIdentityActivity;
import com.twofours.surespot.chat.ChatUtils;
import com.twofours.surespot.common.SurespotLog;
import com.twofours.surespot.common.Utils;
import com.twofours.surespot.encryption.EncryptionController;
import com.twofours.surespot.identity.IdentityController;
import com.twofours.surespot.identity.SurespotIdentity;
import com.twofours.surespot.network.IAsyncCallback;
import com.twofours.surespot.ui.MultiProgressDialog;
import com.twofours.surespot.ui.UIUtils;

public class ManageKeysActivity extends SherlockActivity
{
	private static final String TAG = "ManageKeysActivity";
	private List<String> mIdentityNames;
	private MultiProgressDialog mMpd;
	private AlertDialog mDialog;
	public static boolean just_roll_keys = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_manage_keys);
		Utils.configureActionBar(this, getString(R.string.identity), getString(R.string.keys), true);
		mMpd = new MultiProgressDialog(this, getString(R.string.generating_keys_progress), 750);

		TextView tvBackupWarning = (TextView) findViewById(R.id.newKeysBackup);

		Spannable warning = new SpannableString(getString(R.string.backup_identities_again_keys));

		warning.setSpan(new ForegroundColorSpan(Color.RED), 0, warning.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		tvBackupWarning.setText(TextUtils.concat(warning));

		tvBackupWarning.setVisibility(View.INVISIBLE);

		final Spinner spinner = (Spinner) findViewById(R.id.identitySpinner);

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.sherlock_spinner_item);
		adapter.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
		mIdentityNames = IdentityController.getIdentityNames(this);

		for (String name : mIdentityNames) {
			adapter.add(name);
		}

		spinner.setAdapter(adapter);
		spinner.setSelection(adapter.getPosition(IdentityController.getLoggedInUser()));

		Button rollKeysButton = (Button) findViewById(R.id.bRollKeys);
		rollKeysButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				final String user = (String) spinner.getSelectedItem();

				// make sure file we're going to save to is writable before we start
				if (!IdentityController.ensureIdentityFile(ManageKeysActivity.this, user, true)) {
					Utils.makeToast(ManageKeysActivity.this, getString(R.string.could_not_create_new_keys));
					return;
				}

				rollKeys(user, SurespotApplication.PW_INSECURE);
			}
		});
		
		if (just_roll_keys)
		{
			// System.out.println("ROLLK:001");
			just_roll_keys = false;

			rollKeysButton.setEnabled(false);
			rollKeysButton.setText("automatic mode,\nplease wait ...");

			final String user = (String) spinner.getSelectedItem();

			Thread thread = new Thread()
			{
			    @Override
			    public void run()
			    {
			        try
			        {
			            Thread.sleep(1200);
			        }
			        catch (InterruptedException e)
			        {
			        }
			
			        ManageKeysActivity.this.runOnUiThread(new Runnable()
			        {
			            @Override
			            public void run()
			            {
					// System.out.println("ROLLK:003");
					rollKeys(user, SurespotApplication.PW_INSECURE);
					// System.out.println("ROLLK:004");
			            }
			        });
			        
			    }
			};
			thread.start();

			Thread thread2 = new Thread()
			{
			    @Override
			    public void run()
			    {
			        try
			        {
			            Thread.sleep(9000);
			        }
			        catch (InterruptedException e)
			        {
			        }
			
			        ManageKeysActivity.this.runOnUiThread(new Runnable()
			        {
			            @Override
			            public void run()
			            {
					finish();
					//System.out.println("ROLLK:004a");
			            }
			        });
			        
			    }
			};
			thread2.start();

			//System.out.println("ROLLK:005");
		}
	}

	private class RollKeysWrapper
	{

		public String tokenSig;
		public String authSig;
		public String keyVersion;
		public String clientSig;
		public KeyPair[] keyPairs;

		public RollKeysWrapper(KeyPair[] keyPairs, String tokenSig, String authSig, String keyVersion, String clientSig) {
			super();
			this.keyPairs = keyPairs;
			this.tokenSig = tokenSig;
			this.authSig = authSig;
			this.keyVersion = keyVersion;
			this.clientSig = clientSig;
		}

	}

	private void rollKeys(final String username, final String password) {

		mMpd.incrProgress();
		final SurespotIdentity identity = IdentityController.getIdentity(this, username, password);

		if (identity == null) {
			mMpd.decrProgress();
			Utils.makeLongToast(ManageKeysActivity.this, getString(R.string.could_not_create_new_keys));
			return;
		}

		final PrivateKey latestPk = identity.getKeyPairDSA().getPrivate();

		// create auth sig
		byte[] saltBytes = ChatUtils.base64DecodeNowrap(identity.getSalt());
		final String dPassword = new String(ChatUtils.base64EncodeNowrap(EncryptionController.derive(password, saltBytes)));
		final String authSignature = EncryptionController.sign(latestPk, username, dPassword);
		SurespotLog.v(TAG, "generatedAuthSig: " + authSignature);

		// get a key update token from the server
		MainActivity.getNetworkController().getKeyToken(username, dPassword, authSignature, new JsonHttpResponseHandler() {
			@Override
			public void onSuccess(int statusCode, final JSONObject response) {

				new AsyncTask<Void, Void, RollKeysWrapper>() {
					@Override
					protected RollKeysWrapper doInBackground(Void... params) {
						String keyToken = null;
						String keyVersion = null;

						try {
							keyToken = response.getString("token");
							SurespotLog.v(TAG, "received key token: " + keyToken);
							keyVersion = response.getString("keyversion");
						} catch (JSONException e) {
							return null;
						}

						// create token sig
						final String tokenSignature = EncryptionController.sign(latestPk, ChatUtils.base64DecodeNowrap(keyToken),
								dPassword.getBytes());

						SurespotLog.v(TAG, "generatedTokenSig: " + tokenSignature);
						// generate new key pairs
						KeyPair[] keys = EncryptionController.generateKeyPairsSync();
						if (keys == null) {
							return null;
						}

						//sign new key with old key
						String clientSig = EncryptionController.sign(latestPk, username, Integer.parseInt(keyVersion, 10), EncryptionController.encodePublicKey(keys[0].getPublic()), EncryptionController.encodePublicKey(keys[1].getPublic()));

						return new RollKeysWrapper(keys, tokenSignature, authSignature, keyVersion, clientSig);
					}

					protected void onPostExecute(final RollKeysWrapper result) {
						if (result != null) {
							// upload all this crap to the server
							MainActivity.getNetworkController().updateKeys(username, dPassword,
									EncryptionController.encodePublicKey(result.keyPairs[0].getPublic()),
									EncryptionController.encodePublicKey(result.keyPairs[1].getPublic()), result.authSig, result.tokenSig,
									result.keyVersion, result.clientSig,  new AsyncHttpResponseHandler() {
										public void onSuccess(int statusCode, String content)
										{
											// save the key pairs
											IdentityController.rollKeys(ManageKeysActivity.this, identity, username, password, result.keyVersion,
													result.keyPairs[0], result.keyPairs[1]);
											mMpd.decrProgress();
											Utils.makeLongToast(ManageKeysActivity.this, getString(R.string.keys_created));
											
											// ---- disable automatic showing of the export-keys screen!! ----
											// ---- disable automatic showing of the export-keys screen!! ----
											// Intent intent = new Intent(ManageKeysActivity.this, ExportIdentityActivity.class);
											// intent.putExtra("backupUsername", username);
											// ManageKeysActivity.this.startActivity(intent);
											// ---- disable automatic showing of the export-keys screen!! ----
											// ---- disable automatic showing of the export-keys screen!! ----
										};

										@Override
										public void onFailure(Throwable error, String content)
										{
											SurespotLog.i(TAG, error, "rollKeys");
											mMpd.decrProgress();
											Utils.makeLongToast(ManageKeysActivity.this, getString(R.string.could_not_create_new_keys));

										}
									});
						}
						else
						{
							mMpd.decrProgress();
							Utils.makeLongToast(ManageKeysActivity.this, getString(R.string.could_not_create_new_keys));
						}

					};

				}.execute();

			}

			@Override
			public void onFailure(Throwable error, String content)
			{
				mMpd.decrProgress();
				Utils.makeLongToast(ManageKeysActivity.this, getString(R.string.could_not_create_new_keys));

			}
		});
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId())
		{
		case android.R.id.home:
			finish();

			return true;
		default:
			return super.onOptionsItemSelected(item);
		}

	}
	
	@Override
	public void onPause()
	{		
		super.onPause();
		if (mDialog != null && mDialog.isShowing()) {
			mDialog.dismiss();		
		}
	}
}
