/**
 * 
 */
package com.github.federvieh.selma.assimillib;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.federvieh.selma.R;

/**
 * @author frank
 *
 */
public class AssimilLessonListAdapter extends ArrayAdapter<AssimilLessonHeader> {
	private final AssimilDatabase values;
	private ListTypes lt;

	public AssimilLessonListAdapter(Context context, AssimilDatabase values, ListTypes lt) {
		super(context, R.layout.rowlayout, values);
		this.values = values;
		this.lt = lt;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Context context = parent.getContext();
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.rowlayout, parent, false);
		TextView textView = (TextView) rowView.findViewById(R.id.label);
		ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
		AssimilLessonHeader current = values.get(position);
		textView.setText(context.getResources().getText(R.string.lesson)+" "+current.getNumber());
		AssimilOnClickListener assimilOnClickListener = new AssimilOnClickListener(current, context, position, lt);
		textView.setOnClickListener(assimilOnClickListener);
		// starred?
		if (current.isStarred()) {
			imageView.setImageResource(android.R.drawable.btn_star_big_on);
		} else {
			imageView.setImageResource(android.R.drawable.btn_star_big_off);
		}
		imageView.setOnClickListener(assimilOnClickListener);

		return rowView;
	}
}
