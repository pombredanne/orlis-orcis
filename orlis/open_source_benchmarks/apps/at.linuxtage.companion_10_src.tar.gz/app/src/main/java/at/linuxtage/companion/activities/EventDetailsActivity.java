package at.linuxtage.companion.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.widget.Toast;

import at.linuxtage.companion.R;
import at.linuxtage.companion.db.DatabaseManager;
import at.linuxtage.companion.fragments.EventDetailsFragment;
import at.linuxtage.companion.loaders.LocalCacheLoader;
import at.linuxtage.companion.model.Event;
import at.linuxtage.companion.utils.NfcUtils;
import at.linuxtage.companion.utils.NfcUtils.CreateNfcAppDataCallback;

/**
 * Displays a single event passed either as a complete Parcelable object in extras or as an id in data.
 *
 * @author Christophe Beyls
 */
public class EventDetailsActivity extends ActionBarActivity implements LoaderCallbacks<Event>, CreateNfcAppDataCallback {

	public static final String EXTRA_EVENT = "event";

	private static final int EVENT_LOADER_ID = 1;

	private Event event;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.content);

		ActionBar bar = getSupportActionBar();
		bar.setDisplayHomeAsUpEnabled(true);
		bar.setDisplayShowTitleEnabled(false);

		Event event = getIntent().getParcelableExtra(EXTRA_EVENT);

		if (event != null) {
			// The event has been passed as parameter, it can be displayed immediately
			initEvent(event);
			if (savedInstanceState == null) {
				Fragment f = EventDetailsFragment.newInstance(event);
				getSupportFragmentManager().beginTransaction().add(R.id.content, f).commit();
			}
		} else {
			// Load the event from the DB using its id
			getSupportLoaderManager().initLoader(EVENT_LOADER_ID, null, this);
		}
	}

	/**
	 * Initialize event-related configuration after the event has been loaded.
	 */
	private void initEvent(Event event) {
		this.event = event;
		// Enable up navigation only after getting the event details
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		// Enable Android Beam
		NfcUtils.setAppDataPushMessageCallbackIfAvailable(this, this);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				// Navigate up to the track associated with this event
				Intent upIntent = new Intent(this, TrackScheduleActivity.class);
				upIntent.putExtra(TrackScheduleActivity.EXTRA_DAY, event.getDay());
				upIntent.putExtra(TrackScheduleActivity.EXTRA_TRACK, event.getTrack());
				upIntent.putExtra(TrackScheduleActivity.EXTRA_FROM_EVENT_ID, event.getId());

				if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
					TaskStackBuilder.create(this)
							.addNextIntentWithParentStack(upIntent)
							.startActivities();
					finish();
				} else {
					// Replicate the compatibility implementation of NavUtils.navigateUpTo()
					// to ensure the parent Activity is always launched
					// even if not present on the back stack.
					upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					startActivity(upIntent);
					finish();
				}
				return true;
		}
		return false;
	}

	@Override
	public byte[] createNfcAppData() {
		return String.valueOf(event.getId()).getBytes();
	}

	private static class EventLoader extends LocalCacheLoader<Event> {

		private final long eventId;

		public EventLoader(Context context, long eventId) {
			super(context);
			this.eventId = eventId;
		}

		@Override
		public Event loadInBackground() {
			return DatabaseManager.getInstance().getEvent(eventId);
		}
	}

	@Override
	public Loader<Event> onCreateLoader(int id, Bundle args) {
		Intent intent = getIntent();
		String eventIdString;
		if (NfcUtils.hasAppData(intent)) {
			// NFC intent
			eventIdString = new String(NfcUtils.extractAppData(intent));
		} else {
			// Normal in-app intent
			eventIdString = intent.getDataString();
		}
		return new EventLoader(this, Long.parseLong(eventIdString));
	}

	@Override
	public void onLoadFinished(Loader<Event> loader, Event data) {
		if (data == null) {
			// Event not found, quit
			Toast.makeText(this, getString(R.string.event_not_found_error), Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		initEvent(data);

		FragmentManager fm = getSupportFragmentManager();
		if (fm.findFragmentById(R.id.content) == null) {
			Fragment f = EventDetailsFragment.newInstance(data);
			fm.beginTransaction().add(R.id.content, f).commitAllowingStateLoss();
		}
	}

	@Override
	public void onLoaderReset(Loader<Event> loader) {
	}
}
