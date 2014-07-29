/**
 * 
 */
package com.github.federvieh.selma.assimillib.dao;

import java.util.ArrayList;
import java.util.List;

import com.github.federvieh.selma.assimillib.AssimilLessonHeader;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/**
 * @author frank
 *
 */
public class AssimilLessonHeaderDataSource {
	private AssimilSQLiteHelper dbHelper;
	private SQLiteDatabase database;

	public AssimilLessonHeaderDataSource(Context ctxt){
		dbHelper = new AssimilSQLiteHelper(ctxt);
	}

	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}

	public List<AssimilLessonHeader> getLessonHeaders(String coursename) {
		List<AssimilLessonHeader> headers = new ArrayList<AssimilLessonHeader>();

		String[] returnColumns = {AssimilSQLiteHelper.TABLE_LESSONS_LESSONNAME,
				AssimilSQLiteHelper.TABLE_LESSONS_ID,
				AssimilSQLiteHelper.TABLE_LESSONS_STARRED};
		String selection = null;
		if (coursename!=null){
			selection = AssimilSQLiteHelper.TABLE_LESSONS_COURSENAME + "=" +
					coursename;
		}
		Cursor cursor = database.query(AssimilSQLiteHelper.TABLE_LESSONS,
				returnColumns , selection, null, null, null, 
				AssimilSQLiteHelper.TABLE_LESSONS_LESSONNAME);

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			AssimilLessonHeader header = cursorToHeader(cursor);
			headers.add(header);
			cursor.moveToNext();
		}
		cursor.close();
		return headers;
	}

	private static AssimilLessonHeader cursorToHeader(Cursor cursor) {
		AssimilLessonHeader header = new AssimilLessonHeader();
		header.setId(cursor.getLong(1));
		header.setName(cursor.getString(0));
		if(cursor.getLong(2)>0){
			header.star();
		}
		else{
			//default is unstarred: Nothing to do
		}
		return header;
	}
}
