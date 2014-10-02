package com.github.federvieh.selma;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

import com.github.federvieh.selma.assimillib.AssimilDatabase;
import com.github.federvieh.selma.assimillib.AssimilLessonListAdapter;
import com.github.federvieh.selma.assimillib.AssimilOnClickListener;
import com.github.federvieh.selma.assimillib.ListTypes;
import com.github.federvieh.selma.assimillib.LoaderFragment;
import com.github.federvieh.selma.assimillib.LoaderFragment.LoaderFragmentCallbacks;

public class MainActivity extends ActionBarActivity implements
		NavigationDrawerFragment.NavigationDrawerCallbacks, LoaderFragmentCallbacks,
		PlaybarFragment.OnPlaybarInteractionListener{

	/**
	 * Fragment managing the behaviors, interactions and presentation of the
	 * navigation drawer.
	 */
//	//FIXME: Navigation drawer
//	private NavigationDrawerFragment mNavigationDrawerFragment;

	/**
	 * Used to store the last screen title. For use in
	 * {@link #restoreActionBar()}.
	 */
	private CharSequence mTitle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

//		//FIXME: Navigation drawer
//		mNavigationDrawerFragment = (NavigationDrawerFragment) getSupportFragmentManager()
//				.findFragmentById(R.id.navigation_drawer);
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
	}

	@Override
	public void onNavigationDrawerItemSelected(int position) {
		// update the main content by replacing fragments
		FragmentManager fragmentManager = getSupportFragmentManager();
		fragmentManager
				.beginTransaction()
				.replace(R.id.container,
						PlaceholderFragment.newInstance(position + 1)).commit();
	}

	public void onSectionAttached(int number) {
		switch (number) {
		case 1:
			mTitle = getString(R.string.title_section1);
			break;
		case 2:
			mTitle = getString(R.string.title_section2);
			break;
		case 3:
			mTitle = getString(R.string.title_section3);
			break;
		}
	}

	public void restoreActionBar() {
		ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(mTitle);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
//		//FIXME: Navigation drawer
//		if (!mNavigationDrawerFragment.isDrawerOpen()) {
//			// Only show items in the action bar relevant to this screen
//			// if the drawer is not showing. Otherwise, let the drawer
//			// decide what to show in the action bar.
//			getMenuInflater().inflate(R.menu.main, menu);
//			restoreActionBar();
//			return true;
//		}
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

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		private static final String ARG_SECTION_NUMBER = "section_number";

		/**
		 * Returns a new instance of this fragment for the given section number.
		 */
		public static PlaceholderFragment newInstance(int sectionNumber) {
			PlaceholderFragment fragment = new PlaceholderFragment();
			Bundle args = new Bundle();
			args.putInt(ARG_SECTION_NUMBER, sectionNumber);
			fragment.setArguments(args);
			return fragment;
		}

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			return rootView;
		}

		@Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);
			((MainActivity) activity).onSectionAttached(getArguments().getInt(
					ARG_SECTION_NUMBER));
		}
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
			ListFragment lf = new ListFragment();
			ListAdapter la = new AssimilLessonListAdapter(this, AssimilDatabase.getCurrentLessons());
			lf.setListAdapter(la);
			fragmentTransaction.replace(R.id.container, lf);
			PlaybarFragment pf = PlaybarFragment.newInstance(null, null);
			fragmentTransaction.add(R.id.playbarContainer, pf);
			fragmentTransaction.commit();
			View pb = findViewById(R.id.playbarContainer);
			pb.setVisibility(View.VISIBLE);
			// Set up the drawer.
//			//FIXME: Navigation drawer
//			mNavigationDrawerFragment.setUp(R.id.navigation_drawer,
//					(DrawerLayout) findViewById(R.id.drawer_layout));
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
		
		ShowLessonFragment slf = ShowLessonFragment.newInstance(id, trackNumber);
		fragmentTransaction.replace(R.id.container, slf);
		fragmentTransaction.addToBackStack(null);
		fragmentTransaction.commit();
	}
}
