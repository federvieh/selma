/**
 * 
 */
package com.github.federvieh.selma;

import java.util.ArrayList;

import com.github.federvieh.selma.NavigationDrawerFragment.NavigationDrawerCallbacks;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * @author frank
 *
 */
public class CourseListAdapter extends ArrayAdapter<String> {

	private ArrayList<String> allCourses;
	private NavigationDrawerFragment callback;

	/**
	 * @param allCourses
	 */
	public CourseListAdapter(Context ctxt, ArrayList<String> allCourses, NavigationDrawerFragment navigationDrawerFragment) {
		super(ctxt, R.layout.course_navigation_item, R.id.courseName, allCourses);
		this.allCourses = allCourses;
		this.callback = navigationDrawerFragment;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Context context = parent.getContext();
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.course_navigation_item, parent, false);
		TextView textView = (TextView) rowView.findViewById(R.id.courseName);
		String current = allCourses.get(position);
		textView.setText(current);
		rowView.setClickable(false);
		//Set OnClickListeners
		rowView.findViewById(R.id.textViewAllLessons).setOnClickListener(new CourseSelectOnClickListener(current, false, callback));
		rowView.findViewById(R.id.textViewStarredLessons).setOnClickListener(new CourseSelectOnClickListener(current, true, callback));

		return rowView;
	}
}
