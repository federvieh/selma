/**
 * 
 */
package com.github.federvieh.selma;

import android.app.Activity;
import android.support.v4.app.ListFragment;

/**
 * @author frank
 *
 */
public class LessonListFragment extends ListFragment {
	private ShowLessonFragmentListener listener;

	@Override
	public void onResume() {
	  super.onResume();

	  // Register mMessageReceiver to receive messages.
	  listener.onResumedTitleUpdate("Selma");
	}
	
	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onAttach(android.app.Activity)
	 */
	@Override
	public void onAttach(Activity activity) {
		// TODO Auto-generated method stub
		super.onAttach(activity);
	}

	/**
	 * @param mainActivity
	 */
	public void setListener(ShowLessonFragmentListener listener) {
		this.listener = listener;
	}
}
