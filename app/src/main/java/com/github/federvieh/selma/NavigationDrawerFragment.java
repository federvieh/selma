package com.github.federvieh.selma;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;

import com.github.federvieh.selma.assimillib.AssimilDatabase;
import com.github.federvieh.selma.assimillib.OverlayManager;
import org.acra.ACRA;
import org.acra.ACRAConfigurationException;
import org.acra.ReportingInteractionMode;

/**
 * Fragment used for managing interactions for and presentation of a navigation
 * drawer. See the <a href=
 * "https://developer.android.com/design/patterns/navigation-drawer.html#Interaction"
 * > design guidelines</a> for a complete explanation of the behaviors
 * implemented here.
 */
public class NavigationDrawerFragment extends Fragment {

    /**
     * Remember the position of the selected item.
     */
    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";

    /**
     * Per the design guidelines, you should show the drawer on launch until the
     * user manually expands it. This shared preference tracks this.
     */
//	private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";

    /**
     * A pointer to the current callbacks instance (the Activity).
     */
    private NavigationDrawerCallbacks mCallbacks;

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private ActionBarDrawerToggle mDrawerToggle;

    private DrawerLayout mDrawerLayout;
    private View mDrawerView;
    private View mFragmentContainerView;

    private int mCurrentSelectedPosition = 0;
//	private boolean mFromSavedInstanceState;
//	private boolean mUserLearnedDrawer;

    private LinearLayout mCourseListView;

    private ArrayList<String> allCourses;

    private CourseListAdapter mCourseListAdapter;

    public NavigationDrawerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("LT", this.getClass().getSimpleName()+".onCreate(); savedInstanceState="+savedInstanceState);

        // Read in the flag indicating whether or not the user has demonstrated
        // awareness of the
        // drawer. See PREF_USER_LEARNED_DRAWER for details.
//		SharedPreferences sp = PreferenceManager
//				.getDefaultSharedPreferences(getActivity());
//		mUserLearnedDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false);

        if (savedInstanceState != null) {
            mCurrentSelectedPosition = savedInstanceState
                    .getInt(STATE_SELECTED_POSITION);
//			mFromSavedInstanceState = true;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Indicate that this fragment would like to influence the set of
        // actions in the action bar.
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Log.d("LT", getClass().getSimpleName()+".onCreateView()");
        mDrawerView = inflater.inflate(
                R.layout.fragment_navigation_drawer, container, false);
//		mDrawerView
//				.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//					@Override
//					public void onItemClick(AdapterView<?> parent, View view,
//							int position, long id) {
//						selectItem(position);
//					}
//				});
        mCourseListView = (LinearLayout)mDrawerView.findViewById(R.id.courseListView);
        if((allCourses==null) && (AssimilDatabase.isAllocated())){
            allCourses = AssimilDatabase.getAllCourses();
            mCourseListAdapter = new CourseListAdapter(getActivity(), allCourses, this);
            mCourseListView.removeAllViews();
            for(int i = 0; i < allCourses.size(); i++){
                mCourseListView.addView(mCourseListAdapter.getView(i,null,mCourseListView));
            }
        }
        View showTips = mDrawerView.findViewById(R.id.textViewNavTips);
        OnClickListener oclTips = new OnClickListener() {

            @Override
            public void onClick(View v) {
                OverlayManager.resetOverlays();
                mDrawerLayout.closeDrawer(mFragmentContainerView);
            }
        };
        showTips.setOnClickListener(oclTips);

        View showLicense = mDrawerView.findViewById(R.id.textViewNavLicense);
        OnClickListener oclLicense = new OnClickListener() {

            @Override
            public void onClick(View v) {
                showLicense();
                mDrawerLayout.closeDrawer(mFragmentContainerView);
            }
        };
        showLicense.setOnClickListener(oclLicense);

        View sendLogToDev = mDrawerView.findViewById(R.id.textViewLogToDev);
        OnClickListener oclLogToDev = new OnClickListener() {

            @Override
            public void onClick(View v) {
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
            }
        };
        sendLogToDev.setOnClickListener(oclLogToDev);

        return mDrawerView;
    }


    /**
     *
     */
    protected void showLicense() {
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        PackageInfo pInfo = null;
        try {
            pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
        } catch (NameNotFoundException e) {
            Log.e("LT", "Could not get package version", e);
        }
        StringBuffer licenseText = new StringBuffer(readRawTextFile(getActivity(), R.raw.license_part1));
        if(pInfo!=null){
            licenseText.append(pInfo.versionName);
        }
        else{
            licenseText.append("<unknown version>");
        }
        licenseText.append(readRawTextFile(getActivity(), R.raw.license_part2));
        builder.setMessage(licenseText)
               .setTitle(R.string.action_license);

        builder.setPositiveButton(R.string.show_license, new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) {
//	        	   Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.gnu.org/licenses/gpl-2.0-standalone.html"));
//	        	   startActivity(browserIntent);
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

    /**
     *
     */
    protected void openGPL() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View layoutView = inflater.inflate(R.layout.license_view, null);
        builder.setView(layoutView);
        WebView webView = (WebView)layoutView.findViewById(R.id.webViewLicense);
        webView.loadUrl("file:///android_res/raw/gpl_3_0_standalone.html");

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User closed the dialog
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private static String readRawTextFile(Context ctx, int resId)
    {
        InputStream inputStream = ctx.getResources().openRawResource(resId);

        InputStreamReader inputreader = new InputStreamReader(inputStream);
        BufferedReader buffreader = new BufferedReader(inputreader);
        String line;
        StringBuilder text = new StringBuilder();

        try {
            int lineNbr = 0;
            while (( line = buffreader.readLine()) != null) {
                if(lineNbr>0){
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

    /** Should be called whenever an item was selected, so that the navigation drawer is re-drawn.
     *
     */
    private void redraw(){
        mCourseListAdapter = new CourseListAdapter(getActivity(), allCourses, this);
        mCourseListView.removeAllViews();
        for(int i = 0; i < allCourses.size(); i++){
            mCourseListView.addView(mCourseListAdapter.getView(i,null,mCourseListView));
        }
    }

    public boolean isDrawerOpen() {
        return mDrawerLayout != null
                && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    /**
     * Users of this fragment must call this method to set up the navigation
     * drawer interactions.
     *
     * @param fragmentId
     *            The android:id of this fragment in its activity's layout.
     * @param drawerLayout
     *            The DrawerLayout containing this fragment's UI.
     */
    public void setUp(int fragmentId, DrawerLayout drawerLayout) {
        mFragmentContainerView = getActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;

        // set a custom shadow that overlays the main content when the drawer
        // opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
                GravityCompat.START);
        // set up the drawer's list view with items and click listener
        if((AssimilDatabase.isAllocated())){
            allCourses = AssimilDatabase.getAllCourses();
            mCourseListAdapter = new CourseListAdapter(getActivity(), allCourses, this);
            mCourseListView.removeAllViews();
            for(int i = 0; i < allCourses.size(); i++){
                mCourseListView.addView(mCourseListAdapter.getView(i,null,mCourseListView));
            }
        }

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new ActionBarDrawerToggle(getActivity(), /* host Activity */
        mDrawerLayout, /* DrawerLayout object */
        R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
        R.string.navigation_drawer_open, /*
                                         * "open drawer" description for
                                         * accessibility
                                         */
        R.string.navigation_drawer_close /*
                                         * "close drawer" description for
                                         * accessibility
                                         */
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (!isAdded()) {
                    return;
                }

                ((ShowLessonFragmentListener)getActivity()).onResumedTitleUpdate(null);

                getActivity().supportInvalidateOptionsMenu(); // calls
                                                                // onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (!isAdded()) {
                    return;
                }

//				if (!mUserLearnedDrawer) {
//					// The user manually opened the drawer; store this flag to
//					// prevent auto-showing
//					// the navigation drawer automatically in the future.
//					mUserLearnedDrawer = true;
//					SharedPreferences sp = PreferenceManager
//							.getDefaultSharedPreferences(getActivity());
//					sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true)
//							.commit();
//				}

                getActivity().supportInvalidateOptionsMenu(); // calls
                                                                // onPrepareOptionsMenu()
            }
        };

        // If the user hasn't 'learned' about the drawer, open it to introduce
        // them to the drawer,
        // per the navigation drawer design guidelines.
//		if (!mUserLearnedDrawer && !mFromSavedInstanceState) {
//			mDrawerLayout.openDrawer(mFragmentContainerView);
//		}

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    public void selectItem(String courseName, boolean starredOnly) {
//		mCurrentSelectedPosition = position;
//		if (mDrawerListView != null) {
//			mDrawerListView.setItemChecked(position, true);
//		}
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mFragmentContainerView);
        }
        if (mCallbacks != null) {
            mCallbacks.onLangItemSelected(courseName, starredOnly);
        }
        redraw();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (NavigationDrawerCallbacks) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(
                    "Activity must implement NavigationDrawerCallbacks.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // If the drawer is open, show the global app actions in the action bar.
        // See also
        // showGlobalContextActionBar, which controls the top-left area of the
        // action bar.
        if (mDrawerLayout != null && isDrawerOpen()) {
//			inflater.inflate(R.menu.global, menu);
            showGlobalContextActionBar();
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

//		if (item.getItemId() == R.id.action_example) {
//			Toast.makeText(getActivity(), "Example action.", Toast.LENGTH_SHORT)
//					.show();
//			return true;
//		}

        return super.onOptionsItemSelected(item);
    }

    /**
     * Per the navigation drawer design guidelines, updates the action bar to
     * show the global app 'context', rather than just what's in the current
     * screen.
     */
    private void showGlobalContextActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setTitle(R.string.app_name);
        setDrawerIndicatorEnabled(true);
    }

    private ActionBar getActionBar() {
        return ((ActionBarActivity) getActivity()).getSupportActionBar();
    }

    /**
     * Callbacks interface that all activities using this fragment must
     * implement.
     */
    public static interface NavigationDrawerCallbacks {
        /**
         * Called when an item in the navigation drawer is selected.
         */
        void onLangItemSelected(String courseName, boolean starredOnly);
    }

    /**
     *
     */
    public void setDrawerIndicatorEnabled(boolean enabled) {
        mDrawerToggle.setDrawerIndicatorEnabled(enabled);
    }
}
