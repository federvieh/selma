/**
 * 
 */
package com.github.federvieh.selma.assimillib.dao;

import java.util.ArrayList;
import java.util.List;

import com.github.federvieh.selma.assimillib.AssimilLesson;
import com.github.federvieh.selma.assimillib.AssimilLessonHeader;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/**
 * @author frank
 *
 */
public class AssimilLessonDataSource {
	private AssimilSQLiteHelper dbHelper;
	private SQLiteDatabase database;

	public AssimilLessonDataSource(Context ctxt){
		dbHelper = new AssimilSQLiteHelper(ctxt);
	}

	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}

	public AssimilLesson getLesson(AssimilLessonHeader header) {
		
		AssimilLesson rv = new AssimilLesson(header);

		String[] returnColumns = {
				AssimilSQLiteHelper.TABLE_LESSONTEXTS_TEXTID,
				AssimilSQLiteHelper.TABLE_LESSONTEXTS_TEXT,
//				AssimilSQLiteHelper.TABLE_LESSONTEXTS_LANG,
				AssimilSQLiteHelper.TABLE_LESSONTEXTS_TEXTTRANS,
				AssimilSQLiteHelper.TABLE_LESSONTEXTS_TEXTLIT,
				AssimilSQLiteHelper.TABLE_LESSONTEXTS_ID,
				AssimilSQLiteHelper.TABLE_LESSONTEXTS_AUDIOFILEPATH,
				};
		String selection = AssimilSQLiteHelper.TABLE_LESSONTEXTS_LESSONID + "=" +
				header.getId();
		String orderBy = AssimilSQLiteHelper.TABLE_LESSONTEXTS_TEXTID;
		Cursor cursor = database.query(AssimilSQLiteHelper.TABLE_LESSONTEXTS,
				returnColumns , selection, null, null, null, 
				orderBy);

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			addCursorToLesson(cursor, rv);
			cursor.moveToNext();
		}
		cursor.close();
		return rv;
	}

	private static void addCursorToLesson(Cursor cursor, AssimilLesson lesson) {
		String textId = cursor.getString(0);
		String text = cursor.getString(1);
//		int lang = cursor.getInt(2);
		String texttrans = cursor.getString(2);
		String textlit = cursor.getString(3);
		int id = cursor.getInt(4);
		String audioPath = cursor.getString(5);
		lesson.addText(textId, text, texttrans, textlit, id, audioPath);
	}
}
