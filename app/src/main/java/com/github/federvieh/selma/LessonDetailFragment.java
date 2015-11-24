package com.github.federvieh.selma;

import android.app.Activity;
import android.support.design.widget.CollapsingToolbarLayout;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A fragment representing a single Lesson detail screen.
 * This fragment is either contained in a {@link LessonListActivity}
 * in two-pane mode (on tablets) or a {@link LessonDetailActivity}
 * on handsets.
 */
public class LessonDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public LessonDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.

            Log.d(this.getClass().getSimpleName(), "onCreate: ID is " + getArguments().getLong(ARG_ITEM_ID));
            Activity activity = this.getActivity();
            CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) activity.findViewById(R.id.toolbar_layout);
            if (appBarLayout != null) {
                appBarLayout.setTitle("TODO");//FIXME
            }
        } else {
            Log.d(this.getClass().getSimpleName(), "onCreate: No item ID");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_lesson_detail, container, false);

        // Show the dummy content as text in a TextView.
        ((TextView) rootView.findViewById(R.id.lesson_detail)).setText("TODO");//FIXME

        return rootView;
    }
}
