package at.linuxtage.companion.api;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import at.linuxtage.companion.db.DatabaseManager;
import at.linuxtage.companion.model.Event;
import at.linuxtage.companion.parsers.EventsParser;
import at.linuxtage.companion.utils.HttpUtils;

/**
 * Main API entry point.
 * 
 * @author Christophe Beyls
 * 
 */
public class GLTApi {

	// Local broadcasts parameters
	public static final String ACTION_DOWNLOAD_SCHEDULE_PROGRESS = "at.linuxtage.glt.action.DOWNLOAD_SCHEDULE_PROGRESS";
	public static final String EXTRA_PROGRESS = "PROGRESS";
	public static final String ACTION_DOWNLOAD_SCHEDULE_RESULT = "at.linuxtage.glt.action.DOWNLOAD_SCHEDULE_RESULT";
	public static final String EXTRA_RESULT = "RESULT";

	public static final int RESULT_ERROR = -1;
	public static final int RESULT_UP_TO_DATE = -2;

	private static final Lock scheduleLock = new ReentrantLock();

	/**
	 * Download & store the schedule to the database. Only one thread at a time will perform the actual action, the other ones will return immediately. The
	 * result will be sent back in the form of a local broadcast with an ACTION_DOWNLOAD_SCHEDULE_RESULT action.
	 * 
	 */
	public static void downloadSchedule(Context context) {
		if (!scheduleLock.tryLock()) {
			// If a download is already in progress, return immediately
			return;
		}

		int result = RESULT_ERROR;
		try {
			DatabaseManager dbManager = DatabaseManager.getInstance();
			HttpUtils.HttpResult httpResult = HttpUtils.get(
					context,
					GLTUrls.getSchedule(DatabaseManager.getInstance().getYear()),
					dbManager.getLastModifiedTag(),
					ACTION_DOWNLOAD_SCHEDULE_PROGRESS,
					EXTRA_PROGRESS);
			if (httpResult.inputStream == null) {
				// Nothing to parse, the result is up-to-date.
				result = RESULT_UP_TO_DATE;
				return;
			}

			try {
				Iterable<Event> events = new EventsParser().parse(httpResult.inputStream);
				result = dbManager.storeSchedule(events, httpResult.lastModified);
			} finally {
				try {
					httpResult.inputStream.close();
				} catch (Exception ignored) {
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION_DOWNLOAD_SCHEDULE_RESULT).putExtra(EXTRA_RESULT, result));
			scheduleLock.unlock();
		}
	}
}
