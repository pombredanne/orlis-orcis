package com.horaapps.leafpic;

import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;

/**
 * Created by Jibo on 29/01/2016.
 */
public class IntroActivity extends AppIntro {
    int slide=1;
    BitmapDrawable drawable;
    @Override
    public void init(Bundle savedInstanceState) {

        addSlide(AppIntroFragment.newInstance(this.getString(R.string.welcome_slide_title), this.getString(R.string.welcome_slide_sub),
                R.drawable.leaf_pic, ContextCompat.getColor(this, R.color.accent_teal)));

       /* addSlide(AppIntroFragment.newInstance(this.getString(R.string.storage_slide_title), this.getString(R.string.storage_slide_sub),
                R.drawable.storage_permission, ContextCompat.getColor(this, R.color.accent_brown)));//Color.parseColor("#607D8B")*/

        setBarColor(ContextCompat.getColor(this, R.color.accent_teal));
        setSeparatorColor(ContextCompat.getColor(this, R.color.accent_teal));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(ContextCompat.getColor(getApplicationContext(), R.color.accent_teal));


        drawable = ((BitmapDrawable) getDrawable(R.mipmap.ic_launcher));
        setTaskDescription(new ActivityManager.TaskDescription
                (getString(R.string.app_name), drawable.getBitmap(),
                        ContextCompat.getColor(this, R.color.accent_teal)));
        }
        //MAYBE REMOVE
        showSkipButton(true);

    }

    private void loadMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void onSkipPressed() {
        System.exit(1);
        loadMainActivity();
    }

    @Override
    public void onNextPressed() {

    }

    @Override
    public void onDonePressed() {
        System.exit(1);
        loadMainActivity();
    }

    @Override
    public void onSlideChanged() {
        if (slide==1) {
            slide=2;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setNavigationBarColor(ContextCompat.getColor(getApplicationContext(), R.color.accent_teal));
            }
            setSeparatorColor(ContextCompat.getColor(this, R.color.accent_teal));
            setBarColor(ContextCompat.getColor(this, R.color.accent_teal));
            //drawable = ((BitmapDrawable) ContextCompat.getDrawable(R.mipmap.ic_launcher));


        } else{
            /* TODO: NOT WORK
            if (ContextCompat.checkSelfPermission(IntroActivity.this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                askForPermissions(new String[]{Manifest.permission.CAMERA}, 2);
            }
            */
            slide=1;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setNavigationBarColor(ContextCompat.getColor(getApplicationContext(), R.color.accent_brown));
            }
            setSeparatorColor(ContextCompat.getColor(this, R.color.accent_brown));
            setBarColor(ContextCompat.getColor(this, R.color.accent_brown));
        }
    }

    public void getStarted(View v) {
        loadMainActivity();
    }
}