package com.twofours.surespot.identity;

import java.security.PrivateKey;
import java.util.List;

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.twofours.surespot.R;
import com.twofours.surespot.SurespotApplication;
import com.twofours.surespot.activities.MainActivity;
import com.twofours.surespot.chat.ChatUtils;
import com.twofours.surespot.common.SurespotLog;
import com.twofours.surespot.common.Utils;
import com.twofours.surespot.encryption.EncryptionController;
import com.twofours.surespot.network.IAsyncCallback;
import com.twofours.surespot.ui.MultiProgressDialog;
import com.twofours.surespot.ui.UIUtils;

public class DeleteIdentityActivity extends SherlockActivity {
	private static final String TAG = null;
	private List<String> mIdentityNames;
	private Spinner mSpinner;
	private MultiProgressDialog mMpd;
	private AlertDialog mDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_delete_identity);
		Utils.configureActionBar(this, getString(R.string.identity), getString(R.string.delete), true);

		mMpd = new MultiProgressDialog(this, getString(R.string.delete_identity_progress), 250);

		Button deleteIdentityButton = (Button) findViewById(R.id.bDeleteIdentity);
		mSpinner = (Spinner) findViewById(R.id.identitySpinner);
		refreshSpinner();

		deleteIdentityButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v)
			{
				final String user = (String) mSpinner.getSelectedItem();
				deleteIdentity(user, SurespotApplication.PW_INSECURE);

			}
		});
	}

	private void refreshSpinner() {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.sherlock_spinner_item);
		adapter.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
		mIdentityNames = IdentityController.getIdentityNames(this);

		for (String name : mIdentityNames) {
			adapter.add(name);
		}

		mSpinner.setAdapter(adapter);
		String loggedInUser = IdentityController.getLoggedInUser();
		if (loggedInUser != null) {
			mSpinner.setSelection(adapter.getPosition(loggedInUser));
		}

	}

	private void deleteIdentity(final String username, final String password) {

		mMpd.incrProgress();
		SurespotIdentity identity = IdentityController.getIdentity(this, username, password);

		if (identity == null) {
			mMpd.decrProgress();
			Utils.makeLongToast(DeleteIdentityActivity.this, getString(R.string.could_not_delete_identity));
			return;
		}

		final String version = identity.getLatestVersion();
		final PrivateKey pk = identity.getKeyPairDSA().getPrivate();

		// create auth sig
		byte[] saltBytes = ChatUtils.base64DecodeNowrap(identity.getSalt());
		final String dPassword = new String(ChatUtils.base64EncodeNowrap(EncryptionController.derive(password, saltBytes)));
		final String authSignature = EncryptionController.sign(pk, username, dPassword);
		SurespotLog.v(TAG, "generatedAuthSig: " + authSignature);

		// get a key update token from the server
		MainActivity.getNetworkController().getDeleteToken(username, dPassword, authSignature, new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(int statusCode, final String deleteToken) {

				new AsyncTask<Void, Void, DeleteIdentityWrapper>() {
					@Override
					protected DeleteIdentityWrapper doInBackground(Void... params) {
						SurespotLog.v(TAG, "received delete token: " + deleteToken);

						// create token sig
						final String tokenSignature = EncryptionController.sign(pk, ChatUtils.base64DecodeNowrap(deleteToken),
								dPassword.getBytes());

						SurespotLog.v(TAG, "generatedTokenSig: " + tokenSignature);

						return new DeleteIdentityWrapper(tokenSignature, authSignature, version);
					}

					protected void onPostExecute(final DeleteIdentityWrapper result) {
						if (result != null) {
							// upload all this crap to the server
							MainActivity.getNetworkController().deleteUser(username, dPassword, result.authSig, result.tokenSig,
									result.keyVersion, new AsyncHttpResponseHandler() {
										public void onSuccess(int statusCode, String content) {
											// delete the identity stuff localally
											IdentityController.deleteIdentity(DeleteIdentityActivity.this, username, false);
											refreshSpinner();
											mMpd.decrProgress();
											Utils.makeLongToast(DeleteIdentityActivity.this, getString(R.string.identity_deleted));
										};

										@Override
										public void onFailure(Throwable error, String content) {
											SurespotLog.i(TAG, error, "deleteIdentity");
											mMpd.decrProgress();
											Utils.makeLongToast(DeleteIdentityActivity.this, getString(R.string.could_not_delete_identity));

										}
									});
						}
						else {
							mMpd.decrProgress();
							Utils.makeLongToast(DeleteIdentityActivity.this, getString(R.string.could_not_delete_identity));
						}

					};
				}.execute();

			}

			@Override
			public void onFailure(Throwable error, String content) {
				mMpd.decrProgress();
				Utils.makeLongToast(DeleteIdentityActivity.this, getString(R.string.could_not_delete_identity));

			}
		});
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();

			return true;
		default:
			return super.onOptionsItemSelected(item);
		}

	}

	private class DeleteIdentityWrapper {

		public String tokenSig;
		public String authSig;
		public String keyVersion;

		public DeleteIdentityWrapper(String tokenSig, String authSig, String keyVersion) {
			super();
			this.tokenSig = tokenSig;
			this.authSig = authSig;
			this.keyVersion = keyVersion;
		}

	}
	
	@Override
	public void onPause() {		
		super.onPause();
		if (mDialog != null && mDialog.isShowing()) {
			mDialog.dismiss();		
		}
	}

}
