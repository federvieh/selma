/**
 * 
 */
package com.github.federvieh.selma.assimillib;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.github.federvieh.selma.R;

/**
 * @author frank
 *
 */
public class LoaderFragment extends Fragment {
	enum ActivityState{
		DATABASE_LOADING,
		SCANNING_FOR_LESSONS,
		READY_FOR_PLAYBACK,
	}

	private static final String FORCE_RESET = "com.github.federvieh.selma.assimillib.FORCE_RESET";
	private boolean reset;
	private LoaderFragmentCallbacks mainActivity;
	private boolean wasScanning = false;

	/**
	 * @param mainActivity
	 */
	public LoaderFragment(LoaderFragmentCallbacks mainActivity) {
		super();
		this.mainActivity = mainActivity;
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
        return inflater.inflate(R.layout.loader, container, false);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		//FIXME: How to handle rotation?
		super.onCreate(savedInstanceState);
		reset = false;
		Bundle arguments = this.getArguments();
		if (arguments != null){
			arguments.getBoolean(FORCE_RESET, false);
		}
		if(reset){
			AssimilDatabase.reset();
		}
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onResume()
	 */
	@Override
	public void onResume() {
		super.onResume();
		if(!AssimilDatabase.isAllocated()){
			showWaiting(ActivityState.DATABASE_LOADING);
			new DatabaseInitTask().execute(reset);
		}
		else{
			showWaiting(ActivityState.READY_FOR_PLAYBACK);
		}
	}
	
	/**
	 * @param b
	 */
	private void showWaiting(ActivityState databaseLoading) {
		Log.d("LT", "databaseLoading="+databaseLoading);
		if(databaseLoading==ActivityState.DATABASE_LOADING){
		    TextView textView = (TextView)getView().findViewById(R.id.wait_for_database_text);
		    textView.setText(R.string.waiting_for_database);
		}
		else if(databaseLoading == ActivityState.SCANNING_FOR_LESSONS){
			//On forced re-scans we don't show the intro text.
			if(reset){
				getView().findViewById(R.id.selma_description1).setVisibility(View.GONE);
				getView().findViewById(R.id.selma_description2).setVisibility(View.GONE);
			}
		    TextView textView = (TextView)getView().findViewById(R.id.wait_for_database_text);
		    textView.setText(R.string.refreshing_database);
		}
		else {
			//Show continue button to start LessonListFragment (if lessons have been found)
			if(wasScanning){
				Button continueBtn = (Button) getView().findViewById(R.id.button_go_to_main);
				continueBtn.setEnabled(true);
				//Disable progress indicators
				getView().findViewById(R.id.wait_for_database_progress).setVisibility(View.GONE);
				getView().findViewById(R.id.selma_description2).setVisibility(View.GONE);

				/* Update the texts and buttons. */
				TextView textView = (TextView)getView().findViewById(R.id.wait_for_database_text);
			    boolean lessonsFound = (AssimilDatabase.getDatabase(getActivity())!= null) &&
			    		(AssimilDatabase.getDatabase(getActivity()).size()!=0);
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
			else{ //if(wasScanning)
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
	}

	private class DatabaseInitTask extends AsyncTask<Boolean, ActivityState, ActivityState> {
	    /** The system calls this to perform work in the UI thread and delivers
	      * the result from doInBackground() */
	    protected void onPostExecute(ActivityState result) {
	    	showWaiting(result);
	    }

		/* (non-Javadoc)
		 * @see android.os.AsyncTask#doInBackground(java.lang.Object[])
		 */
		@Override
		protected ActivityState doInBackground(Boolean... forceScan) {
			if(!forceScan[0]){
				AssimilDatabase ad = AssimilDatabase.getDatabase(getActivity(), false);
				if(ad.size()<=0){
					publishProgress(ActivityState.SCANNING_FOR_LESSONS);
					wasScanning = true;
					AssimilDatabase.getDatabase(getActivity(), true);
				}
				else{
					//Lessons were already found. No need to re-scan.
				}
			}
			else{
				publishProgress(ActivityState.SCANNING_FOR_LESSONS);
				wasScanning = true;
				AssimilDatabase.getDatabase(getActivity(), true);
			}
			return ActivityState.READY_FOR_PLAYBACK;
		}
		
		@Override
		protected void onProgressUpdate(ActivityState...activityStates){
			showWaiting(activityStates[0]);
		}
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
