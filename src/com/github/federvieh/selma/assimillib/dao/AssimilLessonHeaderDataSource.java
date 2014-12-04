/**
 * 
 */
package com.github.federvieh.selma.assimillib.dao;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.github.federvieh.selma.assimillib.AssimilLessonHeader;
import com.github.federvieh.selma.assimillib.AssimilDatabase.LessonType;

/**
 * @author frank
 *
 */
public class AssimilLessonHeaderDataSource {
	private SelmaSQLiteHelper dbHelper;
	private SQLiteDatabase database;
	private LessonType lessonType;

	public AssimilLessonHeaderDataSource(LessonType type, Context ctxt){
		this.lessonType = type;
		switch(type){
		case ASSIMIL_MP3_PACK:
			dbHelper = new AssimilSQLiteHelper(ctxt);
			break;
		case ASSIMIL_PC:
			dbHelper = new AssimilPcSQLiteHelper(ctxt);
			break;
		}
	}

	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}

	public List<AssimilLessonHeader> getLessonHeaders(String coursename) {
		List<AssimilLessonHeader> headers = new ArrayList<AssimilLessonHeader>();

		String[] returnColumns = {SelmaSQLiteHelper.TABLE_LESSONS_LESSONNAME,
				SelmaSQLiteHelper.TABLE_LESSONS_ID,
				SelmaSQLiteHelper.TABLE_LESSONS_STARRED,
				SelmaSQLiteHelper.TABLE_LESSONS_COURSENAME};
		String selection = null;
		if (coursename!=null){
			selection = SelmaSQLiteHelper.TABLE_LESSONS_COURSENAME + "=" +
					coursename;
		}
		Cursor cursor = database.query(SelmaSQLiteHelper.TABLE_LESSONS,
				returnColumns , selection, null, null, null, 
				SelmaSQLiteHelper.TABLE_LESSONS_LESSONNAME);

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			AssimilLessonHeader header = cursorToHeader(cursor);
			headers.add(header);
			cursor.moveToNext();
		}
		cursor.close();
		return headers;
	}

	private AssimilLessonHeader cursorToHeader(Cursor cursor) {
		AssimilLessonHeader header = new AssimilLessonHeader(cursor.getLong(1), cursor.getString(3), cursor.getString(0), cursor.getLong(2)>0, lessonType);
		return header;
	}

	/**
	 * @param id 
	 * 
	 */
	public void star(long id) {
		String whereClause = SelmaSQLiteHelper.TABLE_LESSONS_ID + " = " + id;
		ContentValues values = new ContentValues();
		values.put(SelmaSQLiteHelper.TABLE_LESSONS_STARRED, 1);
		database.update(SelmaSQLiteHelper.TABLE_LESSONS, values , whereClause, null);		
	}

	/**
	 * @param id
	 */
	public void unstar(long id) {
		String whereClause = SelmaSQLiteHelper.TABLE_LESSONS_ID + " = " + id;
		ContentValues values = new ContentValues();
		values.put(SelmaSQLiteHelper.TABLE_LESSONS_STARRED, 0);
		database.update(SelmaSQLiteHelper.TABLE_LESSONS, values , whereClause, null);		
	}
}
