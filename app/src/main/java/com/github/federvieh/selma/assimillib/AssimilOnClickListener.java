/**
 * 
 */
package com.github.federvieh.selma.assimillib;

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import com.github.federvieh.selma.PlaybarFragment.OnPlaybarInteractionListener;

/**
 * @author frank
 *
 */
public class AssimilOnClickListener implements OnClickListener {

	public static final String EXTRA_LESSON_ID = "com.github.federvieh.selma.assimillib.EXTRA_LESSON_ID";
	public static final String EXTRA_TRACK_INDEX = "com.github.federvieh.selma.assimillib.EXTRA_TRACK_INDEX";
	private AssimilLessonHeader lesson;
	private OnPlaybarInteractionListener listener;

	/**
	 * 
	 */
	public AssimilOnClickListener(AssimilLessonHeader lesson, OnPlaybarInteractionListener listener, int position) {
		this.lesson = lesson;
		this.listener = listener;
	}

	/* (non-Javadoc)
	 * @see android.view.View.OnClickListener#onClick(android.view.View)
	 */
	public void onClick(View v) {
		if(v instanceof ImageView){
			ImageView imageView = (ImageView) v;
			if(lesson.isStarred()){
				Log.i("LT", "unstarring");
				lesson.unstar(v.getContext());
				imageView.setImageResource(android.R.drawable.btn_star_big_off);
			}
			else{
				Log.i("LT", "starring");
				lesson.star(v.getContext());				
				imageView.setImageResource(android.R.drawable.btn_star_big_on);
			}
		}
		else{
			// Go to show lesson activity
			listener.onLessonClicked(lesson.getId(), -1);
		}
	}

}
