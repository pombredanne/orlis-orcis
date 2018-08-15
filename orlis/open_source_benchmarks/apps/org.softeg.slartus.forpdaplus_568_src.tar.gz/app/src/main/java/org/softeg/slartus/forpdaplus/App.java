package org.softeg.slartus.forpdaplus;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.View;

import com.nostra13.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.UsingFreqLimitedMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.softeg.slartus.forpdacommon.ExtPreferences;
import org.softeg.slartus.forpdanotifyservice.MainService;
import org.softeg.slartus.forpdanotifyservice.favorites.FavoritesNotifier;
import org.softeg.slartus.forpdanotifyservice.qms.QmsNotifier;
import org.softeg.slartus.forpdaplus.classes.common.ArrayUtils;
import org.softeg.slartus.forpdaplus.db.DbHelper;
import org.softeg.slartus.forpdaplus.prefs.PreferencesActivity;
import org.softeg.slartus.forpdaplus.tabs.TabItem;
import org.softeg.slartus.forpdaplus.utils.HttpHelperForImage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: slinkin
 * Date: 05.08.11
 * Time: 8:03
 */
@ReportsCrashes(
        mailTo = "ololosh10050@gmail.com,slartus@gmail.com,devuicore@gmail.com",
        mode = ReportingInteractionMode.DIALOG,
        customReportContent = {ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME,
                ReportField.ANDROID_VERSION, ReportField.PHONE_MODEL,
                ReportField.CUSTOM_DATA, ReportField.STACK_TRACE, ReportField.LOGCAT},
        resToastText = R.string.crash_toast_text, // optional, displayed as soon as the crash occurs, before collecting data which can take a few seconds
        resDialogText = R.string.crash_dialog_text,
        resDialogIcon = android.R.drawable.ic_dialog_info, //optional. default is a warning sign
        resDialogTitle = R.string.crash_dialog_title, // optional. default is your application name
        resDialogCommentPrompt = R.string.crash_dialog_comment_prompt, // optional. when defined, adds a user text field input with this text resource as a label
        resDialogOkToast = R.string.crash_dialog_ok_toast // optional. displays a Toast message when the user accepts to send a report.)
)
public class App extends android.app.Application {
    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_BLACK = 6;

    public static final int THEME_MATERIAL_LIGHT = 2;
    public static final int THEME_MATERIAL_DARK = 3;
    public static final int THEME_MATERIAL_BLACK = 5;

    public static final int THEME_LIGHT_OLD_HD = 4;

    public static final int THEME_CUSTOM_CSS = 99;

    private final Integer[] LIGHT_THEMES = {THEME_LIGHT, THEME_LIGHT_OLD_HD, THEME_MATERIAL_LIGHT};
    private final Integer[] DARK_THEMES = {THEME_MATERIAL_DARK, THEME_DARK};


    public static final int THEME_TYPE_LIGHT = 0;
    public static final int THEME_TYPE_DARK = 2;
    public static final int THEME_TYPE_BLACK = 3;

    private String currentFragmentTag;

    private int tabIterator = 0;

    public int getTabIterator(){
        return tabIterator;
    }

    public void setTabIterator(int tabIterator) {
        this.tabIterator = tabIterator;
    }
    public void clearTabIterator(){
        tabIterator = 0;
    }
    public void plusTabIterator() {
        tabIterator++;
    }

    public String getCurrentFragmentTag(){
        return currentFragmentTag;
    }
    public void setCurrentFragmentTag(String s){
        currentFragmentTag = s;
    }

    private List<TabItem> mTabItems = new ArrayList<>();

    public void setmTabItems(List<TabItem> mTabItems) {
        this.mTabItems = mTabItems;
    }
    public List<TabItem> getTabItems(){
        return mTabItems;
    }
    public int getLastTabPosition(int delPos){
        if((mTabItems.size()-1)<delPos) delPos--;
        return delPos;
    }

    public boolean isContainsByTag(String tag){
        for(TabItem item:getTabItems())
            if(item.getTag().equals(tag)) return true;
        return false;
    }
    public boolean isContainsByUrl(String url){
        for(TabItem item:getTabItems())
            if(item.getUrl().equals(url)) return true;
        return false;
    }

    public TabItem getTabByTag(String tag){
        for(TabItem item:getTabItems())
            if(item.getTag().equals(tag)) return item;
        return null;
    }
    public TabItem getTabByUrl(String url){
        for(TabItem item:getTabItems())
            if(item.getUrl().equals(url)) return item;
        return null;
    }

    private AtomicInteger m_AtomicInteger=new AtomicInteger();
    public int getUniqueIntValue(){
        return m_AtomicInteger.incrementAndGet();
    }

    private SharedPreferences preferences;

    public SharedPreferences getPreferences(){
        if(preferences ==null)
            preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences;
    }


    public String getWebViewFont() {
        return getPreferences().getString("webViewFontName", "");
    }

    public int getColorAccent(String type) {
        int color = 0;
        switch(type) {
            case "Accent":
                color = getPreferences().getInt("accentColor", Color.rgb(2, 119, 189));
                break;
            case "Pressed":
                color = getPreferences().getInt("accentColorPressed", Color.rgb(0, 89, 159));
                break;
        }
        return color;
    }
    public int getMainAccentColor() {
        int color = R.color.accentPink;
        switch (getPreferences().getString("mainAccentColor", "pink")) {
            case "pink":
                color  = R.color.accentPink;
                break;
            case "blue":
                color = R.color.accentBlue;
                break;
            case "gray":
                color = R.color.accentGray;
                break;
        }
        return color;
    }
    public int getThemeStyleResID() {
        int theme = R.style.ThemeLight;
        String color = getPreferences().getString("mainAccentColor", "pink");
        int themeType = getThemeType();
        if (themeType==THEME_TYPE_LIGHT){
            switch (color) {
                case "pink":
                    theme = R.style.MainPinkLight;
                    break;
                case "blue":
                    theme = R.style.MainBlueLight;
                    break;
                case "gray":
                    theme = R.style.MainGrayLight;
                    break;
            }
        }else if(themeType==THEME_TYPE_DARK){
            switch (color) {
                case "pink":
                    theme = R.style.MainPinkDark;
                    break;
                case "blue":
                    theme = R.style.MainBlueDark;
                    break;
                case "gray":
                    theme = R.style.MainGrayDark;
                    break;
            }
        }else {
            switch (color) {
                case "pink":
                    theme = R.style.MainPinkBlack;
                    break;
                case "blue":
                    theme = R.style.MainBlueBlack;
                    break;
                case "gray":
                    theme = R.style.MainGrayBlack;
                    break;
            }
        }
        return theme;
    }
    public int getPrefsThemeStyleResID() {
        int theme = R.style.ThemePrefsLightPink;
        String color = getPreferences().getString("mainAccentColor", "pink");
        int themeType = getThemeType();
        if (themeType==THEME_TYPE_LIGHT){
            switch (color) {
                case "pink":
                    theme = R.style.ThemePrefsLightPink;
                    break;
                case "blue":
                    theme = R.style.ThemePrefsLightBlue;
                    break;
                case "gray":
                    theme = R.style.ThemePrefsLightGray;
                    break;
            }
        }else if(themeType==THEME_TYPE_DARK){
            switch (color) {
                case "pink":
                    theme = R.style.ThemePrefsDarkPink;
                    break;
                case "blue":
                    theme = R.style.ThemePrefsDarkBlue;
                    break;
                case "gray":
                    theme = R.style.ThemePrefsDarkGray;
                    break;
            }
        }else {
            switch (color) {
                case "pink":
                    theme = R.style.ThemePrefsBlackPink;
                    break;
                case "blue":
                    theme = R.style.ThemePrefsBlackBlue;
                    break;
                case "gray":
                    theme = R.style.ThemePrefsBlackGray;
                    break;
            }
        }
        return theme;
    }

    public int getThemeType(){
        int themeType = 0;
        String themeStr = getCurrentTheme();
        if(themeStr.length()<3){
            int theme = Integer.parseInt(themeStr);
            if(ArrayUtils.indexOf(theme, LIGHT_THEMES)!=-1)
                themeType =  THEME_TYPE_LIGHT;
            else if(ArrayUtils.indexOf(theme, DARK_THEMES)!=-1)
                themeType = THEME_TYPE_DARK;
            else
                themeType = THEME_TYPE_BLACK;
        }else {
            if(themeStr.contains("/light/"))
                themeType = THEME_TYPE_LIGHT;
            else if(themeStr.contains("/dark/"))
                themeType = THEME_TYPE_DARK;
            else if(themeStr.contains("/black/"))
                themeType = THEME_TYPE_BLACK;
        }
        return themeType;
    }

    public int getThemeBackgroundColorRes() {
        int themeType = getThemeType();
        if (themeType==THEME_TYPE_LIGHT)
            return R.color.app_background_light;
        else if(themeType==THEME_TYPE_DARK)
            return R.color.app_background_dark;
        else
            return R.color.app_background_black;
    }
    public int getSwipeRefreshBackground() {
        int themeType = getThemeType();
        if (themeType==THEME_TYPE_LIGHT)
            return R.color.swipe_background_light;
        else if(themeType==THEME_TYPE_DARK)
            return R.color.swipe_background_dark;
        else
            return R.color.swipe_background_black;
    }

    public int getNavBarColor(){
        int themeType = getThemeType();
        if (themeType==THEME_TYPE_LIGHT)
            return R.color.navBar_light;
        else if(themeType==THEME_TYPE_DARK)
            return R.color.navBar_dark;
        else
            return R.color.navBar_black;
    }
    public int getDrawerMenuText(){
        int themeType = getThemeType();
        if (themeType==THEME_TYPE_LIGHT)
            return R.color.drawer_menu_text_light;
        else if(themeType==THEME_TYPE_DARK)
            return R.color.drawer_menu_text_dark;
        else
            return R.color.drawer_menu_text_dark;
    }


    public int getThemeStyleWebViewBackground() {
        int themeType = getThemeType();
        if (themeType==THEME_TYPE_LIGHT)
            return Color.parseColor("#eeeeee");
        else if(themeType==THEME_TYPE_DARK)
            return Color.parseColor("#1a1a1a");
        else
            return Color.parseColor("#000000");
    }

    public String getCurrentBackgroundColorHtml() {
        int themeType = getThemeType();
        if (themeType==THEME_TYPE_LIGHT)
            return "#eeeeee";
        else if(themeType==THEME_TYPE_DARK)
            return "#1a1a1a";
        else
            return "#000000";
    }

    public String getCurrentTheme() {
        return getPreferences().getString("appstyle", Integer.toString(THEME_LIGHT));
    }

    public String getCurrentThemeName() {
        int themeType = getThemeType();
        if (themeType==THEME_TYPE_LIGHT)
            return "white";
        else if(themeType==THEME_TYPE_DARK)
            return "dark";
        else
            return "black";
    }

    private String checkThemeFile(String themePath) {
        try {
            if (!new File(themePath).exists()) {
                // Toast.makeText(INSTANCE,"не найден файл темы: "+themePath,Toast.LENGTH_LONG).show();
                return defaultCssTheme();
            }
            return themePath;
        } catch (Throwable ex) {
            return defaultCssTheme();
        }
    }

    private String defaultCssTheme() {
        return "/android_asset/forum/css/4pda_light_blue.css";
    }

    public String getThemeCssFileName() {
        String themeStr = getCurrentTheme();
        return getThemeCssFileName(themeStr);
    }

    public String getThemeCssFileName(String themeStr) {
        if (themeStr.length() > 3)
            return checkThemeFile(themeStr);

        String path = "/android_asset/forum/css/";
        String cssFile = "4pda_light_blue.css";
        int theme = Integer.parseInt(themeStr);
        if (theme == -1)
            return themeStr;
        String color = getPreferences().getString("mainAccentColor", "pink");
        switch (theme) {
            case THEME_LIGHT:
                switch (color) {
                    case "pink":
                        cssFile = "4pda_light_blue.css";
                        break;
                    case "blue":
                        cssFile = "4pda_light_pink.css";
                        break;
                    case "gray":
                        cssFile = "4pda_light_gray.css";
                        break;
                }
                break;
            case THEME_DARK:
                switch (color) {
                    case "pink":
                        cssFile = "4pda_dark_blue.css";
                        break;
                    case "blue":
                        cssFile = "4pda_dark_pink.css";
                        break;
                    case "gray":
                        cssFile = "4pda_dark_gray.css";
                        break;
                }
                break;
            case THEME_BLACK:
                switch (color) {
                    case "pink":
                        cssFile = "4pda_black_blue.css";
                        break;
                    case "blue":
                        cssFile = "4pda_black_pink.css";
                        break;
                    case "gray":
                        cssFile = "4pda_black_gray.css";
                        break;
                }
                break;
            case THEME_MATERIAL_LIGHT:
                cssFile = "material_light.css";
                break;
            case THEME_MATERIAL_DARK:
                cssFile = "material_dark.css";
                break;
            case THEME_MATERIAL_BLACK:
                cssFile = "material_black.css";
                break;
            case THEME_LIGHT_OLD_HD:
                cssFile = "standart_4PDA.css";
                break;

            /*case THEME_WHITE_HD:
                cssFile = "white_hd.css";
                break;
            case THEME_BLACK_HD:
                cssFile = "black_hd.css";
                break;*/
            case THEME_CUSTOM_CSS:
                return "/mnt/sdcard/style.css";
        }
        return path + cssFile;
    }

    private static App INSTANCE = null;

    public App() {
        INSTANCE = this;


    }

    private MyActivityLifecycleCallbacks m_MyActivityLifecycleCallbacks;
    private static boolean isNewYear = false;
    @Override
    public void onCreate() {
        super.onCreate();
        ACRA.init(this);
        initImageLoader(this);
        m_MyActivityLifecycleCallbacks = new MyActivityLifecycleCallbacks();
        registerActivityLifecycleCallbacks(m_MyActivityLifecycleCallbacks);
        setTheme(getThemeStyleResID());
        try {
            DbHelper.prepareBases(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public boolean isNewYear(){
        return isNewYear;
    }

    public void exit() {
        m_MyActivityLifecycleCallbacks.finishActivities();
    }


    private static Boolean m_QmsStarted = false;
    private static Boolean m_FavoritesNotifierStarted = false;

    public static App getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new App();

        }
        if (!m_QmsStarted) {
            reStartQmsService();
        }
        if (!m_FavoritesNotifierStarted) {
            reStartFavoritesNotifierService();
        }
        return INSTANCE;
    }

    public static void resStartNotifierServices() {
        reStartQmsService();
        reStartFavoritesNotifierService();
    }

    private static void stopQmsService() {
        try {
            QmsNotifier.cancelAlarm(INSTANCE);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void reStartQmsService() {
        stopQmsService();
        startQmsService();
    }

    private static void startQmsService() {
        m_QmsStarted = true;
        try {
            if (!QmsNotifier.isUse(getContext()))
                return;
            Intent intent = new Intent(INSTANCE, MainService.class);
            intent.putExtra("CookiesPath", PreferencesActivity.getCookieFilePath(INSTANCE));
            intent.putExtra(QmsNotifier.TIME_OUT_KEY, Math.max(ExtPreferences.parseFloat(App.getInstance().getPreferences(),
                    QmsNotifier.TIME_OUT_KEY, 5), 1));

            QmsNotifier.restartTask(INSTANCE, intent);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    private static DisplayImageOptions.Builder options = new DisplayImageOptions.Builder()
            .showImageForEmptyUri(R.drawable.no_image)
            .cacheInMemory(true)
            .resetViewBeforeLoading(true)
            .cacheOnDisc(true)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .handler(new Handler())
            .displayer(new FadeInBitmapDisplayer(500, true, true, false));

    public static DisplayImageOptions.Builder getDefaultOptionsUIL(){
        return options;
    }
    public static void initImageLoader(Context context) {
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
                .imageDownloader(new HttpHelperForImage(context))
                .threadPoolSize(5)
                .threadPriority(Thread.MIN_PRIORITY)
                .denyCacheImageMultipleSizesInMemory()
                .memoryCache(new UsingFreqLimitedMemoryCache(5 * 1024 * 1024)) // 2 Mb
                .discCacheFileNameGenerator(new HashCodeFileNameGenerator())
                .defaultDisplayImageOptions(options.build())
                .build();

        ImageLoader.getInstance().init(config);
    }

    private static void stopFavoritesNotifierService() {
        try {
            FavoritesNotifier.cancelAlarm(INSTANCE);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static void reStartFavoritesNotifierService() {
        stopFavoritesNotifierService();
        startFavoritesNotifierService();
    }

    private static void startFavoritesNotifierService() {
        m_FavoritesNotifierStarted = true;
        try {
            if (!FavoritesNotifier.isUse(getContext())) return;

            Intent intent = new Intent(INSTANCE, MainService.class);
            intent.putExtra("CookiesPath", PreferencesActivity.getCookieFilePath(INSTANCE));
            intent.putExtra(FavoritesNotifier.TIME_OUT_KEY, Math.max(ExtPreferences.parseFloat(App.getInstance().getPreferences(),
                    FavoritesNotifier.TIME_OUT_KEY, 5), 1));

            FavoritesNotifier.restartTask(INSTANCE, intent);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static Context getContext() {
        return getInstance();
    }

    public static SwipeRefreshLayout createSwipeRefreshLayout(Activity activity, View view,
                                                                final Runnable refreshAction) {
        SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.ptr_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshAction.run();
            }
        });
        swipeRefreshLayout.setColorSchemeResources(App.getInstance().getMainAccentColor());
        swipeRefreshLayout.setProgressBackgroundColorSchemeResource(App.getInstance().getSwipeRefreshBackground());
        return swipeRefreshLayout;
    }




    private static final class MyActivityLifecycleCallbacks implements ActivityLifecycleCallbacks {

        private HashMap<String, Activity> m_Activities = new HashMap<>();

        public void onActivityCreated(Activity activity, Bundle bundle) {
            m_Activities.put(activity.getLocalClassName(), activity);
        }

        public void onActivityDestroyed(Activity activity) {
            if (m_Activities.containsKey(activity.getLocalClassName()))
                m_Activities.remove(activity.getLocalClassName());
        }

        public void onActivityPaused(Activity activity) {

        }

        public void onActivityResumed(Activity activity) {

        }

        public void onActivitySaveInstanceState(Activity activity,
                                                Bundle outState) {

        }

        public void onActivityStarted(Activity activity) {

        }

        public void onActivityStopped(Activity activity) {

        }

        public void finishActivities() {
            for (Map.Entry<String, Activity> entry : m_Activities.entrySet()) {
                try {
                    Activity activity = entry.getValue();

                    if (activity == null)
                        continue;

                    if (Build.VERSION.SDK_INT >= 17 && activity.isDestroyed())
                        continue;

                    if (activity.isFinishing())
                        continue;

                    entry.getValue().finish();
                } catch (Throwable ex) {
                    Log.e("", "finishActivities:" + ex.toString());
                }
            }
        }
    }


}