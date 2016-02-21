package com.github.federvieh.selma;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by frank on 11/17/15.
 */
public class LessonListCursorAdapter extends CursorAdapter {

    private int mIdxLessonName = -1;
    private int mIdxStarred = -1;
    private int mxIdxId = -1;

    public LessonListCursorAdapter(Context context, Cursor cursor, int flags) {
        super(context, cursor, flags);
        //Assuming that the cursor is always the same in the newView/bindView methods
        //we now find out the index of the necessary columns
        if( cursor!= null) {
            mIdxLessonName = cursor.getColumnIndex(SelmaSQLiteHelper2.TABLE_LESSONS_LESSONNAME);
            mIdxStarred = cursor.getColumnIndex(SelmaSQLiteHelper2.TABLE_LESSONS_STARRED);
            mxIdxId = cursor.getColumnIndex(SelmaSQLiteHelper2.TABLE_LESSONS_ID);
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        //The cursor is already set to the dataset in question
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.rowlayout, parent, false);
        return rowView;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        Log.d(this.getClass().getSimpleName(), "bindView(): Called for position "
                + cursor.getPosition() + ": " + cursor.getString(mIdxLessonName));
        TextView textView = (TextView) view.findViewById(R.id.lessonListRowTextView);
        ImageView imageView = (ImageView) view.findViewById(R.id.icon);
        textView.setText(context.getResources().getText(R.string.lesson)
                + " " + cursor.getString(mIdxLessonName));
        //FIXME: We need a listener to star/unstar the lesson
//        AssimilOnClickListener assimilOnClickListener = new AssimilOnClickListener(current, mListener, position);
//        textView.setOnClickListener(assimilOnClickListener);
//        imageView.setOnClickListener(assimilOnClickListener);
        // starred?
        if (cursor.getLong(mIdxStarred)>0) {
            imageView.setImageResource(android.R.drawable.btn_star_big_on);
        } else {
            imageView.setImageResource(android.R.drawable.btn_star_big_off);
        }
    }
}
