package com.github.federvieh.selma;

import android.app.Activity;
import android.database.Cursor;
import android.support.design.widget.CollapsingToolbarLayout;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
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
public class LessonDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";
    private static final int LOADER_ID_LESSON = 0;
    private static final int LOADER_ID_LESSON_TITLE = 1;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private long mLessonId;

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
            Log.d(this.getClass().getSimpleName(), "onCreate: ID is " + getArguments().getLong(ARG_ITEM_ID));
            mLessonId = getArguments().getLong(ARG_ITEM_ID);
            Activity activity = this.getActivity();
            if(activity instanceof LessonDetailActivity) {
                activity.setTitle(R.string.lesson);
                Loader<Cursor> cursorLoader = getLoaderManager().initLoader(LOADER_ID_LESSON_TITLE, getArguments(), this);
            }
//            Toolbar appBarLayout = (Toolbar) activity.findViewById(R.id.detail_toolbar);
//            if (appBarLayout != null) {
//                appBarLayout.setTitle(R.string.lesson);//FIXME
//            }
        } else {
            Log.d(this.getClass().getSimpleName(), "onCreate: No item ID");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_lesson_detail, container, false);
//        rootView.setTag(TAG);//FIXME: What is this?

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);

        // LinearLayoutManager is used here, this will layout the elements in a similar fashion
        // to the way ListView would layout elements. The RecyclerView.LayoutManager defines how
        // elements are laid out.
        mLayoutManager = new LinearLayoutManager(getActivity());

        setRecyclerViewLayoutManager();

        Loader<Cursor> cursorLoader = getLoaderManager().initLoader(LOADER_ID_LESSON, getArguments(), this);

        // needed to indicate that the back
        // button in action bar is used
        //FIXME: How does this work in two-pane view?
        setHasOptionsMenu(true);


        return rootView;
    }

    /**
     * Set RecyclerView's LayoutManager to the one given.
     */
    public void setRecyclerViewLayoutManager() {
        int scrollPosition = 0;

        // If a layout manager has already been set, get current scroll position.
        if (mRecyclerView.getLayoutManager() != null) {
            scrollPosition = ((LinearLayoutManager) mRecyclerView.getLayoutManager())
                    .findFirstCompletelyVisibleItemPosition();
        }

        mLayoutManager = new LinearLayoutManager(getActivity());

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.scrollToPosition(scrollPosition);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(this.getClass().getSimpleName(), "onCreateLoader()");
        if(id == LOADER_ID_LESSON) {
            String[] projection = {SelmaSQLiteHelper2.TABLE_LESSONTEXTS_LESSONID,
                    SelmaSQLiteHelper2.TABLE_LESSONTEXTS_TEXTLIT,
                    SelmaSQLiteHelper2.TABLE_LESSONTEXTS_TEXT,
                    SelmaSQLiteHelper2.TABLE_LESSONTEXTS_TEXTTRANS,
                    SelmaSQLiteHelper2.TABLE_LESSONTEXTS_TEXTTYPE
            };
            CursorLoader cursorLoader;
            String selection = SelmaSQLiteHelper2.TABLE_LESSONTEXTS_LESSONID + "=" + args.getLong(ARG_ITEM_ID);
            Log.d(getClass().getSimpleName(), "selection: " + selection);
            cursorLoader = new CursorLoader(getContext(),
                    SelmaContentProvider.CONTENT_URI_LESSON_CONTENT, projection, selection, null, null);
            return cursorLoader;
        } else if (id == LOADER_ID_LESSON_TITLE) {
            String[] projection = {SelmaSQLiteHelper2.TABLE_LESSONS_LESSONNAME,
            };
            CursorLoader cursorLoader;
            String selection = SelmaSQLiteHelper2.TABLE_LESSONS_ID + "=" + args.getLong(ARG_ITEM_ID);
            Log.d(getClass().getSimpleName(), "selection: " + selection);
            cursorLoader = new CursorLoader(getContext(),
                    SelmaContentProvider.CONTENT_URI_LESSONS, projection, selection, null, null);
            return cursorLoader;
        } else {
            Log.wtf(this.getClass().getSimpleName(), "Unknown loader ID " + id);
            throw new IllegalArgumentException("Unknown loader ID " + id);
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d(this.getClass().getSimpleName(), "onLoadFinished()");
        if(loader.getId()==LOADER_ID_LESSON) {
            //FIXME: Need to set the displaymode
            LessonDetailAdapter adapter = new LessonDetailAdapter(data, DisplayMode.ORIGINAL_TEXT);
            // Set AssimilShowLessonListAdapter as the adapter for RecyclerView.
            mRecyclerView.setAdapter(adapter);
            mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
            setRecyclerViewLayoutManager();
        } else if(loader.getId()==LOADER_ID_LESSON_TITLE) {
            //There is always only one result.
            data.moveToFirst();
            getActivity().setTitle(getContext().getString(R.string.lesson) +
                    " " +
                    data.getString(data.getColumnIndex(SelmaSQLiteHelper2.TABLE_LESSONS_LESSONNAME)));
        } else {
            Log.wtf(this.getClass().getSimpleName(), "Unknown loader ID " + loader.getId());
            throw new IllegalArgumentException("Unknown loader ID " + loader.getId());
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        //FIXME: What should be done now?
        Log.d(this.getClass().getSimpleName(), "Loader reset. What now!?");
    }
}
