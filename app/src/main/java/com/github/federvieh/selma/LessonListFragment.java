/*
 * Copyright (C) 2016 Frank Oltmanns (frank.oltmanns+selma(at)gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.federvieh.selma;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.github.federvieh.selma.dao.ScannerAssimilMP3Type1;

import java.util.Date;
import java.util.concurrent.FutureTask;

/**
 * A list fragment representing a list of Lessons. This fragment
 * also supports tablet devices by allowing list items to be given an
 * 'activated' state upon selection. This helps indicate which item is
 * currently being viewed in a {@link LessonDetailFragment}.
 * <p/>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 * <p/>
 * When opening this Fragment:
 * - A list of lessons is being loaded from the database
 * - The scanners are being started (looking in the Android medida library for lessons) if it has
 * not been started in the last ten minutes.
 * <p/>
 * When a lesson in this list is clicked the underlying activity is informed (which should then
 * show the lesson in a {@link LessonDetailFragment}).
 * <p/>
 * The list of lessons is by default the complete list of all lessons in the database. This can be
 * changed by calling the {@link #setCourse(String, boolean)} method.
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
    private static final long MIN_TIME_SINCE_LAST_SCAN = (10 * 60 * 1000); // Ten minutes

    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        private long lastPlayedLessonId = -1;

        @Override
        public void onReceive(Context context, Intent intent) {
            ListAdapter la = getListAdapter();
            if(la!=null) {
                long lessonId = intent.getLongExtra(LessonPlayer.EXTRA_LESSON_ID, -1);
                if (lessonId != lastPlayedLessonId) {
                    //TODO: Test me!
                    ((LessonListCursorAdapter)la).notifyDataSetChanged();
                }
                lastPlayedLessonId = lessonId;
            }
        }
    };
    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = sDummyCallbacks;

    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;
    //    private String mCourse;
//    private boolean mShowStarredOnly;
    private Cursor mCursor;
    private int mIdxId = -1;
    private static long lastScanTime = 0;
    private long selectedLessonId = -1;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(this.getClass().getSimpleName(), "onCreateLoader()");
        String[] projection = {SelmaSQLiteHelper2.TABLE_LESSONS_ID,
                SelmaSQLiteHelper2.TABLE_LESSONS_LESSONNAME,
                SelmaSQLiteHelper2.TABLE_LESSONS_STARRED};
        CursorLoader cursorLoader;
        if (args != null) {
            StringBuffer selection = new StringBuffer();
            if (args.getString(ARG_COURSE) != null) {
                selection
                        .append(SelmaSQLiteHelper2.TABLE_LESSONS_COURSENAME)
                        .append("='")
                        .append(args.getString(ARG_COURSE))
                        .append("'");
            }
            if (args.getBoolean(ARG_STARRED)) {
                if (selection.length() > 0) {
                    selection.append(" AND ");
                }
                selection
                        .append(SelmaSQLiteHelper2.TABLE_LESSONS_STARRED)
                        .append("<>0");
            }
            Log.d(getClass().getSimpleName(), "selection: " + selection);
            cursorLoader = new CursorLoader(getContext(),
                    SelmaContentProvider.CONTENT_URI_LESSONS, projection, selection.toString(), null, null);
        } else {
            cursorLoader = new CursorLoader(getContext(),
                    SelmaContentProvider.CONTENT_URI_LESSONS, projection, null, null, null);
        }
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d(this.getClass().getSimpleName(), "onLoadFinished()");
        mCursor = data;
        if (mCursor != null) {
            mIdxId = mCursor.getColumnIndex(SelmaSQLiteHelper2.TABLE_LESSONS_ID);
        }

        if ((data != null) && (data.getCount() > 0)) {
            final int count = data.getCount();
            Log.d(this.getClass().getSimpleName(), "Found " + count + " lessons.");
            ListAdapter ca = getListAdapter();
            if (ca != null && ca instanceof LessonListCursorAdapter) {
                ((LessonListCursorAdapter) ca).swapCursor(data);
                int pos = getPosFromId(selectedLessonId);
                this.setActivatedPosition(pos);
            } else {
                ca = new LessonListCursorAdapter(getContext(), data, 0);
                setListAdapter(ca);
                Log.d(this.getClass().getSimpleName(), "adapter has " + ca.getCount() + " lessons.");
            }
        } else {
            //FIXME: Set proper text using resources
            Log.d(this.getClass().getSimpleName(), "No lessons found.");
            setEmptyText("No lessons found.");
            setListAdapter(new LessonListCursorAdapter(getContext(), data, 0));
        }
        long now = (new Date()).getTime();
        if (lastScanTime < (now - MIN_TIME_SINCE_LAST_SCAN)) {
            lastScanTime = now;
            ScannerAssimilMP3Type1.startScanning(getActivity().getApplicationContext());
        } else {
            Log.d(this.getClass().getSimpleName(), "Skipped scanning, last scan was " + (now - lastScanTime) + " milliseconds ago.");
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        //FIXME: What should be done now?
        Log.d("LT", "Loader reset. What now!?");
        mCursor = null;
    }

    public void setCourse(String courseName, boolean starred) {
        Bundle arguments = new Bundle();
        if (!courseName.equals(getString(R.string.all_courses))) {
            arguments.putString(ARG_COURSE, courseName);
        }
        arguments.putBoolean(ARG_STARRED, starred);
        getLoaderManager().restartLoader(LOADER_ID_DATABASE, arguments, LessonListFragment.this);
    }

    /**
     * Return the position of the requested lesson.
     *
     * @param lessonId ID of lesson
     * @return the position of the lesson in the list. Or -1 if not found.
     */
    private int getPosFromId(long lessonId) {
        boolean emptyCursor = true;
        if (mCursor != null) {
            emptyCursor = !mCursor.moveToFirst();
        }
        if (emptyCursor) {
            return -1;
        }
        int pos = 0;
        do {
            if (mCursor.getLong(mIdxId) == lessonId) {
                Log.d(getClass().getSimpleName(), "Found position of lesson " + lessonId + " at position " + pos);
                return pos;
            }
            pos++;
        } while (mCursor.moveToNext());
        //Not found
        Log.d(getClass().getSimpleName(), "Lesson " + lessonId + " not found in list");
        return -1;
    }

    public void setSelectedLesson(long lessonId) {
        int pos = getPosFromId(lessonId);
        this.selectedLessonId = lessonId;
        setActivatedPosition(pos);
//        setSelection(pos);
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         *
         * @param id The ID of the selected item
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

//        if(getArguments()!=null) {
//            if (getArguments().containsKey(ARG_COURSE)) {
//                mCourse = getArguments().getString(ARG_COURSE);
//            }
//            if (getArguments().containsKey(ARG_STARRED)) {
//                mShowStarredOnly = getArguments().getBoolean(ARG_STARRED);
//            }
//        }
        Loader<Cursor> cursorLoader = getLoaderManager().initLoader(LOADER_ID_DATABASE, getArguments(), this);
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
        //FIXME: This should only be done once! When can we unregister?
        getContext().getContentResolver().registerContentObserver(SelmaContentProvider.CONTENT_URI_LESSONS, true, observer);
//TODO: Maybe set title
//        Activity activity = this.getActivity();
//        CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) activity.findViewById(R.id.toolbar_layout);
//        if (appBarLayout != null) {
//            appBarLayout.setTitle(mItem.content);
//        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d(this.getClass().getSimpleName(), "onViewCreated()");
        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        } else {
            //FIXME: This doesn't work. When can we activate the current position?
            setActivatedPosition(getPosFromId(selectedLessonId));
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

    @Override
    public void onResume() {
        super.onResume();

        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(messageReceiver,
                new IntentFilter(LessonPlayer.PLAY_UPDATE_INTENT));
    }

    @Override
    public void onPause() {
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(messageReceiver);
        super.onPause();
    }
}
