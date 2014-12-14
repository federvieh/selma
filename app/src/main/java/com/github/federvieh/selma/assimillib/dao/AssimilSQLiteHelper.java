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
public class AssimilSQLiteHelper extends SelmaSQLiteHelper {
	/* Prefixes as used in the Assimil lesson MP3 files.
	 * 
	 */
	public static final String TITLE_PREFIX = "S00-TITLE-";
	private static final int PREFIX_LENGTH = "S01-".length();

	/* This stores the file list of all directories that contain mp3 files in order
	 * to speed up searching for translations
	 */
	private static HashMap<String, File[]> files = new HashMap<String, File[]>();

	private Context caller;

	public AssimilSQLiteHelper(Context context){
		super(context, null);
		this.caller = context;
	}

	/* (non-Javadoc)
	 * @see com.github.federvieh.selma.assimillib.dao.SelmaSQLiteHelper#createIfNotExists(java.lang.String, java.lang.String, java.lang.String, android.content.Context, android.content.SharedPreferences)
	 */
	@Override
	public void createIfNotExists(String number, String language,
			String fullAlbum, SharedPreferences settings) {
        ContentResolver contentResolver = caller.getContentResolver();
        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = { android.provider.MediaStore.Audio.Media.TITLE,
        		android.provider.MediaStore.Audio.Media.ALBUM,
        		android.provider.MediaStore.Audio.Media._ID,
        		android.provider.MediaStore.Audio.Media.DATA
        };
        String findLessonTexts = android.provider.MediaStore.Audio.Media.ALBUM+" = '"+fullAlbum+"' AND ("+
        		android.provider.MediaStore.Audio.Media.TITLE+" LIKE 'N%-%' OR "+ //NUMBER
        		android.provider.MediaStore.Audio.Media.TITLE+" LIKE 'S%' OR "+   //Text
        		android.provider.MediaStore.Audio.Media.TITLE+" LIKE 'T%')";      //Translate
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
//			AssimilSQLiteHelper dbHelper = new AssimilSQLiteHelper(caller);
			SQLiteDatabase db = this.getWritableDatabase();
			//Find the lesson in the lesson table
			String[] columns = {AssimilSQLiteHelper.TABLE_LESSONS_ID};
			Cursor cursorAlbum = db.query(AssimilSQLiteHelper.TABLE_LESSONS, columns,
					AssimilSQLiteHelper.TABLE_LESSONS_LESSONNAME + "= '" + number + "'" +
							" AND " + AssimilSQLiteHelper.TABLE_LESSONS_COURSENAME + "= '" + language + "'",
							null, null, null, null);
			if(!cursorAlbum.moveToFirst()){
				//No result, i.e. we need to create a new entry for this album
				Log.d("LT", "Creating new lesson for " + fullAlbum);
				ContentValues valuesLessonTable = new ContentValues();
				valuesLessonTable.put(AssimilSQLiteHelper.TABLE_LESSONS_COURSENAME, language);
				valuesLessonTable.put(AssimilSQLiteHelper.TABLE_LESSONS_LESSONNAME, number);
				valuesLessonTable.put(AssimilSQLiteHelper.TABLE_LESSONS_STARRED, 0);
				db.insert(AssimilSQLiteHelper.TABLE_LESSONS, null, valuesLessonTable);
				cursorAlbum = db.query(AssimilSQLiteHelper.TABLE_LESSONS, columns,
						AssimilSQLiteHelper.TABLE_LESSONS_LESSONNAME + "= '" + number + "'" +
								" AND " + AssimilSQLiteHelper.TABLE_LESSONS_COURSENAME + "= '" + language + "'",
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
			long albumId = cursorAlbum.getLong(cursorAlbum.getColumnIndex(AssimilSQLiteHelper.TABLE_LESSONS_ID));

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
        		if(fullTitle.startsWith(TITLE_PREFIX)){
        			text = fullTitle.substring(TITLE_PREFIX.length());
        			textNumber = fullTitle.substring(0, PREFIX_LENGTH-1);
        			textType = TextType.HEADING;
        		}
        		else if(fullTitle.matches("S[0-9][0-9]-.*")){
        			text = fullTitle.substring(PREFIX_LENGTH);
        			textNumber = fullTitle.substring(0, PREFIX_LENGTH-1);
        			textType = TextType.NORMAL;
        		}
        		else if(fullTitle.matches("T[0-9][0-9]-.*")){
        			text = fullTitle.substring(PREFIX_LENGTH);
        			textNumber = fullTitle.substring(0, PREFIX_LENGTH-1);
        			if(textNumber.equals("T00")){
        				textType = TextType.TRANSLATE_HEADING;
        			}
        			else{
        				textType = TextType.TRANSLATE;
        			}
        		}
        		else if(fullTitle.matches("N[0-9]*-.*")){
        			text = fullTitle.substring(fullTitle.indexOf("-")+1);
        			textNumber = fullTitle.substring(0, fullTitle.indexOf("-"));
        			textType = TextType.LESSONNUMBER;
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
    				Log.d("LT", "Text " + textNumber + " for lesson \"" + fullAlbum + "\" already exists. Skipping...");
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
    					values.put(AssimilSQLiteHelper.TABLE_LESSONTEXTS_TEXTTRANS, translations[0]);
    				}
    				if(translations[1] != null){
    					values.put(AssimilSQLiteHelper.TABLE_LESSONTEXTS_TEXTLIT, translations[1]);
    				}
    				values.put(AssimilSQLiteHelper.TABLE_LESSONTEXTS_AUDIOFILEPATH, path);
    				db.insert(AssimilSQLiteHelper.TABLE_LESSONTEXTS, null, values);
    			}
        	} while (cursor.moveToNext());
        	cursor.close();
        	db.close();
        }
        files.clear();
	}
}
