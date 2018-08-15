package at.linuxtage.companion.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.Locale;

import at.linuxtage.companion.R;

public class MapFragment extends Fragment {

	private static final double DESTINATION_LATITUDE = 47.06914;
	private static final double DESTINATION_LONGITUDE = 15.41001;
	private static final String DESTINATION_NAME = "FH Joanneum";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_map, container, false);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.map, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.directions:
				launchDirections();
				return true;
		}
		return false;
	}

	private void launchDirections() {
		// Build intent to start Google Maps directions
		String uri = String.format(Locale.US,
				//"http://maps.google.com/maps?f=d&daddr=%1$f,%2$f(%3$s)&dirflg=r",
                "http://www.openstreetmap.org/#map=17/%1$f/%2$f/%3$s",
                DESTINATION_LATITUDE, DESTINATION_LONGITUDE, DESTINATION_NAME);

		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));

		startActivity(intent);


    }
}
