package com.github.federvieh.selma;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.SpinnerAdapter;

import com.github.federvieh.selma.assimillib.AssimilDatabase;
import com.github.federvieh.selma.assimillib.AssimilLessonListAdapter;
import com.github.federvieh.selma.assimillib.AssimilOnClickListener;
import com.github.federvieh.selma.assimillib.LessonPlayer;
import com.github.federvieh.selma.assimillib.OverlayManager;
import com.github.federvieh.selma.assimillib.LessonPlayer.PlayMode;
import com.github.federvieh.selma.assimillib.ListTypes;
import com.github.federvieh.selma.assimillib.LoaderFragment;
import com.github.federvieh.selma.assimillib.LoaderFragment.LoaderFragmentCallbacks;

public class MainActivity extends ActionBarActivity implements
		NavigationDrawerFragment.NavigationDrawerCallbacks, LoaderFragmentCallbacks,
		PlaybarFragment.OnPlaybarInteractionListener, ShowLessonFragmentListener{

	public enum ActivityState{
		DATABASE_LOADING,
		INITIAL_SCANNING_FOR_LESSONS,
		FORCED_SCANNING_FOR_LESSONS,
		READY_FOR_PLAYBACK_NO_SCANNING,
		READY_FOR_PLAYBACK_AFTER_SCANNING,
		READY_FOR_PLAYBACK_AFTER_FORCED_SCANNING,
	}

	private static final String PREF_STARRED_ONLY = "PREF_STARRED_ONLY";
	private static final String PREF_CURRENT_COURSE = "PREF_CURRENT_COURSE";

	/**
	 * Fragment managing the behaviors, interactions and presentation of the
	 * navigation drawer.
	 */
	private NavigationDrawerFragment mNavigationDrawerFragment;

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
		Log.w("LT", this.getClass().getSimpleName()+".onCreate(); savedInstanceState="+savedInstanceState);

		mTitle = getTitle();
		Intent intend = getIntent();

		long lessonTemp = intend.getLongExtra(AssimilOnClickListener.EXTRA_LESSON_ID,-1);
		int trackNumber = intend.getIntExtra(AssimilOnClickListener.EXTRA_TRACK_INDEX, -1);
		boolean forceReload = intend.getBooleanExtra(LoaderFragment.FORCE_RESET, false);
		//First check, if this got called from savedInstance (e.g. rotation).
		//Note that this check must be before checking for intend, because the
		//intend will also be visible after rotation.
		if (savedInstanceState!=null){//Not called by intend, so just rotation!?
			Log.i("LT", this.getClass().getSimpleName()+".onCreate(); Got called from savedInstance");
			setContentView(R.layout.activity_main);
			Log.w("LT", this.getClass().getSimpleName()+".onCreate(); contentView was set");
		}
		else if(lessonTemp>=0){
			//This is called by onLoadingFinished(), so we don't call it here: setContentView(R.layout.activity_main);
			//Log.w("LT", this.getClass().getSimpleName()+".onCreate(); contentView was set");
			Log.i("LT", this.getClass().getSimpleName()+".onCreate(); Got called from intend with lesson id");
			onLoadingFinished(true);
			onLessonClicked(lessonTemp, trackNumber);
		}
		else{
			if(forceReload){
				Log.i("LT", this.getClass().getSimpleName()+".onCreate(); Got called from intend with force reload");
			}
			else{
				Log.i("LT", this.getClass().getSimpleName()+".onCreate(); Got called without intend or savedInstance");
			}
			setContentView(R.layout.activity_loader);
			Log.w("LT", this.getClass().getSimpleName()+".onCreate(); contentView was set");
			if((dbInitTask==null) || forceReload){
				dbInitTask = new DatabaseInitTask();
				dbInitTask.execute(forceReload);
			}
			Log.w("LT", this.getClass().getSimpleName()+".onCreate(); calling Loader");
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
	
	@Override
	public void onLangItemSelected(String courseName, boolean starredOnly) {
		Log.i("LT", this.getClass().getSimpleName()+".onLangItemSelected(); coursename="
				+ courseName + ", starredOnly="+starredOnly);
		AssimilDatabase.setLang(courseName);
		AssimilDatabase.setStarredOnly(starredOnly);
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(this);
		sp.edit().putBoolean(PREF_STARRED_ONLY, starredOnly).putString(PREF_CURRENT_COURSE,courseName)
				.commit();

		FragmentManager fragmentManager = getSupportFragmentManager();
//		int popped = 0;
		while(fragmentManager.popBackStackImmediate()){
//			popped++;
//			Log.d("LT", "popped = " + popped);
		}
		Fragment f = fragmentManager.findFragmentById(R.id.container);
		ListAdapter la = new AssimilLessonListAdapter(this, AssimilDatabase.getCurrentLessons());
		((LessonListFragment)f).setListAdapter(la);
	}

//	public void restoreActionBar() {
//		ActionBar actionBar = getSupportActionBar();
//		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
//		actionBar.setDisplayShowTitleEnabled(true);
//		actionBar.setTitle(mTitle);
//	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (mNavigationDrawerFragment!=null && !mNavigationDrawerFragment.isDrawerOpen()) {
			// Only show items in the action bar relevant to this screen
			// if the drawer is not showing. Otherwise, let the drawer
			// decide what to show in the action bar.
//			getMenuInflater().inflate(R.menu.main, menu);
//			restoreActionBar();
			return true;
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		//int id = item.getItemId();
		return super.onOptionsItemSelected(item);
	}

	/* (non-Javadoc)
	 * @see com.github.federvieh.selma.assimillib.LoaderFragment.LoaderFragmentCallbacks#onLoadingFinished()
	 */
	@Override
	public void onLoadingFinished(boolean lessonsFound) {
		Log.i("LT", this.getClass().getSimpleName()+".onLoadingFinished(); lessonsFound="
				+ lessonsFound);
		if(lessonsFound){
			setContentView(R.layout.activity_main);
			Log.w("LT", this.getClass().getSimpleName()+".onLoadingFinished(); contentView was set");

			//Which lesson list (language+starred) to show is stored as preference
			SharedPreferences sp = PreferenceManager
					.getDefaultSharedPreferences(this);
			String currCourse = sp.getString(PREF_CURRENT_COURSE, null);
			boolean starredOnly = sp.getBoolean(PREF_STARRED_ONLY, false);
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
			if(mNavigationDrawerFragment==null){
				mNavigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager()
						.findFragmentById(R.id.navigation_drawer);
			}
			mNavigationDrawerFragment.setUp(R.id.navigation_drawer,
					(DrawerLayout) findViewById(R.id.drawer_layout));
		}
		else{
			//Nothing to do, is handled in LoaderFragment
		}
	}

	/* (non-Javadoc)
	 * @see com.github.federvieh.selma.PlaybarFragment.OnPlaybarInteractionListener#onLessonClicked(long, int)
	 */
	@Override
	public void onLessonClicked(long id, int trackNumber) {
		Log.d("LT", "Start lesson " + id + ", track " + trackNumber);
		//The following call clears the call stack
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
		if(title != null){
			mTitle = title;
		}
		ActionBar actionBar = getSupportActionBar();
		actionBar.setTitle(mTitle);
		if(mNavigationDrawerFragment==null){
			mNavigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
			mNavigationDrawerFragment.setUp(R.id.navigation_drawer,
					(DrawerLayout) findViewById(R.id.drawer_layout));
		}
		final Fragment f = getSupportFragmentManager().findFragmentById(R.id.container);
		boolean hasShowLessonFragment = (f.getClass().equals(ShowLessonFragment.class));
		boolean hasLessonListFragment = (f.getClass().equals(LessonListFragment.class));
		if(hasShowLessonFragment){
			SpinnerAdapter mSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.exercise_option_list,
					R.layout.spinner_dropdown_item);
			ActionBar.OnNavigationListener mOnNavigationListener = new ActionBar.OnNavigationListener() {
				@Override
				public boolean onNavigationItemSelected(int position, long itemId) {
					ListTypes lt;
					if(position==0){
						lt = ListTypes.TRANSLATE;
					}
					else{
						lt = ListTypes.NO_TRANSLATE;
					}
					((ShowLessonFragment)f).updateListType(lt);
					return true;
				}
			};
			ListTypes lt = LessonPlayer.getListType();
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
			actionBar.setListNavigationCallbacks(mSpinnerAdapter, mOnNavigationListener);
			int navItem = 0;
			switch(lt){
			case TRANSLATE:
				navItem = 0;
				break;
			case NO_TRANSLATE:
				navItem = 1;
				break;
			}
			actionBar.setSelectedNavigationItem(navItem);
			mNavigationDrawerFragment.setDrawerIndicatorEnabled(false);

			OverlayManager.showOverlayLessonContent(this);
		}
		else{
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
			mNavigationDrawerFragment.setDrawerIndicatorEnabled(true);
			if(hasLessonListFragment){
				OverlayManager.showOverlayLessonList(this);
			}
		}
	}

	private class DatabaseInitTask extends AsyncTask<Boolean, ActivityState, ActivityState> {
	    /** The system calls this to perform work in the UI thread and delivers
	      * the result from doInBackground() */
	    protected void onPostExecute(ActivityState result) {
        	FragmentManager fragmentManager = getSupportFragmentManager();
    		final Fragment f = fragmentManager.findFragmentById(R.id.container);
    		boolean hasLoaderFragment = (f!=null)&&(f.getClass().equals(LoaderFragment.class));
    		if(hasLoaderFragment){
    			((LoaderFragment)f).showWaiting(result);
    		}
    		else{
        		Log.w("LT", this.getClass().getSimpleName()+".onPostExecute(); Loading finished with state "+
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
			Log.w("LT", "Scanning started, force = "+forceScan[0]);
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
			}
			else {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			}
			boolean wasScanning = false;
			if(!forceScan[0]){
				AssimilDatabase ad = AssimilDatabase.getDatabase(getApplicationContext(), false);
				if(ad.getAllLessonHeaders().size()<=0){
					publishProgress(ActivityState.INITIAL_SCANNING_FOR_LESSONS);
					wasScanning = true;
					AssimilDatabase.getDatabase(getApplicationContext(), true);
				}
				else{
					//Lessons were already found. No need to re-scan.
				}
			}
			else{
				publishProgress(ActivityState.FORCED_SCANNING_FOR_LESSONS);
				wasScanning = true;
				AssimilDatabase.reset();
				AssimilDatabase.getDatabase(getApplicationContext(), true);
			}
			if(wasScanning){
				if(!forceScan[0]){
					return ActivityState.READY_FOR_PLAYBACK_AFTER_SCANNING;
				}
				else{
					return ActivityState.READY_FOR_PLAYBACK_AFTER_FORCED_SCANNING;
				}
			}
			return ActivityState.READY_FOR_PLAYBACK_NO_SCANNING;
		}
		
		@Override
		protected void onProgressUpdate(ActivityState...activityStates){
        	FragmentManager fragmentManager = getSupportFragmentManager();
    		final Fragment f = fragmentManager.findFragmentById(R.id.container);
    		boolean hasLoaderFragment = (f!=null)&&(f.getClass().equals(LoaderFragment.class));
    		if(hasLoaderFragment){
    			((LoaderFragment)f).showWaiting(activityStates[0]);
    		}
    		else{
        		Log.w("LT", this.getClass().getSimpleName()+".onProgressUpdate(); Loading progressed tos state "+
        				activityStates[0] + ", but no LoaderFragment exists.");
    		}
		}
	}
	
}
