package com.infonuascape.osrshelper;

import java.text.NumberFormat;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.infonuascape.osrshelper.hiscore.HiscoreHelper;
import com.infonuascape.osrshelper.utils.Skill;
import com.infonuascape.osrshelper.utils.Utils;
import com.infonuascape.osrshelper.utils.exceptions.PlayerNotFoundException;
import com.infonuascape.osrshelper.utils.players.PlayerSkills;
import com.infonuascape.osrshelper.views.RSViewPopulate;

public class HighScoreActivity extends Activity {
	private final static String EXTRA_USERNAME = "extra_username";
	private static final int NUM_PAGES = 2;
	private String username;
	private TextView header;
	private TextView combatText;
	private PlayerSkills playerSkills;
	private TableLayout rsView;
	private TableLayout table;
	private ArrayList<ImageView> dots;
	private ViewPager mViewPager;

	public static void show(final Context context, final String username) {
		Intent intent = new Intent(context, HighScoreActivity.class);
		intent.putExtra(EXTRA_USERNAME, username);
		context.startActivity(intent);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.hiscores);

		username = getIntent().getStringExtra(EXTRA_USERNAME);

		header = (TextView) findViewById(R.id.header);
		header.setText(getString(R.string.loading_highscores, username));
		
		combatText = (TextView) findViewById(R.id.combat);

		rsView = (TableLayout) findViewById(R.id.rs_view);
		table = (TableLayout) findViewById(R.id.table_hiscores);

		WizardPagerAdapter adapter = new WizardPagerAdapter();
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(adapter);
		addDots();

		new PopulateTable().execute();

	}

	private void changeHeaderText(final String text) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				findViewById(R.id.progressbar).setVisibility(View.GONE);
				header.setText(text);
			}
		});
	}
	
	private void changeCombatText(){
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				combatText.setVisibility(View.VISIBLE);
				combatText.setText(getString(R.string.combat_lvl, Utils.getCombatLvl(playerSkills)));
			}
		});
	}

	private class PopulateTable extends AsyncTask<String, Void, PlayerSkills> {

		@Override
		protected PlayerSkills doInBackground(String... urls) {
			HiscoreHelper hiscoreHelper = new HiscoreHelper();
			hiscoreHelper.setUserName(username);
			PlayerSkills playerSkills = null;

			try {
				playerSkills = hiscoreHelper.getPlayerStats();
			} catch (PlayerNotFoundException e) {
				changeHeaderText(getString(R.string.not_existing_player, username));

			} catch (Exception uhe) {
				uhe.printStackTrace();
				changeHeaderText(getString(R.string.network_error));
			}
			return playerSkills;
		}

		@Override
		protected void onPostExecute(PlayerSkills playerSkillsCallback) {
			if (playerSkillsCallback != null) {
				playerSkills = playerSkillsCallback;
				populateTable(playerSkills);
			}
		}
	}

	private void populateTable(PlayerSkills playerSkills) {
		changeHeaderText(getString(R.string.showing_results, username));
		changeCombatText();

		ArrayList<Skill> skills = PlayerSkills.getSkillsInOrder(playerSkills);
		for (Skill skill : skills) {
			table.addView(createRow(skill));
		}

		RSViewPopulate rsViewPopulate = new RSViewPopulate(this, PlayerSkills.getSkillsInOrderForRSView(playerSkills));
		rsView = rsViewPopulate.populate(rsView);
	}

	private TableRow createRow(Skill skill) {
		TableRow tableRow = new TableRow(this);
		TableRow.LayoutParams params = new TableRow.LayoutParams();
		params.weight = 1;
		params.width = 0;
		params.topMargin = 10;
		params.bottomMargin = 10;
		params.gravity = Gravity.CENTER;

		// Skill image
		ImageView image = new ImageView(this);
		image.setImageResource(skill.getDrawableInt());
		image.setLayoutParams(params);
		tableRow.addView(image);
		

		// Lvl
		TextView text = new TextView(this);
		if (skill.getRank() != -1) {
			text.setText(skill.getLevel() + "");
		}
		text.setLayoutParams(params);
		text.setGravity(Gravity.CENTER);
		text.setTextColor(getResources().getColor(R.color.text_normal));
		tableRow.addView(text);

		// XP
		text = new TextView(this);
		if (skill.getRank() != -1) {
			text.setText(NumberFormat.getInstance().format(skill.getExperience()));
		}
		
		text.setLayoutParams(params);
		text.setGravity(Gravity.CENTER);
		text.setTextColor(getResources().getColor(R.color.text_normal));
		tableRow.addView(text);

		// Ranking
		text = new TextView(this);

		if (skill.getRank() != -1) {
			text.setText(NumberFormat.getInstance().format(skill.getRank()));
			text.setTextColor(getResources().getColor(R.color.text_normal));
		} else {
			text.setText(getString(R.string.not_ranked));
			text.setTextColor(getResources().getColor(R.color.Red));
		}

		text.setLayoutParams(params);
		text.setGravity(Gravity.CENTER);
		tableRow.addView(text);

		return tableRow;
	}

	public void addDots() {
		dots = new ArrayList<ImageView>();
		LinearLayout dotsLayout = (LinearLayout)findViewById(R.id.dots);

		for(int i = 0; i < NUM_PAGES; i++) {
			ImageView dot = new ImageView(this);
			dot.setImageDrawable(getResources().getDrawable(R.drawable.pager_dot_not_selected));

			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT
			);
			dotsLayout.addView(dot, params);

			dots.add(dot);
		}

		mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			}

			@Override
			public void onPageSelected(int position) {
				selectDot(position);
			}

			@Override
			public void onPageScrollStateChanged(int state) {
			}
		});

		selectDot(0);
	}

	public void selectDot(int idx) {
		Resources res = getResources();
		for(int i = 0; i < NUM_PAGES; i++) {
			int drawableId = (i==idx)?(R.drawable.pager_dot_selected):(R.drawable.pager_dot_not_selected);
			Drawable drawable = res.getDrawable(drawableId);
			dots.get(i).setImageDrawable(drawable);
		}
	}


	class WizardPagerAdapter extends PagerAdapter {

		public Object instantiateItem(ViewGroup collection, int position) {

			int resId = 0;
			switch (position) {
				case 0:
					resId = R.id.scroll_table;
					break;
				case 1:
					resId = R.id.scroll_table_rs_view;
					break;
			}
			return findViewById(resId);
		}

		@Override
		public int getCount() {
			return 2;
		}

		@Override
		public boolean isViewFromObject(View arg0, Object arg1) {
			return arg0 == ((View) arg1);
		}
	}
}
