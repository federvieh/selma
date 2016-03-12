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

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;


/**
 * An activity representing a list of Lessons. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link LessonDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link LessonListFragment} and the item details
 * (if present) is a {@link LessonDetailFragment}.
 * <p>
 * This activity also implements the required
 * {@link LessonListFragment.Callbacks} interface
 * to listen for item selections.
 */
public class LessonListActivity extends AppCompatActivity
        implements LessonListFragment.Callbacks, NavigationView.OnNavigationItemSelectedListener, LoaderManager.LoaderCallbacks<Cursor> {

    private static final String PREF_STARRED_ONLY = "PREF_STARRED_ONLY";
    private static final String PREF_CURRENT_COURSE = "PREF_CURRENT_COURSE";

    private static final int LOADER_ID_DATABASE = 0;
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private static boolean sTwoPane;

    public static boolean getTwoPane() {
        return sTwoPane;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        if (findViewById(R.id.lesson_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            sTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            ((LessonListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.lesson_list))
                    .setActivateOnItemClick(true);
        }

        getSupportLoaderManager().initLoader(LOADER_ID_DATABASE, null, this);

        //If new courses have been added, the list of courses needs to be reloaded to refresh the navigation view.
        ContentObserver observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                onChange(selfChange, null);
            }

            @Override
            public void onChange(boolean selfChange, Uri uri) {
                Log.i("LT", "I was " + (selfChange ? "self-" : "") + "notified about URI " + uri);
                LessonListActivity.this.getSupportLoaderManager().restartLoader(LOADER_ID_DATABASE, null, LessonListActivity.this);
            }
        };
        //FIXME: This should only be done once! When can we unregister?
        getContentResolver().registerContentObserver(SelmaContentProvider.CONTENT_URI_COURSES, true, observer);

        //Which lesson list (language+starred) to show is stored as preference
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
        String currCourse = sp.getString(PREF_CURRENT_COURSE, getString(R.string.all_courses));
        final boolean starredOnly = sp.getBoolean(PREF_STARRED_ONLY, false);
        onLangItemSelected(currCourse, starredOnly);

        //Called from notification
        if (getIntent() != null) {
            long lessonId = getIntent().getLongExtra(LessonDetailFragment.ARG_ITEM_ID, -1);
            if (lessonId != -1) {
                //FIXME: Highlight the lesson
                if (sTwoPane) {
                    LessonListFragment llf = ((LessonListFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.lesson_list));
                    //Hmm... Highlighting the lessons seems only to work when the fragment has been fully loaded
//                    int lessonPos = llf.getPosFromId(lessonId);
//                    llf.setSelection(lessonPos);
//                    llf.setSelectedLesson(lessonId);
                }
                onItemSelected(lessonId);
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Callback method from {@link LessonListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(long id) {
        if (sTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.

            Bundle arguments = new Bundle();
            arguments.putLong(LessonDetailFragment.ARG_ITEM_ID, id);
            LessonDetailFragment fragment = new LessonDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.lesson_detail_container, fragment)
                    .commit();
            LessonListFragment llf = ((LessonListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.lesson_list));
            //Hmm... Highlighting the lessons seems only to work when the fragment has been fully loaded
//                    int lessonPos = llf.getPosFromId(lessonId);
//                    llf.setSelection(lessonPos);
                    llf.setSelectedLesson(id);
        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, LessonDetailActivity.class);
            detailIntent.putExtra(LessonDetailFragment.ARG_ITEM_ID, id);
            startActivity(detailIntent);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        //FIXME: Implement static item selection
        CharSequence title = menuItem.getTitle();
        Log.d("LT", "Navigation item " + title);
        if (title.equals(getString(R.string.action_show_tips))) {
            //OverlayManager.resetOverlays();
        } else if (title.equals(getString(R.string.action_license))) {
            //showLicense();
        } else if (title.equals(getString(R.string.settings))) {
            //TODO: What kind of settings are to be expected
        } else {
            boolean starred;
            if (title.equals(getString(R.string.all_lessons))) {
                starred = false;
            } else if (title.equals(getString(R.string.starred_lessons))) {
                starred = true;
            } else {
                return false;
            }
            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            final Menu menu = navigationView.getMenu();
            String courseName = findCoursename(menu, menuItem, starred);
            onLangItemSelected(courseName, starred);
            boolean ic = menuItem.isCheckable();
            if (!ic) {
                menuItem.setCheckable(true);
            }
            menuItem.setChecked(true);
            uncheckOthers(menuItem, menu);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void onLangItemSelected(String courseName, boolean starred) {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(this);
        sp.edit().putBoolean(PREF_STARRED_ONLY, starred).putString(PREF_CURRENT_COURSE, courseName)
                .commit();

        setTitle(courseName);
        if (!courseName.equals(getString(R.string.all_courses))) {
            LessonPlayer.setCourseName(courseName);
        } else {
            LessonPlayer.setCourseName(null);
        }
        LessonPlayer.setStarredOnly(starred);

        ((LessonListFragment) getSupportFragmentManager()
                .findFragmentById(R.id.lesson_list))
                .setCourse(courseName, starred);
    }

    private String findCoursename(Menu m, MenuItem menuItem, boolean isStarred) {
        for (int i = 0; i < m.size(); i++) {
            MenuItem menuItemIter = m.getItem(i);
            if (menuItemIter.hasSubMenu()) {
                SubMenu sm = menuItemIter.getSubMenu();
                int index = isStarred ? 1 : 0;
                if (sm.getItem(index).equals(menuItem)) {
                    return menuItemIter.getTitle().toString();
                } else {
                    //continue
                }
            }
        }
        return null;
    }

    /**
     * De-select all other menu items after selecting an item in the navigation view. This is necessary due to a
     * bug in the NavigationView: https://code.google.com/p/android/issues/detail?id=175216
     * <p>
     * TODO: Check once in a while if issue 175216 has been solved.
     *
     * @param menuItem
     * @param m
     */
    private void uncheckOthers(MenuItem menuItem, Menu m) {
        for (int i = 0; i < m.size(); i++) {
            MenuItem menuItemIter = m.getItem(i);
            if (menuItemIter.equals(menuItem)) {
                //This is the menu item we actually want to keep checked. So, do nothing with it.
            } else {
                menuItemIter.setChecked(true);
                menuItemIter.setChecked(false);
                if (menuItemIter.hasSubMenu()) {
                    //recursive call on submenus
                    uncheckOthers(menuItem, menuItemIter.getSubMenu());
                }
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(this.getClass().getSimpleName(), "onCreateLoader()");
        String[] projection = {SelmaSQLiteHelper2.TABLE_LESSONS_COURSENAME};
        CursorLoader cursorLoader = new CursorLoader(this,
                SelmaContentProvider.CONTENT_URI_COURSES, projection, null, null, null);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d(this.getClass().getSimpleName(), "onLoadFinished()");

        if (data != null) {
        }

        if ((data != null) && (data.getCount() > 0)) {
            final int count = data.getCount();
            int idxCourseName = data.getColumnIndex(SelmaSQLiteHelper2.TABLE_LESSONS_COURSENAME);
            Log.d(this.getClass().getSimpleName(), "Found " + count + " courses.");
            //TODO: Highlight the currently selected course
            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            final Menu menu = navigationView.getMenu();
            menu.clear();
            if (data.getCount() > 1) {
                SubMenu topChannelMenu = menu.addSubMenu(getString(R.string.all_courses));
                topChannelMenu.add(getString(R.string.all_lessons));
                topChannelMenu.add(getString(R.string.starred_lessons));
            }
            data.moveToPosition(-1);
            while (data.moveToNext()) {
                SubMenu topChannelMenu = menu.addSubMenu(data.getString(idxCourseName));
                topChannelMenu.add(getString(R.string.all_lessons));
                topChannelMenu.add(getString(R.string.starred_lessons));
            }
            menu.add(R.string.action_show_tips);
            menu.add(R.string.action_license);
            menu.add(R.string.settings);

            //TODO: https://code.google.com/p/android/issues/detail?id=176300
            MenuItem mi = menu.getItem(menu.size() - 1);
            mi.setTitle(mi.getTitle());

        } else {
            Log.d(this.getClass().getSimpleName(), "No lessons found.");
            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            final Menu menu = navigationView.getMenu();
            menu.clear();
            SubMenu topChannelMenu = menu.addSubMenu(getString(R.string.all_courses));
            topChannelMenu.add(getString(R.string.all_lessons));
            topChannelMenu.add(getString(R.string.starred_lessons));
            menu.add(R.string.action_show_tips);
            menu.add(R.string.action_license);
            menu.add(R.string.settings);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(this.getClass().getSimpleName(), "Loader reset: Removing all items from nav drawer.");
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        final Menu menu = navigationView.getMenu();
        menu.clear();
        SubMenu topChannelMenu = menu.addSubMenu(getString(R.string.all_courses));
        topChannelMenu.add(getString(R.string.all_lessons));
        topChannelMenu.add(getString(R.string.starred_lessons));
        menu.add(R.string.action_show_tips);
        menu.add(R.string.action_license);
        menu.add(R.string.settings);
    }
}
