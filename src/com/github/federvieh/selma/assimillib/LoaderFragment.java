/**
 * 
 */
package com.github.federvieh.selma.assimillib;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.github.federvieh.selma.MainActivity.ActivityState;
import com.github.federvieh.selma.R;

/**
 * @author frank
 *
 */
public class LoaderFragment extends Fragment {
//	private static final String FORCE_RESET = "com.github.federvieh.selma.assimillib.FORCE_RESET";
//	private boolean reset;
	//Attribute callback has to be static in order to rotate the device while loading
	private static LoaderFragmentCallbacks mainActivity;
	private ActivityState currentState = ActivityState.DATABASE_LOADING;

	/**
	 * @param mainActivity
	 */
	public LoaderFragment(LoaderFragmentCallbacks mainActivity) {
		super();
		LoaderFragment.mainActivity = mainActivity;
	}

	public LoaderFragment() {
		super();
		Log.w("LT", "Empty Constructor called. Probably rotating.");
		if(mainActivity==null){
			Log.w("LT", "No mainActivity!");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
	        Bundle savedInstanceState){
		//TODO:From ArticleFragment (example code). What should this Fragment do instead?
        // If activity recreated (such as from screen rotate), restore
        // the previous article selection set by onSaveInstanceState().
        // This is primarily necessary when in the two-pane layout.
//        if (savedInstanceState != null) {
//            mCurrentPosition = savedInstanceState.getInt(ARG_POSITION);
//        }

        // Inflate the layout for this fragment
		Log.w("LT", this.getClass().getSimpleName()+".onCreateView(); container="+container);
        return inflater.inflate(R.layout.loader, container, false);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i("LT", this.getClass().getSimpleName()+".onCreate(); savedInstanceState="+savedInstanceState);
//		reset = false;
//		Bundle arguments = this.getArguments();
//		Log.w("LT", this.getClass().getSimpleName()+".onCreate(); arguments="+arguments);
//		if (arguments != null){
//			reset = arguments.getBoolean(FORCE_RESET, false);
//		}
//		if(reset){
//			AssimilDatabase.reset();
//		}
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onResume()
	 */
	@Override
	public void onResume() {
		super.onResume();
//		if(!AssimilDatabase.isAllocated()){
//			showWaiting(ActivityState.DATABASE_LOADING);
//			new DatabaseInitTask().execute(reset);
//		}
//		else{
//			showWaiting(ActivityState.READY_FOR_PLAYBACK);
//		}
		showWaiting(currentState);
	}
	
	/**
	 * @param b
	 */
	public void showWaiting(ActivityState databaseLoading) {
		Log.d("LT", "databaseLoading="+databaseLoading);
		currentState  = databaseLoading;
		switch(databaseLoading){
		case DATABASE_LOADING:
		{
		    TextView textView = (TextView)getView().findViewById(R.id.wait_for_database_text);
		    textView.setText(R.string.waiting_for_database);
		    break;
		}
		case FORCED_SCANNING_FOR_LESSONS:
		{
			//On forced re-scans we don't show the intro text.
			getView().findViewById(R.id.selma_description1).setVisibility(View.GONE);
			getView().findViewById(R.id.selma_description2).setVisibility(View.GONE);
		}
		//FALLTHRU
		case INITIAL_SCANNING_FOR_LESSONS:
		{
		    TextView textView = (TextView)getView().findViewById(R.id.wait_for_database_text);
		    textView.setText(R.string.refreshing_database);
		    break;
		}
		case READY_FOR_PLAYBACK_AFTER_SCANNING:
		{
			//Show continue button to start LessonListFragment (if lessons have been found)
	//		if(wasScanning){
			Button continueBtn = (Button) getView().findViewById(R.id.button_go_to_main);
			continueBtn.setEnabled(true);
			//Disable progress indicators
			getView().findViewById(R.id.wait_for_database_progress).setVisibility(View.GONE);
			getView().findViewById(R.id.selma_description2).setVisibility(View.GONE);

			/* Update the texts and buttons. */
			TextView textView = (TextView)getView().findViewById(R.id.wait_for_database_text);
			boolean lessonsFound = (AssimilDatabase.getDatabase(getActivity(), false)!= null) &&
					(AssimilDatabase.getDatabase(getActivity(), false).getAllLessonHeaders().size()!=0);
			if(lessonsFound){
				textView.setText(R.string.selma_description_scanning_finished);
				textView.setTextColor(getResources().getColor(R.color.DarkGreen));
				continueBtn.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						mainActivity.onLoadingFinished(true);
					}
				});
			}
			else{
				textView.setText(R.string.selma_description_no_content_found);
				textView.setTextColor(getResources().getColor(R.color.Red));
				continueBtn.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Go through the intro information
						mainActivity.onLoadingFinished(false);
					}
				});
				//FIXME: Uninstall button!?
			}
		}
		case READY_FOR_PLAYBACK_NO_SCANNING:
			mainActivity.onLoadingFinished(true);
		}
			//TODO: Go through the help "dialogs" if no lessons have been found
			//TODO: Must the below go into the LessonListFragment!?
//			setContentView(R.layout.activity_lesson_list);
//			setTitle("");
//			SpinnerAdapter mSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.playback_option_list,
//					android.R.layout.simple_spinner_dropdown_item);
//			ActionBar.OnNavigationListener mOnNavigationListener = new ActionBar.OnNavigationListener() {
//				@Override
//				public boolean onNavigationItemSelected(int position, long itemId) {
//					switch(lt){
//					case LIST_TYPE_ALL_NO_TRANSLATE:
//					case LIST_TYPE_STARRED_NO_TRANSLATE:
//						if(position==0){
//							lt = ListTypes.LIST_TYPE_ALL_NO_TRANSLATE;
//						}
//						else{
//							lt = ListTypes.LIST_TYPE_STARRED_NO_TRANSLATE;
//						}
//						break;
//					case LIST_TYPE_STARRED_TRANSLATE:
//					case LIST_TYPE_ALL_TRANSLATE:
//						if(position==0){
//							lt = ListTypes.LIST_TYPE_ALL_TRANSLATE;
//						}
//						else{
//							lt = ListTypes.LIST_TYPE_STARRED_TRANSLATE;
//						}
//						break;
//					}
//					Log.d("LT", this.getClass().getSimpleName()+".onNavigationItemSelected(); lt="+lt);
//					PlaybarManager.setListType(lt);
//					updateListType();
//					return true;
//				}
//			};
//			getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
//			getSupportActionBar().setListNavigationCallbacks(mSpinnerAdapter, mOnNavigationListener);
//			getSupportActionBar().setTitle("");
//			int navItem = 0;
//			switch(lt){
//			case LIST_TYPE_ALL_NO_TRANSLATE:
//			case LIST_TYPE_ALL_TRANSLATE:
//				navItem = 0;
//				break;
//			case LIST_TYPE_STARRED_NO_TRANSLATE:
//			case LIST_TYPE_STARRED_TRANSLATE:
//				navItem = 1;
//				break;
//			}
//			getSupportActionBar().setSelectedNavigationItem(navItem);
	}

	/**
	 * Callbacks interface that all activities using this fragment must
	 * implement.
	 */
	public static interface LoaderFragmentCallbacks {
		/**
		 * Called when an item in the navigation drawer is selected.
		 * @param b 
		 */
		void onLoadingFinished(boolean b);
	}

}
