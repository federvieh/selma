/**
 *
 */
package com.github.federvieh.selma.assimillib;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.github.federvieh.selma.PlaybarFragment.OnPlaybarInteractionListener;
import com.github.federvieh.selma.R;

import java.util.ArrayList;

/**
 * @author frank
 */
public class AssimilLessonListAdapter extends ArrayAdapter<AssimilLessonHeader> {
    private final ArrayList<AssimilLessonHeader> values;
    private OnPlaybarInteractionListener mListener;

    public AssimilLessonListAdapter(Activity activity, ArrayList<AssimilLessonHeader> values) {
        super(activity, R.layout.rowlayout, values);
        this.values = values;
        try {
            mListener = (OnPlaybarInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnPlaybarInteractionListener");
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Context context = parent.getContext();
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.rowlayout, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.showLessonTextViewLeft);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        AssimilLessonHeader current = values.get(position);
        textView.setText(context.getResources().getText(R.string.lesson) + " " + current.getNumber());
        AssimilOnClickListener assimilOnClickListener = new AssimilOnClickListener(current, mListener, position);
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
