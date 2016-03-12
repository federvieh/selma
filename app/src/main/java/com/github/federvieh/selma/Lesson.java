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

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;

public class Lesson {
    private final String mLessonName;
    private final Cursor mCursor;
    private final int mIdxPath;
    private final int mIdxTextId;
    private final String mCourseName;
    private long id;

    public Lesson(long id, Context context) {
        ContentResolver cr = context.getContentResolver();
        this.id = id;
        {
            String[] projection = {SelmaSQLiteHelper2.TABLE_LESSONS_LESSONNAME, SelmaSQLiteHelper2.TABLE_LESSONS_COURSENAME};
            String selection = SelmaSQLiteHelper2.TABLE_LESSONS_ID + " = " + id;
            Cursor lessonCursor = cr.query(SelmaContentProvider.CONTENT_URI_LESSONS, projection, selection, null, null);
            lessonCursor.moveToFirst();
            mLessonName = lessonCursor.getString(lessonCursor.getColumnIndex(SelmaSQLiteHelper2.TABLE_LESSONS_LESSONNAME));
            mCourseName = lessonCursor.getString(lessonCursor.getColumnIndex(SelmaSQLiteHelper2.TABLE_LESSONS_COURSENAME));
            lessonCursor.close();
        }

        {
            String[] projection = {SelmaSQLiteHelper2.TABLE_LESSONTEXTS_AUDIOFILEPATH, SelmaSQLiteHelper2.TABLE_LESSONTEXTS_TEXTID};
            String selection = SelmaSQLiteHelper2.TABLE_LESSONTEXTS_LESSONID + " = " + id;
            mCursor = cr.query(SelmaContentProvider.CONTENT_URI_LESSON_CONTENT, projection, selection, null, SelmaSQLiteHelper2.TABLE_LESSONTEXTS_TEXTID);
            mIdxPath = mCursor.getColumnIndex(SelmaSQLiteHelper2.TABLE_LESSONTEXTS_AUDIOFILEPATH);
            mIdxTextId = mCursor.getColumnIndex(SelmaSQLiteHelper2.TABLE_LESSONTEXTS_TEXTID);
        }

    }

    public String getPathByTrackNo(int trackNumber) {
        mCursor.moveToPosition(trackNumber);
        return mCursor.getString(mIdxPath);
    }

    public String getTextNumber(int trackNumber) {
        mCursor.moveToPosition(trackNumber);
        return mCursor.getString(mIdxTextId);
    }

    public long getId() {
        return id;
    }

    public Lesson getNextLesson(boolean starredOnly, String courseName, LessonPlayer.PlayMode pm, Context context) {
        ContentResolver cr = context.getContentResolver();
        Lesson rv = null;
        String[] projection = {SelmaSQLiteHelper2.TABLE_LESSONS_LESSONNAME, SelmaSQLiteHelper2.TABLE_LESSONS_ID, SelmaSQLiteHelper2.TABLE_LESSONS_COURSENAME};
        String selection = courseName != null ? SelmaSQLiteHelper2.TABLE_LESSONS_COURSENAME + " = '" + courseName + "'" : null;
        if(starredOnly){
            if(selection != null) {
                selection += " AND " + SelmaSQLiteHelper2.TABLE_LESSONS_STARRED + " != 0";
            } else {
                selection = SelmaSQLiteHelper2.TABLE_LESSONS_STARRED + " != 0";
            }
        }
        Cursor lessonCursor = cr.query(SelmaContentProvider.CONTENT_URI_LESSONS, projection, selection, null, null);
        if (lessonCursor == null || lessonCursor.getCount() < 1) {
            return null;
        }
        int idxName = lessonCursor.getColumnIndex(SelmaSQLiteHelper2.TABLE_LESSONS_LESSONNAME);
        int idxCourse = lessonCursor.getColumnIndex(SelmaSQLiteHelper2.TABLE_LESSONS_COURSENAME);
        boolean found = false;
        lessonCursor.moveToFirst();
        do {
            if (lessonCursor.getString(idxName).equals(this.mLessonName)
                    && lessonCursor.getString(idxCourse).equals(this.mCourseName)) {
                found = true;
                break;
            }
        } while (lessonCursor.moveToNext());
        if (found) {
            int idxId = lessonCursor.getColumnIndex(SelmaSQLiteHelper2.TABLE_LESSONS_ID);
            if (lessonCursor.moveToNext()) {
                rv = new Lesson(lessonCursor.getLong(idxId), context);
            } else {
                if(pm == LessonPlayer.PlayMode.REPEAT_ALL_LESSONS) {
                    lessonCursor.moveToFirst();
                    rv = new Lesson(lessonCursor.getLong(idxId), context);
                }
            }
        }
        lessonCursor.close();
        return rv;
    }

    @Override
    protected void finalize() throws Throwable {
        mCursor.close();
        super.finalize();
    }

    public String getLessonName() {
        return mLessonName;
    }
}
