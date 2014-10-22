package com.github.federvieh.selma;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import com.github.federvieh.selma.assimillib.ListTypes;
import com.github.federvieh.selma.assimillib.LoaderFragment;
import com.github.federvieh.selma.assimillib.LoaderFragment.LoaderFragmentCallbacks;

public class MainActivity extends ActionBarActivity implements
		NavigationDrawerFragment.NavigationDrawerCallbacks, LoaderFragmentCallbacks,
		PlaybarFragment.OnPlaybarInteractionListener, ShowLessonFragmentListener{

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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mTitle = getTitle();
        Intent intend = getIntent();
        long lessonTemp = intend.getLongExtra(AssimilOnClickListener.EXTRA_LESSON_ID,-1);
        int trackNumber = intend.getIntExtra(AssimilOnClickListener.EXTRA_TRACK_INDEX, -1);


        if(lessonTemp>=0){
        	onLoadingFinished(true);
        	onLessonClicked(lessonTemp, trackNumber);
        }
        else{
        	FragmentManager fragmentManager = getSupportFragmentManager();
        	fragmentManager.beginTransaction()
        	.replace(R.id.container,
        			new LoaderFragment(this)).commit();
        }
		SharedPreferences settings = getSharedPreferences("selma", Context.MODE_PRIVATE);
		int i = settings.getInt(ShowLessonFragment.LIST_MODE, ListTypes.TRANSLATE.ordinal());
		Log.d("LT", this.getClass().getSimpleName()+".onCreate(); i="+i);
		ListTypes lt = ListTypes.values()[i];
		LessonPlayer.setListType(lt);
	}

	@Override
	public void onLangItemSelected(String courseName, boolean starredOnly) {
		AssimilDatabase.setLang(courseName);
		AssimilDatabase.setStarredOnly(starredOnly);
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
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/* (non-Javadoc)
	 * @see com.github.federvieh.selma.assimillib.LoaderFragment.LoaderFragmentCallbacks#onLoadingFinished()
	 */
	@Override
	public void onLoadingFinished(boolean lessonsFound) {
		if(lessonsFound){
			//FIXME: Which lesson list (language+starred) to show must be stored as preference
			FragmentManager fragmentManager = getSupportFragmentManager();
			FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
			LessonListFragment lf = new LessonListFragment();
			lf.setListener(this);
			ListAdapter la = new AssimilLessonListAdapter(this, AssimilDatabase.getCurrentLessons());
			lf.setListAdapter(la);
			fragmentTransaction.replace(R.id.container, lf);
			PlaybarFragment pf = PlaybarFragment.newInstance(null, null);
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
			//TODO: Go through intro information
		}
	}

	/* (non-Javadoc)
	 * @see com.github.federvieh.selma.PlaybarFragment.OnPlaybarInteractionListener#onLessonClicked(long, int)
	 */
	@Override
	public void onLessonClicked(long id, int trackNumber) {
		Log.d("LT", "Start lesson " + id + ", track " + trackNumber);
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
		final Fragment f = getSupportFragmentManager().findFragmentById(R.id.container);
		boolean hasShowLessonFragment = (f.getClass().equals(ShowLessonFragment.class));
		if(hasShowLessonFragment){
			SpinnerAdapter mSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.exercise_option_list,
					android.R.layout.simple_spinner_dropdown_item);
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
		}
		else{
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
			mNavigationDrawerFragment.setDrawerIndicatorEnabled(true);
		}
	}

}
