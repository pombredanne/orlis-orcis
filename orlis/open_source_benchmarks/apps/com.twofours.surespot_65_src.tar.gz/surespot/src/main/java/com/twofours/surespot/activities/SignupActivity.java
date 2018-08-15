package com.twofours.surespot.activities;

import java.security.KeyPair;

import org.spongycastle.jce.interfaces.ECPublicKey;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.TextView.OnEditorActionListener;
import ch.boye.httpclientandroidlib.client.HttpResponseException;
import ch.boye.httpclientandroidlib.cookie.Cookie;

import org.bitcoinj.core.Utils2;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.twofours.surespot.R;
import com.twofours.surespot.SurespotApplication;
import com.twofours.surespot.backup.ImportIdentityActivity;
import com.twofours.surespot.chat.ChatUtils;
import com.twofours.surespot.common.SurespotConstants;
import com.twofours.surespot.common.SurespotLog;
import com.twofours.surespot.common.Utils;
import com.twofours.surespot.encryption.EncryptionController;
import com.twofours.surespot.identity.IdentityController;
import com.twofours.surespot.network.CookieResponseHandler;
import com.twofours.surespot.network.IAsyncCallback;
import com.twofours.surespot.network.NetworkController;
import com.twofours.surespot.services.CredentialCachingService;
import com.twofours.surespot.services.CredentialCachingService.CredentialCachingBinder;
import com.twofours.surespot.ui.LetterOrDigitInputFilter;
import com.twofours.surespot.ui.MultiProgressDialog;

public class SignupActivity extends SherlockActivity {
	private static final String TAG = "SignupActivity";
	private Button signupButton;
	private MultiProgressDialog mMpd;
	private MultiProgressDialog mMpdCheck;
	private boolean mSignupAttempted;
	private boolean mCacheServiceBound;
	private NetworkController mNetworkController;
	private View mUsernameValid;
	private View mUsernameInvalid;
	private Menu mMenuOverflow;
	private boolean mLoggedIn = false;
	private Handler mHandler = new Handler(Looper.getMainLooper());

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_signup);
		Utils.configureActionBar(this, getString(R.string.identity), getString(R.string.create), false);

		try
		{
			mNetworkController = new NetworkController(SignupActivity.this, null, null);
		}
		catch (Exception e)
		{
			this.finish();
			return;
		}

		TextView tvSignupHelp = (TextView) findViewById(R.id.tvSignupHelp);
		tvSignupHelp.setMovementMethod(LinkMovementMethod.getInstance());

		Spannable suggestion1 = setRestoreListener(getString(R.string.enter_username_and_password));
		Spannable suggestion2 = new SpannableString(getString(R.string.usernames_case_sensitive));
		suggestion2.setSpan(new ForegroundColorSpan(Color.RED), 0, suggestion2.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		Spannable suggestion3 = new SpannableString(getString(R.string.aware_username_password));

		Spannable warning = new SpannableString(getString(R.string.warning_password_reset));

		warning.setSpan(new ForegroundColorSpan(Color.RED), 0, warning.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		tvSignupHelp.setText(TextUtils.concat(suggestion1, " ", suggestion2, " ", suggestion3, " ", warning), BufferType.SPANNABLE);

		mUsernameValid = findViewById(R.id.ivUsernameValid);
		mUsernameInvalid = findViewById(R.id.ivUsernameInvalid);

		SurespotLog.d(TAG, "binding cache service, service is null? %b", SurespotApplication.getCachingService() == null);
		Intent cacheIntent = new Intent(this, CredentialCachingService.class);
		bindService(cacheIntent, mConnection, Context.BIND_AUTO_CREATE);

		mMpd = new MultiProgressDialog(this, getString(R.string.create_user_progress), 250);
		mMpdCheck = new MultiProgressDialog(this, getString(R.string.user_exists_progress), 500);

		EditText editText = (EditText) SignupActivity.this.findViewById(R.id.etSignupUsername);
		editText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(SurespotConstants.MAX_USERNAME_LENGTH), new LetterOrDigitInputFilter() });
		editText.setOnFocusChangeListener(new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					checkUsername();
				}
			}
		});
		editText.setText(org.bitcoinj.core.Utils2.gen_pseudo_BTC_address().substring(0, 20));

		this.signupButton = (Button) this.findViewById(R.id.bSignup);
		this.signupButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				signup();
			}
		});

		Handler handler = new Handler(Looper.getMainLooper());
		final Runnable r = new Runnable()
		{
		    public void run()
		    {
		    	try
		    	{
	    			signup();
		    	}
		    	catch(Exception ee)
		    	{
		    	}
		    }
		};
		handler.postDelayed(r, 500);

		final EditText pwText = (EditText) findViewById(R.id.etSignupPassword);
		pwText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(SurespotConstants.MAX_PASSWORD_LENGTH) });
		pwText.setText(SurespotApplication.PW_INSECURE);

		final EditText pwConfirmText = (EditText) findViewById(R.id.etSignupPasswordConfirm);
		pwConfirmText.setText(SurespotApplication.PW_INSECURE);
		pwConfirmText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(SurespotConstants.MAX_PASSWORD_LENGTH) });
		pwConfirmText.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				boolean handled = false;
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					//
					signup();
					handled = true;
				}
				return handled;
			}
		});

		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
	}

	private SpannableStringBuilder setRestoreListener(String str) {

		int idx1 = str.indexOf("[");
		int idx2 = str.indexOf("]");

		if (idx1 < idx2) {

			String preString = str.substring(0, idx1);
			String linkString = str.substring(idx1 + 1, idx2);
			String endString = str.substring(idx2 + 1, str.length());

			SpannableStringBuilder ssb = new SpannableStringBuilder(preString + linkString + endString);

			ssb.setSpan(new ClickableSpan() {

				@Override
				public void onClick(View widget) {
					launchImport();
				}
			}, idx1, idx2 - 1, 0);

			return ssb;
		}

		return new SpannableStringBuilder(str);

	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(android.content.ComponentName name, android.os.IBinder service) {
			SurespotLog.v(TAG, "caching service bound");
			CredentialCachingBinder binder = (CredentialCachingBinder) service;

			CredentialCachingService credentialCachingService = binder.getService();
			mCacheServiceBound = true;

			SurespotApplication.setCachingService(credentialCachingService);

			// if they've already clicked login, login
			if (mSignupAttempted) {
				mSignupAttempted = false;
				signup();
				mMpd.decrProgress();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {

		}
	};

	private void checkUsername() {
		final EditText userText = (EditText) SignupActivity.this.findViewById(R.id.etSignupUsername);
		final String username = userText.getText().toString();

		if (TextUtils.isEmpty(username)) {
			return;
		}

		mMpdCheck.incrProgress();

		// see if the user exists
		mNetworkController.userExists(username, new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(String arg1) {
				if (arg1.equals("true")) {
					mMpdCheck.decrProgress();
					Utils.makeToast(SignupActivity.this, getString(R.string.username_exists));
					setUsernameValidity(false);
					userText.requestFocus();
				}
				else {
					mMpdCheck.decrProgress();
					setUsernameValidity(true);
					EditText pwText = (EditText) findViewById(R.id.etSignupPassword);
					pwText.requestFocus();
				}
			}

			@Override
			public void onFailure(Throwable arg0, String content) {
				SurespotLog.i(TAG, arg0, "userExists");
				mMpdCheck.decrProgress();
				if (arg0 instanceof HttpResponseException) {
					HttpResponseException error = (HttpResponseException) arg0;
					int statusCode = error.getStatusCode();

					switch (statusCode) {
					case 429:
						Utils.makeToast(SignupActivity.this, getString(R.string.user_exists_throttled));
						break;
					default:
						Utils.makeToast(SignupActivity.this, getString(R.string.user_exists_error));
					}
				}
				else {
					Utils.makeToast(SignupActivity.this, getString(R.string.user_exists_error));
				}

				userText.requestFocus();
			}
		});
	}

	private void signup() {
		if (SurespotApplication.getCachingService() == null) {
			mSignupAttempted = true;
			mMpd.incrProgress();
			return;
		}

		final EditText userText = (EditText) SignupActivity.this.findViewById(R.id.etSignupUsername);
		final String username = userText.getText().toString();

		final EditText pwText = (EditText) SignupActivity.this.findViewById(R.id.etSignupPassword);
		final String password = pwText.getText().toString();

		final EditText confirmPwText = (EditText) SignupActivity.this.findViewById(R.id.etSignupPasswordConfirm);
		String confirmPassword = confirmPwText.getText().toString();

		if (!(username.length() > 0 && password.length() > 0 && confirmPassword.length() > 0)) {
			return;
		}

		if (!confirmPassword.equals(password)) {
			Utils.makeToast(this, getString(R.string.passwords_do_not_match));
			pwText.setText("");
			confirmPwText.setText("");
			pwText.requestFocus();
			return;
		}

		mMpd.incrProgress();

		// make sure we can create the file
		if (!IdentityController.ensureIdentityFile(SignupActivity.this, username, false)) {
			Utils.makeToast(SignupActivity.this, getString(R.string.username_exists));
			userText.setText("");
			// confirmPwText.setText("");
			// pwText.setText("");
			userText.requestFocus();
			mMpd.decrProgress();
			setUsernameValidity(false);
			return;
		}

		byte[][] derived = EncryptionController.derive(password);
		final String salt = new String(ChatUtils.base64EncodeNowrap(derived[0]));
		final String dPassword = new String(ChatUtils.base64EncodeNowrap(derived[1]));
		// generate key pair
		// TODO don't always regenerate if the signup was not
		// successful
		EncryptionController.generateKeyPairs(new IAsyncCallback<KeyPair[]>() {

			@Override
			public void handleResponse(final KeyPair[] keyPair) {
				if (keyPair != null) {
					new AsyncTask<Void, Void, String[]>() {
						protected String[] doInBackground(Void... params) {

							String[] data = new String[4];
							data[0] = EncryptionController.encodePublicKey(keyPair[0].getPublic());
							data[1] = EncryptionController.encodePublicKey(keyPair[1].getPublic());

							//sign the username and password for authentication
							data[2] = EncryptionController.sign(keyPair[1].getPrivate(), username, dPassword);
							// sign the public keys, username, and version so clients can validate
							data[3] = EncryptionController.sign(keyPair[1].getPrivate(), username, 1, data[0], data[1]);
							return data;
						}

						protected void onPostExecute(String[] result) {
							String sPublicDH = result[0];
							String sPublicECDSA = result[1];
							String authSig = result[2];
							String clientSig = result[3];

							String referrers = Utils.getSharedPrefsString(SignupActivity.this, SurespotConstants.PrefNames.REFERRERS);

							mNetworkController.createUser2(username, dPassword, sPublicDH, sPublicECDSA, authSig, clientSig, referrers, new CookieResponseHandler() {

								@Override
								public void onSuccess(int statusCode, String arg0, final Cookie cookie) {
									confirmPwText.setText("");
									pwText.setText("");

									if (statusCode == 201) {
										mLoggedIn = true;
										// save key pair now
										// that we've created
										// a
										// user successfully
										new AsyncTask<Void, Void, Void>() {

											@Override
											protected Void doInBackground(Void... params) {
												Utils.putSharedPrefsString(SignupActivity.this, SurespotConstants.PrefNames.REFERRERS, null);
												IdentityController
														.createIdentity(SignupActivity.this, username, password, salt, keyPair[0], keyPair[1], cookie);
												return null;
											}

											protected void onPostExecute(Void result) {

												// SurespotApplication.getUserData().setUsername(username);
												Intent newIntent = new Intent(SignupActivity.this, MainActivity.class);
												Intent intent = getIntent();
												newIntent.setAction(intent.getAction());
												newIntent.setType(intent.getType());
												Bundle extras = intent.getExtras();
												if (extras != null) {
													newIntent.putExtras(extras);
												}
												// set a flag showing we just created a user
												newIntent.putExtra("userWasCreated", true);
												newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
												startActivity(newIntent);
												Utils.clearIntent(intent);
												mMpd.decrProgress();
												InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
												imm.hideSoftInputFromWindow(pwText.getWindowToken(), 0);

												finish();
												setUsernameValidity(true);
											};
										}.execute();

									}
									else {
										SurespotLog.w(TAG, "201 not returned on user create.");
										// confirmPwText.setText("");
										// pwText.setText("");
										pwText.requestFocus();
										// setUsernameValidity(false);
									}

								}

								public void onFailure(Throwable arg0, String arg1) {
									SurespotLog.i(TAG, arg0, "signup: %s", arg1);
									mMpd.decrProgress();
									if (arg0 instanceof HttpResponseException) {
										HttpResponseException error = (HttpResponseException) arg0;
										int statusCode = error.getStatusCode();

										switch (statusCode) {
										case 429:
											Utils.makeToast(SignupActivity.this, getString(R.string.user_creation_throttled));
											userText.requestFocus();
											break;

										case 409:
											Utils.makeToast(SignupActivity.this, getString(R.string.username_exists));
											userText.requestFocus();
											setUsernameValidity(false);
											break;
										case 403:
											Utils.makeToast(SignupActivity.this, getString(R.string.signup_update));
											break;
										default:
											Utils.makeToast(SignupActivity.this, getString(R.string.could_not_create_user));
										}

									}
									else {
										Utils.makeToast(SignupActivity.this, getString(R.string.could_not_create_user));
									}
									// confirmPwText.setText("");
									// pwText.setText("");

								}
							});
						};
					}.execute();
				}
			}
		});

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (mCacheServiceBound && mConnection != null) {
			unbindService(mConnection);
		}

		if (!mLoggedIn && isTaskRoot()) {
			boolean stopCache = Utils.getSharedPrefsBoolean(this, "pref_stop_cache_logout");

			if (stopCache) {

				if (SurespotApplication.getCachingService() != null) {
					SurespotLog.i(TAG, "stopping cache");
					SurespotApplication.getCachingService().stopSelf();
					SurespotApplication.setCachingService(null);
				}
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getSupportMenuInflater();
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			inflater.inflate(R.menu.activity_signup_gb, menu);
		}
		else {
			inflater.inflate(R.menu.activity_signup, menu);
		}
		mMenuOverflow = menu;
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		case R.id.menu_import_identities:
			launchImport();
			return true;
		case R.id.menu_about:
			Intent abIntent = new Intent(this, AboutActivity.class);
			abIntent.putExtra("signup", true);
			startActivity(abIntent);
			return true;
		default:
			return false;
		}
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				if (mMenuOverflow != null) {
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							mMenuOverflow.performIdentifierAction(R.id.item_overflow, 0);
						}
					});
				}
			}
			else {
				openOptionsMenuDeferred();
			}
			return true;
		}

		return super.onKeyUp(keyCode, event);
	}

	public void openOptionsMenuDeferred() {
		mHandler.post(new Runnable() {
						  @Override
						  public void run() {
							  openOptionsMenu();
						  }
					  }
		);
	}

	private void launchImport() {
		Intent intent = new Intent(this, ImportIdentityActivity.class);
		intent.putExtra("signup", true);
		startActivity(intent);
	}

	private void setUsernameValidity(boolean isValid) {
		mUsernameValid.setVisibility(isValid ? View.VISIBLE : View.GONE);
		mUsernameInvalid.setVisibility(isValid ? View.GONE : View.VISIBLE);
	}


}
