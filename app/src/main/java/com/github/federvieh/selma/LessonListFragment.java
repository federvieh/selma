package com.github.federvieh.selma;

import android.app.Activity;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.github.federvieh.selma.dao.ScannerAssimilMP3Type1;

/**
 * A list fragment representing a list of Lessons. This fragment
 * also supports tablet devices by allowing list items to be given an
 * 'activated' state upon selection. This helps indicate which item is
 * currently being viewed in a {@link LessonDetailFragment}.
 * <p/>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class LessonListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";
    private static final String ARG_COURSE = "course_name";
    private static final String ARG_STARRED = "show_starred_only";

    private static final int LOADER_ID_DATABASE = 0;

     /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = sDummyCallbacks;

    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;
    private String mCourse;
    private boolean mShowStarredOnly;
    private Cursor mCursor;
    private int mIdxId = -1;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(this.getClass().getSimpleName(), "onCreateLoader()");
        String[] projection = {SelmaSQLiteHelper2.TABLE_LESSONS_ID,
                SelmaSQLiteHelper2.TABLE_LESSONS_LESSONNAME,
                SelmaSQLiteHelper2.TABLE_LESSONS_STARRED };
        //FIXME: build where clause using mCourse and mShowStarredOnly
        CursorLoader cursorLoader = new CursorLoader(getContext(),
                SelmaContentProvider.CONTENT_URI_LESSONS, projection, null, null, null);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d(this.getClass().getSimpleName(), "onLoadFinished()");
        mCursor = data;
        if( mCursor!= null) {
            mIdxId = mCursor.getColumnIndex(SelmaSQLiteHelper2.TABLE_LESSONS_ID);
        }

        if((data != null) && (data.getCount() > 0)){
            final int count = data.getCount();
            Log.d(this.getClass().getSimpleName(), "Found " + count + " lessons.");
            setListAdapter(new LessonListCursorAdapter(getContext(), data, 0));//TODO: What does third parameter "flags" do?
        } else {
            //FIXME: Set proper text using resources
            Log.d(this.getClass().getSimpleName(), "No lessons found.");
            setEmptyText("No lessons found.");
            setListAdapter(new LessonListCursorAdapter(getContext(), data, 0));//TODO: What does third parameter "flags" do?
            ScannerAssimilMP3Type1.startScanning(getContext());
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        //FIXME: What should be done now?
        Log.d("LT", "Loader reset. What now!?");
        loader.reset();
        mCursor = null;
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        public void onItemSelected(long id);
    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(long id) {
        }
    };

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public LessonListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getArguments()!=null) {
            if (getArguments().containsKey(ARG_COURSE)) {
                mCourse = getArguments().getString(ARG_COURSE);
            }
            if (getArguments().containsKey(ARG_STARRED)) {
                mShowStarredOnly = getArguments().getBoolean(ARG_STARRED);
            }
        }
        Loader<Cursor> cursorLoader = getLoaderManager().initLoader(LOADER_ID_DATABASE, null, this);
        //FIXME: This just tests notification and must be moved to a useful location
        ContentObserver observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                onChange(selfChange, null);
            }

            @Override
            public void onChange(boolean selfChange, Uri uri) {
                Log.i("LT", "I was " + (selfChange ? "self-" : "") + "notified about URI " + uri);
                LessonListFragment.this.getLoaderManager().restartLoader(LOADER_ID_DATABASE, null, LessonListFragment.this);
            }
        };
        //FIXME: This should only be done once! When cam we unregister?
        getContext().getContentResolver().registerContentObserver(SelmaContentProvider.CONTENT_URI_LESSONS, true, observer);
//TODO: Maybe set title
//        Activity activity = this.getActivity();
//        CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) activity.findViewById(R.id.toolbar_layout);
//        if (appBarLayout != null) {
//            appBarLayout.setTitle(mItem.content);
//        }
        // TODO: replace with a real list adapter.
        // FIXME: use Loader
//        setListAdapter(new ArrayAdapter<DummyContent.DummyItem>(
//                getActivity(),
//                android.R.layout.simple_list_item_activated_1,
//                android.R.id.text1,
//                DummyContent.ITEMS));
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        mCursor.moveToPosition(position);
        mCallbacks.onItemSelected(mCursor.getLong(mIdxId));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be
     * given the 'activated' state when touched.
     */
    public void setActivateOnItemClick(boolean activateOnItemClick) {
        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        getListView().setChoiceMode(activateOnItemClick
                ? ListView.CHOICE_MODE_SINGLE
                : ListView.CHOICE_MODE_NONE);
    }

    private void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(mActivatedPosition, false);
        } else {
            getListView().setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }
}
