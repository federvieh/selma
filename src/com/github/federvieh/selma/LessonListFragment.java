/**
 * 
 */
package com.github.federvieh.selma;

import com.github.federvieh.selma.assimillib.AssimilDatabase;
import com.github.federvieh.selma.assimillib.AssimilLessonListAdapter;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.widget.ListAdapter;

/**
 * @author frank
 *
 */
public class LessonListFragment extends ListFragment {
	private ShowLessonFragmentListener listener;

	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i("LT", this.getClass().getSimpleName()+".onCreate(); savedInstanceState="+savedInstanceState);
	}

	@Override
	public void onResume() {
		super.onResume();
		ListAdapter la = new AssimilLessonListAdapter(getActivity(), AssimilDatabase.getCurrentLessons());
		this.setListAdapter(la);

		listener.onResumedTitleUpdate("Selma");
	}
	
	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onAttach(android.app.Activity)
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.listener = (ShowLessonFragmentListener) activity;
	}
}
