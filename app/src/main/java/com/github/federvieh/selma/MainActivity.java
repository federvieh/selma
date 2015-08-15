package com.github.federvieh.selma;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.*;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.SpinnerAdapter;
import com.github.federvieh.selma.assimillib.*;
import com.github.federvieh.selma.assimillib.LessonPlayer.PlayMode;
import com.github.federvieh.selma.assimillib.LoaderFragment.LoaderFragmentCallbacks;
import org.acra.ACRA;
import org.acra.ACRAConfigurationException;
import org.acra.ReportingInteractionMode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements
        LoaderFragmentCallbacks,
        PlaybarFragment.OnPlaybarInteractionListener, ShowLessonFragmentListener {

    private DrawerLayout mDrawerLayout;

    public enum ActivityState {
        DATABASE_LOADING,
        INITIAL_SCANNING_FOR_LESSONS,
        FORCED_SCANNING_FOR_LESSONS,
        READY_FOR_PLAYBACK_NO_SCANNING,
        READY_FOR_PLAYBACK_AFTER_SCANNING,
        READY_FOR_PLAYBACK_AFTER_FORCED_SCANNING,
    }

    private static final String PREF_STARRED_ONLY = "PREF_STARRED_ONLY";
    private static final String PREF_CURRENT_COURSE = "PREF_CURRENT_COURSE";

    private NavigationView mNavigationView;

    /**
     * Used to store the last screen title. For use in
     * {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    private DatabaseInitTask dbInitTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //For some reason between the following two log calls, several seconds pass.
        //Hence the screen is not updated while loading and rotating the device. This is why
        //in the async task the screen rotation is locked.
        //TODO: Get rid of locked screen while loading
        Log.w("LT", this.getClass().getSimpleName() + ".onCreate(); savedInstanceState=" + savedInstanceState);

        mTitle = getTitle();
        Intent intend = getIntent();

        long lessonTemp = intend.getLongExtra(AssimilOnClickListener.EXTRA_LESSON_ID, -1);
        int trackNumber = intend.getIntExtra(AssimilOnClickListener.EXTRA_TRACK_INDEX, -1);
        boolean forceReload = intend.getBooleanExtra(LoaderFragment.FORCE_RESET, false);
        //First check, if this got called from savedInstance (e.g. rotation).
        //Note that this check must be before checking for intend, because the
        //intend will also be visible after rotation.
        if (savedInstanceState != null) {//Not called by intend, so just rotation!?
            Log.i("LT", this.getClass().getSimpleName() + ".onCreate(); Got called from savedInstance");
            setContentView(R.layout.activity_main);
            Log.w("LT", this.getClass().getSimpleName() + ".onCreate(); contentView was set");
        } else if (lessonTemp >= 0) {
            //This is called by onLoadingFinished(), so we don't call it here: setContentView(R.layout.activity_main);
            //Log.w("LT", this.getClass().getSimpleName()+".onCreate(); contentView was set");
            Log.i("LT", this.getClass().getSimpleName() + ".onCreate(); Got called from intend with lesson id");
            onLoadingFinished(true);
            onLessonClicked(lessonTemp, trackNumber);
        } else {
            if (forceReload) {
                Log.i("LT", this.getClass().getSimpleName() + ".onCreate(); Got called from intend with force reload");
            } else {
                Log.i("LT", this.getClass().getSimpleName() + ".onCreate(); Got called without intend or savedInstance");
            }
            setContentView(R.layout.activity_loader);
            Log.w("LT", this.getClass().getSimpleName() + ".onCreate(); contentView was set");
            if ((dbInitTask == null) || forceReload) {
                dbInitTask = new DatabaseInitTask();
                dbInitTask.execute(forceReload);
            }
            Log.w("LT", this.getClass().getSimpleName() + ".onCreate(); calling Loader");
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.container,
                            new LoaderFragment(this)).commit();
        }
        SharedPreferences settings = getSharedPreferences("selma", Context.MODE_PRIVATE);
        int i = settings.getInt(ShowLessonFragment.LIST_MODE, ListTypes.TRANSLATE.ordinal());
        ListTypes lt = ListTypes.values()[i];
        LessonPlayer.setListType(lt);

        i = settings.getInt(PlaybarFragment.PLAY_MODE, PlayMode.REPEAT_ALL_LESSONS.ordinal());
        PlayMode pm = PlayMode.values()[i];
        LessonPlayer.setPlayMode(pm);
    }

    private void onLangItemSelected(String courseName, boolean starredOnly) {
        Log.i("LT", this.getClass().getSimpleName() + ".onLangItemSelected(); coursename="
                + courseName + ", starredOnly=" + starredOnly);
        AssimilDatabase.setLang(courseName);
        AssimilDatabase.setStarredOnly(starredOnly);
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
        sp.edit().putBoolean(PREF_STARRED_ONLY, starredOnly).putString(PREF_CURRENT_COURSE, courseName)
                .commit();

        FragmentManager fragmentManager = getSupportFragmentManager();
//		int popped = 0;
        while (fragmentManager.popBackStackImmediate()) {
//			popped++;
//			Log.d("LT", "popped = " + popped);
        }
        Fragment f = fragmentManager.findFragmentById(R.id.container);
        ListAdapter la = new AssimilLessonListAdapter(this, AssimilDatabase.getCurrentLessons());
        ((LessonListFragment) f).setListAdapter(la);
    }

//	public void restoreActionBar() {
//		ActionBar actionBar = getSupportActionBar();
//		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
//		actionBar.setDisplayShowTitleEnabled(true);
//		actionBar.setTitle(mTitle);
//	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        final Fragment f = getSupportFragmentManager().findFragmentById(R.id.container);
        boolean hasShowLessonFragment = (f.getClass().equals(ShowLessonFragment.class));
        boolean hasLessonListFragment = (f.getClass().equals(LessonListFragment.class));
        switch (item.getItemId()) {
            case android.R.id.home:
                if (hasLessonListFragment) {
                    mDrawerLayout.openDrawer(GravityCompat.START);
                    return true;
                } else {
                    //Fall through
                }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /* (non-Javadoc)
         * @see com.github.federvieh.selma.assimillib.LoaderFragment.LoaderFragmentCallbacks#onLoadingFinished()
         */
    @Override
    public void onLoadingFinished(boolean lessonsFound) {
        Log.i("LT", this.getClass().getSimpleName() + ".onLoadingFinished(); lessonsFound="
                + lessonsFound);
        if (lessonsFound) {
            setContentView(R.layout.activity_main);
            Log.w("LT", this.getClass().getSimpleName() + ".onLoadingFinished(); contentView was set");


            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);


            //Which lesson list (language+starred) to show is stored as preference
            SharedPreferences sp = PreferenceManager
                    .getDefaultSharedPreferences(this);
            String currCourse = sp.getString(PREF_CURRENT_COURSE, null);
            final boolean starredOnly = sp.getBoolean(PREF_STARRED_ONLY, false);
            AssimilDatabase.setLang(currCourse);
            AssimilDatabase.setStarredOnly(starredOnly);
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            LessonListFragment lf = new LessonListFragment();
            fragmentTransaction.replace(R.id.container, lf);
            PlaybarFragment pf = PlaybarFragment.newInstance();
            fragmentTransaction.add(R.id.playbarContainer, pf);
            fragmentTransaction.commit();
            View pb = findViewById(R.id.playbarContainer);
            pb.setVisibility(View.VISIBLE);
            // Set up the drawer.
            mNavigationView = (NavigationView) findViewById(R.id.navigation_view);
            ArrayList<String> allCourses = AssimilDatabase.getAllCourses();
            final Menu menu = mNavigationView.getMenu();
            if (allCourses.size() > 1) {
                SubMenu topChannelMenu = menu.addSubMenu(getString(R.string.all_courses));
                topChannelMenu.add(getString(R.string.all_lessons));
                topChannelMenu.add(getString(R.string.starred_lessons));
            }
            for (String course : allCourses) {
                SubMenu topChannelMenu = menu.addSubMenu(course);
                topChannelMenu.add(getString(R.string.all_lessons));
                topChannelMenu.add(getString(R.string.starred_lessons));
            }
            menu.add(R.string.action_show_tips);
            menu.add(R.string.action_license);
            menu.add(R.string.action_log_to_dev);

            //TODO: https://code.google.com/p/android/issues/detail?id=176300
            MenuItem mi = menu.getItem(menu.size() - 1);
            mi.setTitle(mi.getTitle());
            mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
            //setting up selected item listener
            mNavigationView.setNavigationItemSelectedListener(
                    new NavigationView.OnNavigationItemSelectedListener() {
                        @Override
                        public boolean onNavigationItemSelected(MenuItem menuItem) {
                            CharSequence title = menuItem.getTitle();
                            Log.d("LT", "Navigation item " + title);
                            if (title.equals(getString(R.string.action_show_tips))) {
                                OverlayManager.resetOverlays();
                            } else if (title.equals(getString(R.string.action_license))) {
                                showLicense();
                            } else if (title.equals(getString(R.string.action_log_to_dev))) {
                                try {
                                    ACRA.getConfig().setMode(ReportingInteractionMode.SILENT);
                                } catch (ACRAConfigurationException e) {
                                    Log.e("LT", "Problem setting ACRA config to silent", e);
                                }
                                ACRA.getErrorReporter().handleException(null, false);
                                try {
                                    ACRA.getConfig().setMode(ReportingInteractionMode.DIALOG);
                                } catch (ACRAConfigurationException e) {
                                    Log.e("LT", "Problem setting ACRA config back to dialog", e);
                                }
                            } else {
                                boolean starred;
                                if (title.equals(getString(R.string.all_lessons))) {
                                    starred = false;
                                } else if (title.equals(getString(R.string.starred_lessons))) {
                                    starred = true;
                                } else {
                                    return false;
                                }
                                String courseName = findCoursename(menu, menuItem, starred);
                                onLangItemSelected(courseName, starred);
                                boolean ic = menuItem.isCheckable();
                                if (!ic) {
                                    menuItem.setCheckable(true);
                                }
                                menuItem.setChecked(true);
                                uncheckOthers(menuItem, menu);
                            }
                            mDrawerLayout.closeDrawers();
                            return true;
                        }

                        private String findCoursename(Menu m, MenuItem menuItem, boolean isStarred) {
                            for (int i = 0; i < m.size(); i++) {
                                MenuItem menuItemIter = m.getItem(i);
                                if (menuItemIter.hasSubMenu()) {
                                    SubMenu sm = menuItemIter.getSubMenu();
                                    int index = isStarred ? 1 : 0;
                                    if (sm.getItem(index).equals(menuItem)) {
                                        return menuItemIter.getTitle().toString();
                                    } else {
                                        //continue
                                    }
                                }
                            }
                            return null;
                        }

                        private void uncheckOthers(MenuItem menuItem, Menu m) {
                            for (int i = 0; i < m.size(); i++) {
                                MenuItem menuItemIter = m.getItem(i);
                                if (menuItemIter.equals(menuItem)) {
                                    Log.d("LT", "FOUND! " + i);
                                } else {
                                    Log.d("LT", "NOT FOUND! " + i);
                                    //TODO: https://code.google.com/p/android/issues/detail?id=175216
                                    menuItemIter.setChecked(true);
                                    menuItemIter.setChecked(false);
                                    if (menuItemIter.hasSubMenu()) {
                                        //recursive call on submenus
                                        uncheckOthers(menuItem, menuItemIter.getSubMenu());
                                    }
                                }
                            }
                        }
                    });
        } else {
            //Nothing to do, is handled in LoaderFragment
        }
    }

    /* (non-Javadoc)
     * @see com.github.federvieh.selma.PlaybarFragment.OnPlaybarInteractionListener#onLessonClicked(long, int)
     */
    @Override
    public void onLessonClicked(long id, int trackNumber) {
        Log.d("LT", "Start lesson " + id + ", track " + trackNumber);
        /*The following call clears the call stack*/
        onLangItemSelected(AssimilDatabase.getLang(), AssimilDatabase.isStarredOnly());
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        ShowLessonFragment slf = ShowLessonFragment.newInstance(id, trackNumber, this);
        fragmentTransaction.replace(R.id.container, slf);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    /* (non-Javadoc)
     * @see com.github.federvieh.selma.ShowLessonFragmentListener#onResumedTitleUpdate(java.lang.String)
     */
    @Override
    public void onResumedTitleUpdate(String title) {
        if (title != null) {
            mTitle = title;
        }
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(mTitle);
        final Fragment f = getSupportFragmentManager().findFragmentById(R.id.container);
        boolean hasShowLessonFragment = (f.getClass().equals(ShowLessonFragment.class));
        boolean hasLessonListFragment = (f.getClass().equals(LessonListFragment.class));
        if (hasShowLessonFragment) {
            SpinnerAdapter mSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.exercise_option_list,
                    R.layout.spinner_dropdown_item);
            ActionBar.OnNavigationListener mOnNavigationListener = new ActionBar.OnNavigationListener() {
                @Override
                public boolean onNavigationItemSelected(int position, long itemId) {
                    ListTypes lt;
                    if (position == 0) {
                        lt = ListTypes.TRANSLATE;
                    } else {
                        lt = ListTypes.NO_TRANSLATE;
                    }
                    ((ShowLessonFragment) f).updateListType(lt);
                    return true;
                }
            };
            ListTypes lt = LessonPlayer.getListType();
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            actionBar.setListNavigationCallbacks(mSpinnerAdapter, mOnNavigationListener);
            int navItem = 0;
            switch (lt) {
                case TRANSLATE:
                    navItem = 0;
                    break;
                case NO_TRANSLATE:
                    navItem = 1;
                    break;
            }
            actionBar.setSelectedNavigationItem(navItem);

            getSupportActionBar().setHomeAsUpIndicator(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            OverlayManager.showOverlayLessonContent(this);
        } else {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_drawer);

            if (hasLessonListFragment) {
                OverlayManager.showOverlayLessonList(this);
            }
        }
    }

    private class DatabaseInitTask extends AsyncTask<Boolean, ActivityState, ActivityState> {
        /**
         * The system calls this to perform work in the UI thread and delivers
         * the result from doInBackground()
         */
        protected void onPostExecute(ActivityState result) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            final Fragment f = fragmentManager.findFragmentById(R.id.container);
            boolean hasLoaderFragment = (f != null) && (f.getClass().equals(LoaderFragment.class));
            if (hasLoaderFragment) {
                ((LoaderFragment) f).showWaiting(result);
            } else {
                Log.w("LT", this.getClass().getSimpleName() + ".onPostExecute(); Loading finished with state " +
                        result + ", but no LoaderFragment exists.");
            }
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }

        /* (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(java.lang.Object[])
         */
        @Override
        protected ActivityState doInBackground(Boolean... forceScan) {
            //TODO: If wasScanning, we could inform LoaderFragment, but if not, we could directly load the lesson list, shouldn't we?
            Log.w("LT", "Scanning started, force = " + forceScan[0]);
            //TODO: Get rid of locked screen while loading
            //The former commented calls are supposed to be "better" for locking screen orientation,
            //but I couldn't see the difference on the Nexus 5 and the latter calls are required on
            //API level 8 devices. So, I keep this for now for all API levels and maybe change it later,
            //when the difference is discovered.
//			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
//			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            int currentOrientation = getResources().getConfiguration().orientation;
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
            boolean wasScanning = false;
            if (!forceScan[0]) {
                AssimilDatabase ad = AssimilDatabase.getDatabase(getApplicationContext(), false);
                if (ad.getAllLessonHeaders().size() <= 0) {
                    publishProgress(ActivityState.INITIAL_SCANNING_FOR_LESSONS);
                    wasScanning = true;
                    AssimilDatabase.getDatabase(getApplicationContext(), true);
                } else {
                    //Lessons were already found. No need to re-scan.
                }
            } else {
                publishProgress(ActivityState.FORCED_SCANNING_FOR_LESSONS);
                wasScanning = true;
                AssimilDatabase.reset();
                AssimilDatabase.getDatabase(getApplicationContext(), true);
            }
            if (wasScanning) {
                if (!forceScan[0]) {
                    return ActivityState.READY_FOR_PLAYBACK_AFTER_SCANNING;
                } else {
                    return ActivityState.READY_FOR_PLAYBACK_AFTER_FORCED_SCANNING;
                }
            }
            return ActivityState.READY_FOR_PLAYBACK_NO_SCANNING;
        }

        @Override
        protected void onProgressUpdate(ActivityState... activityStates) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            final Fragment f = fragmentManager.findFragmentById(R.id.container);
            boolean hasLoaderFragment = (f != null) && (f.getClass().equals(LoaderFragment.class));
            if (hasLoaderFragment) {
                ((LoaderFragment) f).showWaiting(activityStates[0]);
            } else {
                Log.w("LT", this.getClass().getSimpleName() + ".onProgressUpdate(); Loading progressed tos state " +
                        activityStates[0] + ", but no LoaderFragment exists.");
            }
        }
    }


    private void showLicense() {
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("LT", "Could not get package version", e);
        }
        StringBuffer licenseText = new StringBuffer(readRawTextFile(this, R.raw.license_part1));
        if (pInfo != null) {
            licenseText.append(pInfo.versionName);
        } else {
            licenseText.append("<unknown version>");
        }
        licenseText.append(readRawTextFile(this, R.raw.license_part2));
        builder.setMessage(licenseText)
                .setTitle(R.string.action_license);

        builder.setPositiveButton(R.string.show_license, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                openGPL();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void openGPL() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Get the layout inflater
        LayoutInflater inflater = getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View layoutView = inflater.inflate(R.layout.license_view, null);
        builder.setView(layoutView);
        WebView webView = (WebView) layoutView.findViewById(R.id.webViewLicense);
        webView.loadUrl("file:///android_res/raw/gpl_3.0_standalone.html");

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User closed the dialog
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private static String readRawTextFile(Context ctx, int resId) {
        InputStream inputStream = ctx.getResources().openRawResource(resId);

        InputStreamReader inputreader = new InputStreamReader(inputStream);
        BufferedReader buffreader = new BufferedReader(inputreader);
        String line;
        StringBuilder text = new StringBuilder();

        try {
            int lineNbr = 0;
            while ((line = buffreader.readLine()) != null) {
                if (lineNbr > 0) {
                    text.append('\n');
                }
                text.append(line);
                lineNbr++;
            }
        } catch (IOException e) {
            return null;
        }
        return text.toString();
    }
}
