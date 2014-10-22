/**
 * 
 */
package com.github.federvieh.selma;

import com.github.federvieh.selma.NavigationDrawerFragment.NavigationDrawerCallbacks;
import com.github.federvieh.selma.assimillib.AssimilDatabase;

import android.graphics.drawable.Drawable.Callback;
import android.view.View;
import android.view.View.OnClickListener;

/**
 * @author frank
 *
 */
public class CourseSelectOnClickListener implements OnClickListener {

	private String courseName;
	private boolean starredOnly;
	private NavigationDrawerFragment callback;

	/**
	 * @param callback 
	 * @param current
	 * @param b
	 */
	public CourseSelectOnClickListener(String courseName, boolean starredOnly, NavigationDrawerFragment callback) {
		this.courseName = courseName;
		this.starredOnly = starredOnly;
		this.callback = callback;
	}

	/* (non-Javadoc)
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	@Override
	public void onClick(View v) {
		callback.selectItem(courseName, starredOnly);
	}

}
