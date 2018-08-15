package com.einmalfel.podlisten;

import android.app.Application;
import android.util.Log;

import com.facebook.stetho.Stetho;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

public class DebuggableApp extends Application {
  private static final String TAG = "PLA";

  RefWatcher refWatcher;

  public void onCreate() {
    Log.i(TAG, "Initializing stetho...");
    Stetho.initialize(Stetho.newInitializerBuilder(this).enableDumpapp(Stetho
        .defaultDumperPluginsProvider(this)).enableWebKitInspector(Stetho
        .defaultInspectorModulesProvider(this)).build());
    Log.i(TAG, "Initializing leakcanary...");
    refWatcher = LeakCanary.install(this);
    super.onCreate();
  }
}
