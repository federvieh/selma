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
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;

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
    private static final String LAST_DISPLAY_MODE = "LAST_DISPLAY_MODE";
    public static final String LAST_LIST_TYPE = "LAST_LIST_TYPE";
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private long mLessonId;

    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        private long lastPlayedLessonId = -1;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mAdapter != null) {
                long lessonId = intent.getLongExtra(LessonPlayer.EXTRA_LESSON_ID, -1);
                long curShownLessonId = mAdapter.getLessonId();
                Log.d("LT", "ShowLessonFragment.messageReceiver.onReceive() got called with lessonId " + lessonId +
                        ". Current lesson's ID is " + curShownLessonId + ". Last lesson ID is " + lastPlayedLessonId);
                if (lessonId == curShownLessonId) {
                    //Might now be playing a new track, update the list in order to highlight the current track
                    mAdapter.notifyItemChanged(LessonPlayer.getPreviousTrack());
                    mAdapter.notifyItemChanged(LessonPlayer.getTrackNumber(context));
                } else if (lessonId != lastPlayedLessonId) {
                    //Currently one item is shown in bold, but we are now playing a different
                    //lesson. So, the list has to be re-drawn.
                    //TODO: Test me!
                    mAdapter.notifyDataSetChanged();
                }
                lastPlayedLessonId = lessonId;
            }
        }
    };
    private LessonDetailAdapter mAdapter;
    private DisplayMode displayMode = DisplayMode.ORIGINAL_TEXT;
    private ListTypes listType;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public LessonDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sp = getContext().getSharedPreferences("selma", Context.MODE_PRIVATE);
        int ldm = sp.getInt(LAST_DISPLAY_MODE, DisplayMode.ORIGINAL_TEXT.ordinal());
        displayMode = DisplayMode.values()[ldm];

        int llt = sp.getInt(LAST_LIST_TYPE, ListTypes.ALL.ordinal());
        listType = ListTypes.values()[llt];

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            Log.d(this.getClass().getSimpleName(), "onCreate: ID is " + getArguments().getLong(ARG_ITEM_ID));
            mLessonId = getArguments().getLong(ARG_ITEM_ID);
            Activity activity = this.getActivity();
            if (activity instanceof LessonDetailActivity) {
                activity.setTitle(R.string.lesson);
            }
            Loader<Cursor> cursorLoader = getLoaderManager().initLoader(LOADER_ID_LESSON_TITLE, getArguments(), this);
//            Toolbar appBarLayout = (Toolbar) activity.findViewById(R.id.detail_toolbar);
//            if (appBarLayout != null) {
//                appBarLayout.setTitle(R.string.lesson);//FIXME
//            }
        } else {
            Log.d(this.getClass().getSimpleName(), "onCreate: No item ID");
        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        SubMenu displayModeMenu = menu.addSubMenu(R.string.display_mode);
        inflater.inflate(R.menu.display_modes, displayModeMenu);
        SubMenu listTypeMenu = menu.addSubMenu(R.string.filter);
        inflater.inflate(R.menu.list_type, listTypeMenu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_display_mode_original:
                setDisplayMode(DisplayMode.ORIGINAL_TEXT);
                return true;
            case R.id.action_display_mode_translation:
                setDisplayMode(DisplayMode.TRANSLATION);
                return true;
            case R.id.action_display_mode_literal:
                setDisplayMode(DisplayMode.LITERAL);
                return true;
            case R.id.action_display_mode_original_translation:
                setDisplayMode(DisplayMode.ORIGINAL_TRANSLATION);
                return true;
            case R.id.action_display_mode_original_literal:
                setDisplayMode(DisplayMode.ORIGINAL_LITERAL);
                return true;
            case R.id.action_list_type_all:
                setListType(ListTypes.ALL);
                return true;
            case R.id.action_list_type_show_no_translation:
                setListType(ListTypes.NO_TRANSLATE);
                return true;
            case R.id.action_list_type_show_only_translation:
                setListType(ListTypes.ONLY_TRANSLATE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
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
        if (id == LOADER_ID_LESSON) {
            String[] projection = {SelmaSQLiteHelper2.TABLE_LESSONTEXTS_LESSONID,
                    SelmaSQLiteHelper2.TABLE_LESSONTEXTS_TEXTLIT,
                    SelmaSQLiteHelper2.TABLE_LESSONTEXTS_TEXT,
                    SelmaSQLiteHelper2.TABLE_LESSONTEXTS_TEXTTRANS,
                    SelmaSQLiteHelper2.TABLE_LESSONTEXTS_TEXTTYPE
            };
            CursorLoader cursorLoader;
            String selection = SelmaSQLiteHelper2.getSelectionQuery(args.getLong(ARG_ITEM_ID), listType);
            Log.d(getClass().getSimpleName(), "selection: " + selection);
            cursorLoader = new CursorLoader(getContext(),
                    SelmaContentProvider.CONTENT_URI_LESSON_CONTENT, projection, selection, null, null);
            return cursorLoader;
        } else if (id == LOADER_ID_LESSON_TITLE) {
            String[] projection = {
                    SelmaSQLiteHelper2.TABLE_LESSONS_LESSONNAME,
                    SelmaSQLiteHelper2.TABLE_LESSONS_COURSENAME,
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
        if (loader.getId() == LOADER_ID_LESSON) {
            mAdapter = new LessonDetailAdapter(data, this.displayMode, listType, getContext());
            mRecyclerView.setAdapter(mAdapter);
            mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
            setRecyclerViewLayoutManager();
        } else if (loader.getId() == LOADER_ID_LESSON_TITLE) {
            //There is always only one result.
            data.moveToFirst();
            Activity activity = getActivity();
            if (activity instanceof LessonDetailActivity) {
                activity.setTitle(
                        getContext().getString(R.string.lesson) +
                                " " +
                                data.getString(data.getColumnIndex(
                                        SelmaSQLiteHelper2.TABLE_LESSONS_LESSONNAME)));
            } else {
                activity.setTitle(
                        data.getString(data.getColumnIndex(
                                SelmaSQLiteHelper2.TABLE_LESSONS_COURSENAME)) + ": " +
                                getContext().getString(R.string.lesson) +
                                " " +
                                data.getString(data.getColumnIndex(
                                        SelmaSQLiteHelper2.TABLE_LESSONS_LESSONNAME)));
            }
        } else {
            Log.wtf(this.getClass().getSimpleName(), "Unknown loader ID " + loader.getId());
            throw new IllegalArgumentException("Unknown loader ID " + loader.getId());
        }
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

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        //FIXME: What should be done now?
        Log.d(this.getClass().getSimpleName(), "Loader reset. What now!?");
    }

    public void setDisplayMode(DisplayMode displayMode) {
        this.displayMode = displayMode;
        SharedPreferences.Editor editor = getContext().getSharedPreferences("selma", Context.MODE_PRIVATE).edit();
        editor
                .putInt(LAST_DISPLAY_MODE, displayMode.ordinal())
                .commit();

        if (mAdapter != null) {
            mAdapter.setDisplayMode(this.displayMode);
            mAdapter.notifyDataSetChanged();
        }
    }

    public void setListType(ListTypes listType) {
        this.listType = listType;
        SharedPreferences.Editor editor = getContext().getSharedPreferences("selma", Context.MODE_PRIVATE).edit();
        editor
                .putInt(LAST_LIST_TYPE, listType.ordinal())
                .commit();
        getLoaderManager().restartLoader(LOADER_ID_LESSON, getArguments(), this);
        LessonPlayer.setListType(listType, getContext());
    }
}
