/**
 * 
 */
package com.github.federvieh.selma.assimillib.dao;

import java.io.File;
import java.util.HashMap;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

/**
 * @author frank
 * 
 *
 */
public class AssimilPcSQLiteHelper extends SelmaSQLiteHelper {
	
	private static final int PREFIX_LENGTH = "l000_".length();
	
	/* This stores the file list of all directories that contain mp3 files in order
	 * to speed up searching for translations
	 */
	private static HashMap<String, File[]> files = new HashMap<String, File[]>();

	private Context caller;

	public AssimilPcSQLiteHelper(Context context){
		super(context, null);
		this.caller = context;
	}

	/* (non-Javadoc)
	 * @see com.github.federvieh.selma.assimillib.dao.SelmaSQLiteHelper#createIfNotExists(java.lang.String, java.lang.String, java.lang.String, android.content.Context, android.content.SharedPreferences)
	 */
	@Override
	public void createIfNotExists(String number, String language,
			String fullAlbumNotUsedInAssimilPc, SharedPreferences settings) {
		// TODO Auto-generated method stub
        ContentResolver contentResolver = caller.getContentResolver();
        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = { android.provider.MediaStore.Audio.Media.TITLE,
        		android.provider.MediaStore.Audio.Media.ALBUM,
        		android.provider.MediaStore.Audio.Media._ID,
        		android.provider.MediaStore.Audio.Media.DATA
        };
        String findLessonTexts =
        		android.provider.MediaStore.Audio.Media.TITLE+" REGEXP 'l"+number+"_[0-9][0-9a]' OR "+ //e.g. l001_01
        		android.provider.MediaStore.Audio.Media.TITLE+" REGEXP 'e"+number+"_[0-9][0-9]'";      //e.g. e001_01
        Cursor cursor = contentResolver.query(uri, projection, findLessonTexts, null, android.provider.MediaStore.Audio.Media.TITLE);
        if(cursor == null){
        	//TODO: query failed
        	return;
        }
        else if (!cursor.moveToFirst()){
        	// TODO: no media on device
        	return;
        }
        else{
			AssimilPcSQLiteHelper dbHelper = this;
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			//Find the lesson in the lesson table
			String[] columns = {AssimilPcSQLiteHelper.TABLE_LESSONS_ID};
			Cursor cursorAlbum = db.query(AssimilPcSQLiteHelper.TABLE_LESSONS, columns,
					AssimilPcSQLiteHelper.TABLE_LESSONS_LESSONNAME + "= 'L" + number + "'" +
							" AND " + AssimilPcSQLiteHelper.TABLE_LESSONS_COURSENAME + "= '" + language + "'",
							null, null, null, null);
			if(!cursorAlbum.moveToFirst()){
				//No result, i.e. we need to create a new entry for this album
				Log.d("LT", "Creating new lesson for " + number);
				ContentValues valuesLessonTable = new ContentValues();
				valuesLessonTable.put(AssimilPcSQLiteHelper.TABLE_LESSONS_COURSENAME, language);
				valuesLessonTable.put(AssimilPcSQLiteHelper.TABLE_LESSONS_LESSONNAME, "L"+number);
				valuesLessonTable.put(AssimilPcSQLiteHelper.TABLE_LESSONS_STARRED, 0);
				db.insert(AssimilPcSQLiteHelper.TABLE_LESSONS, null, valuesLessonTable);
				cursorAlbum = db.query(AssimilPcSQLiteHelper.TABLE_LESSONS, columns,
						AssimilPcSQLiteHelper.TABLE_LESSONS_LESSONNAME + "= 'L" + number + "'" +
								" AND " + AssimilPcSQLiteHelper.TABLE_LESSONS_COURSENAME + "= '" + language + "'",
								null, null, null, null);
			}
			if(!cursorAlbum.moveToFirst()){
				//Still no result!
				Log.wtf("LT", "Creating new lesson header was unsuccessful!");
				return;
			}
			if(cursorAlbum.getCount()!=1){
				//Hmm... also not expected
				Log.wtf("LT", "Query for lesson header returned " + cursorAlbum.getCount() + ", but expected is 1!");
				return;
			}
			long albumId = cursorAlbum.getLong(cursorAlbum.getColumnIndex(AssimilPcSQLiteHelper.TABLE_LESSONS_ID));

        	int titleColumn = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE);
        	int idColumn = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media._ID);
        	int dataColumn = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.DATA);
        	//title = S00-TITLE-İki genç
        	//album = ASSIMIL Turkish With Ease - L001
        	do{
        		String fullTitle = cursor.getString(titleColumn);
        		String id = cursor.getString(idColumn);
        		String path = cursor.getString(dataColumn);
//      		Log.i("LT", "Path: "+path);
        		String text = null;
        		String textNumber = null;
        		TextType textType;
        		if(fullTitle.equals("l"+number+"_0a")){
        			text = fullTitle;
        			textNumber = "N"+fullTitle.substring(PREFIX_LENGTH);
        			textType = TextType.LESSONNUMBER;
        		}
        		else if(fullTitle.matches("l"+number+"_[0-9][0-9]")){
        			text = fullTitle;//.substring(PREFIX_LENGTH);
        			textNumber = "S"+fullTitle.substring(PREFIX_LENGTH);
        			if(fullTitle.equals("l"+number+"_00")){
        				textType = TextType.HEADING;
        			}
        			else{
        				textType = TextType.NORMAL;
        			}
        		}
        		else if(fullTitle.matches("e"+number+"_[0-9][0-9]")){
        			text = fullTitle;//.substring(PREFIX_LENGTH);
        			textNumber = "T"+fullTitle.substring(PREFIX_LENGTH);
        			textType = TextType.TRANSLATE;
        		}
        		else{
        			//Something's wrong!
            		Log.w("LT", "Unknown file!");
            		Log.w("LT", "text = '"+fullTitle+"'");
            		Log.w("LT", "id =   '"+id+"'");
            		Log.w("LT", "==============================================");
            		continue;
        		}
    			//Find the lesson in the lesson table
    			String[] columnsTexts = {TABLE_LESSONTEXTS_TEXTID};
    			Cursor cursorLessontext = db.query(TABLE_LESSONTEXTS, columnsTexts,
    					TABLE_LESSONTEXTS_TEXTID + " = '" + textNumber + "' AND "
    							+ TABLE_LESSONTEXTS_LESSONID + " = " + albumId, null, null, null, null);
    			if(cursorLessontext.moveToFirst()){
    				//No result, i.e. we don't need to create a new entry for this text
    				Log.d("LT", "Text " + textNumber + " for lesson \"" + number + "\" already exists. Skipping...");
    			}
    			else{
    				String[] translations = findTexts(path);
    				ContentValues values = new ContentValues();
    				values.put(TABLE_LESSONTEXTS_LESSONID,  albumId);
    				values.put(TABLE_LESSONTEXTS_TEXTID,    textNumber);
    				values.put(TABLE_LESSONTEXTS_TEXTTYPE,  textType.ordinal());
    				if(translations[2] != null){
    					text = translations[2];
    				}
    				values.put(TABLE_LESSONTEXTS_TEXT,      text);
    				if(translations[0] != null){
    					values.put(AssimilPcSQLiteHelper.TABLE_LESSONTEXTS_TEXTTRANS, translations[0]);
    				}
    				if(translations[1] != null){
    					values.put(AssimilPcSQLiteHelper.TABLE_LESSONTEXTS_TEXTLIT, translations[1]);
    				}
    				values.put(AssimilPcSQLiteHelper.TABLE_LESSONTEXTS_AUDIOFILEPATH, path);
    				db.insert(AssimilPcSQLiteHelper.TABLE_LESSONTEXTS, null, values);
    			}
        	} while (cursor.moveToNext());
        	cursor.close();
        	db.close();
        }
        files.clear();
	}
}
