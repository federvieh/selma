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
import com.github.federvieh.selma.assimillib.dao.SelmaSQLiteHelper.TextType;

/** Adapter for showing all the texts in a lesson.
 * @author frank
 *
 */
public class AssimilShowLessonListAdapter extends ArrayAdapter<String> {
	private AssimilLesson lesson;
	private DisplayMode displayMode;


	public AssimilShowLessonListAdapter(Context context, AssimilLesson lesson, ListTypes lt, DisplayMode displayMode) {
		super(context, R.layout.rowlayout, (lt == ListTypes.NO_TRANSLATE)?lesson.getLessonList(displayMode):lesson.getTextList(displayMode));
		this.lesson = lesson;
		this.displayMode = displayMode;
	}


	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Context context = parent.getContext();
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.rowlayout, parent, false);
		TextView textView = (TextView) rowView.findViewById(R.id.label);
		ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
		String current = lesson.getTextList(displayMode)[position];
		TextType textType = lesson.getTextTypeList()[position];
		textView.setText(current);
		switch(textType){
		case TRANSLATE_HEADING:
			textView.setTextColor(context.getResources().getColor(R.color.DarkSlateGray));
			//FALLTHROUGH
		case LESSONNUMBER:
		case HEADING:
			textView.setTextSize(18);
			textView.setTypeface(null, Typeface.ITALIC);
			break;
		case TRANSLATE:
			textView.setTextColor(context.getResources().getColor(R.color.DarkSlateGray));
			//FALLTHROUGH
		case NORMAL:
			textView.setTextSize(16);
			break;
		}
		if((LessonPlayer.getTrackNumber(getContext())==position)&&(LessonPlayer.getLesson(getContext()).getHeader().equals(lesson.getHeader()))){
			textView.setTypeface(null, Typeface.BOLD|((textView.getTypeface()!=null)?textView.getTypeface().getStyle():0));
		}
//		AssimilOnClickListener assimilOnClickListener = new AssimilOnClickListener(current, context, position, lt);
//		textView.setOnClickListener(assimilOnClickListener);
		imageView.setVisibility(View.GONE);

		return rowView;
	}
}
