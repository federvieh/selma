/**
 * 
 */
package com.github.federvieh.selma.assimillib;

import android.content.Context;
import android.graphics.Typeface;
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
public class AssimilShowLessonListAdapter extends ArrayAdapter<String> {
	private ListTypes lt;
	private Context context;
	private AssimilLesson lesson;


	public AssimilShowLessonListAdapter(Context context, AssimilLesson lesson, ListTypes lt) {
		super(context, R.layout.rowlayout, ((lt == ListTypes.LIST_TYPE_ALL_NO_TRANSLATE)||(lt ==ListTypes.LIST_TYPE_STARRED_NO_TRANSLATE))?lesson.getLessonList():lesson.getTextList());
		this.context = context;
		this.lesson = lesson;
		this.lt = lt;
	}


	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.rowlayout, parent, false);
		TextView textView = (TextView) rowView.findViewById(R.id.label);
		ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
		String current = lesson.getTextList()[position];
		textView.setText(current);
		if((position>1)&&(position<lesson.getLessonList().length)){
			textView.setTextSize(16);
			textView.setTextColor(context.getResources().getColor(R.color.DarkSlateGray));
		}
		else if(position>lesson.getLessonList().length){
			textView.setTextSize(16);
			//textView.setTextColor(context.getResources().getColor((lt==ListTypes.LIST_TYPE_ALL_TRANSLATE)?R.color.holo_blue_dark:R.color.holo_green_dark));
		}
		else if(position==lesson.getLessonList().length){
			textView.setTextSize(18);
			textView.setTextColor(context.getResources().getColor((lt==ListTypes.LIST_TYPE_ALL_TRANSLATE)?R.color.holo_blue_dark:R.color.holo_green_dark));
			textView.setTypeface(null, Typeface.ITALIC);
		}
		else{
			textView.setTextSize(18);
			textView.setTypeface(null, Typeface.ITALIC);
		}
		if((PlaybarManager.getTrackNumber()==position)&&(PlaybarManager.getLessonInstance().equals(lesson))){
			textView.setTypeface(null, Typeface.BOLD|((textView.getTypeface()!=null)?textView.getTypeface().getStyle():0));
		}
//		AssimilOnClickListener assimilOnClickListener = new AssimilOnClickListener(current, context, position, lt);
//		textView.setOnClickListener(assimilOnClickListener);
		imageView.setVisibility(View.GONE);

		return rowView;
	}
}
