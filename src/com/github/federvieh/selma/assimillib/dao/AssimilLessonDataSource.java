/**
 * 
 */
package com.github.federvieh.selma.assimillib.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.github.federvieh.selma.R;
import com.github.federvieh.selma.assimillib.AssimilLesson;
import com.github.federvieh.selma.assimillib.AssimilLessonHeader;
import com.github.federvieh.selma.assimillib.dao.SelmaSQLiteHelper.TextType;

/**
 * @author frank
 *
 */
public class AssimilLessonDataSource {
	private SelmaSQLiteHelper dbHelper;
	private SQLiteDatabase database;
	private Context ctxt;

	public AssimilLessonDataSource(SelmaSQLiteHelper helper){
		dbHelper = helper;
		this.ctxt = helper.getContext();
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
				SelmaSQLiteHelper.TABLE_LESSONTEXTS_TEXTID,
				SelmaSQLiteHelper.TABLE_LESSONTEXTS_TEXT,
				SelmaSQLiteHelper.TABLE_LESSONTEXTS_TEXTTRANS,
				SelmaSQLiteHelper.TABLE_LESSONTEXTS_TEXTLIT,
				SelmaSQLiteHelper.TABLE_LESSONTEXTS_ID,
				SelmaSQLiteHelper.TABLE_LESSONTEXTS_AUDIOFILEPATH,
				SelmaSQLiteHelper.TABLE_LESSONTEXTS_TEXTTYPE,
				};
		String selection = SelmaSQLiteHelper.TABLE_LESSONTEXTS_LESSONID + "=" +
				header.getId();
		String orderBy = SelmaSQLiteHelper.TABLE_LESSONTEXTS_TEXTID;
		Cursor cursor = database.query(SelmaSQLiteHelper.TABLE_LESSONTEXTS,
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

	private void addCursorToLesson(Cursor cursor, AssimilLesson lesson) {
		String textId = cursor.getString(0);
		String text = cursor.getString(1);
		String texttrans = cursor.getString(2);
		if(texttrans==null){
			texttrans = ctxt.getResources().getText(R.string.not_yet_translated).toString();
		}
		String textlit = cursor.getString(3);
		if(textlit==null){
			textlit = ctxt.getResources().getText(R.string.not_yet_translated).toString();
		}
		int id = cursor.getInt(4);
		String audioPath = cursor.getString(5);
		int textType = cursor.getInt(6);
		lesson.addText(textId, text, texttrans, textlit, id, audioPath, TextType.values()[textType]);
	}

	/**
	 * @param id
	 * @param newTrans
	 */
	public void updateTranslation(int id, String newTrans) {
		String whereClause = SelmaSQLiteHelper.TABLE_LESSONTEXTS_ID + " = " + id;
		ContentValues values = new ContentValues();
		values.put(SelmaSQLiteHelper.TABLE_LESSONTEXTS_TEXTTRANS, newTrans);
		database.update(SelmaSQLiteHelper.TABLE_LESSONTEXTS, values , whereClause, null);
	}

	/**
	 * @param id
	 * @param newLit
	 */
	public void updateTranslationLit(Integer id, String newLit) {
		String whereClause = SelmaSQLiteHelper.TABLE_LESSONTEXTS_ID + " = " + id;
		ContentValues values = new ContentValues();
		values.put(SelmaSQLiteHelper.TABLE_LESSONTEXTS_TEXTLIT, newLit);
		database.update(SelmaSQLiteHelper.TABLE_LESSONTEXTS, values , whereClause, null);
	}

	/**
	 * @param id
	 * @param newText
	 */
	public void updateOriginalText(Integer id, String newText) {
		String whereClause = SelmaSQLiteHelper.TABLE_LESSONTEXTS_ID + " = " + id;
		ContentValues values = new ContentValues();
		values.put(SelmaSQLiteHelper.TABLE_LESSONTEXTS_TEXT, newText);
		database.update(SelmaSQLiteHelper.TABLE_LESSONTEXTS, values , whereClause, null);
	}
}
