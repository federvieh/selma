package com.github.federvieh.selma;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.github.federvieh.selma.assimillib.AssimilDatabase;
import com.github.federvieh.selma.assimillib.AssimilLesson;
import com.github.federvieh.selma.assimillib.AssimilOnClickListener;
import com.github.federvieh.selma.assimillib.AssimilShowLessonListAdapter;
import com.github.federvieh.selma.assimillib.DisplayMode;
import com.github.federvieh.selma.assimillib.LessonPlayer;
import com.github.federvieh.selma.assimillib.ListTypes;

/**
 * A fragment representing a list of lesson tracks.
 */
public class ShowLessonFragment extends ListFragment {

	private static final String ARG_LESSON_ID = "param1";
	private static final String ARG_TRACK_NUMBER = "param2";

	private AssimilLesson lesson;
	private int tracknumber = -1;

	private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
		  @Override
		  public void onReceive(Context context, Intent intent) {
			  long lessonId = intent.getLongExtra(AssimilOnClickListener.EXTRA_LESSON_ID, -1);
			  Log.d("LT", "ShowLessonFragment.messageReceiver.onReceive() got called with lessonId "+lessonId+". Current lesson's ID is "+lesson.getHeader().getId());
			  if(lessonId == lesson.getHeader().getId()){
				  //Might now be playing a new track, update the list in order to highlight the current track
				  getListView().invalidateViews();
			  }
		  }
		};

	public static ShowLessonFragment newInstance(long lessonId, int trackNumber) {
		ShowLessonFragment fragment = new ShowLessonFragment();
		Bundle args = new Bundle();
		args.putLong(ARG_LESSON_ID, lessonId);
		args.putInt(ARG_TRACK_NUMBER, trackNumber);
		fragment.setArguments(args);
		return fragment;
	}

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public ShowLessonFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getArguments() != null) {
			long lessonId = getArguments().getLong(ARG_LESSON_ID);
			lesson = AssimilDatabase.getLesson(lessonId, getActivity());
			tracknumber = getArguments().getInt(ARG_TRACK_NUMBER);
		}

		// FIXME: Where to get list type and display mode?
		AssimilShowLessonListAdapter assimilShowLessonListAdapter;
		ListTypes lt = ListTypes.TRANSLATE;
		DisplayMode displayMode = DisplayMode.ORIGINAL_TEXT;
		assimilShowLessonListAdapter = new AssimilShowLessonListAdapter(getActivity(), lesson, lt, displayMode);

		setListAdapter(assimilShowLessonListAdapter);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState){
		super.onActivityCreated(savedInstanceState);
		if(tracknumber >= 0){
			this.setSelection(tracknumber);
			tracknumber = -1;
		}
	}

	@Override
	public void onResume() {
	  super.onResume();

	  // Register mMessageReceiver to receive messages.
	  LocalBroadcastManager.getInstance(getActivity()).registerReceiver(messageReceiver,
	      new IntentFilter(LessonPlayer.PLAY_UPDATE_INTENT));
	}

	@Override
	public void onPause() {
	  // Unregister since the activity is not visible
	  LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(messageReceiver);
	  super.onPause();
	} 
	

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
    	LessonPlayer.play(lesson, position, false, v.getContext());
	}

}
