/**
 * 
 */
package com.github.federvieh.selma.assimillib.dao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

/**
 * @author frank
 * 
 *
 */
public class AssimilSQLiteHelper extends SQLiteOpenHelper {
	
	private static final int ASSIMIL_DATABASE_VERSION = 2;
	private static final String ASSIMIL_DATABASE_NAME = "assimil.db";
	
	/* Table "lessons"
	 * | _id | coursename        | lessonname | starred |
	 * +-----+-------------------+------------+---------+
	 * |auto | Turkish with Ease | L001       | 1       |
	 * |auto | Turkish with Ease | L002       | 1       |
	 * |auto | Spanish with Ease | L001       | 0       |
	 */
	public static final String TABLE_LESSONS = "lessons";
	public static final String TABLE_LESSONS_ID = "_id";
	public static final String TABLE_LESSONS_COURSENAME = "coursename";
	public static final String TABLE_LESSONS_LESSONNAME = "lessonname";
	public static final String TABLE_LESSONS_STARRED = "starred";
	private static final String ASSIMIL_CREATE_TABLE_LESSONS = 
			"create table " + TABLE_LESSONS + " (" +
			TABLE_LESSONS_ID + " integer primary key autoincrement, " +
			TABLE_LESSONS_COURSENAME + " text not null, " +
			TABLE_LESSONS_LESSONNAME + " int not null, " +
			TABLE_LESSONS_STARRED + " int not null" +
			");";
	private static final String ASSIMIL_DROP_TABLE_LESSONS = 
			"drop TABLE if exists " + TABLE_LESSONS + ";";

	/* Table "lessontexts"
	 * | _id | lessonid              | textid | text           | text_trans   | text_lit     | audiofile        |
	 * +-----+-----------------------+--------+----------------+--------------+--------------+------------------+
	 * |auto | ref to _id of lessons | S01    | Merhaba Mehmet | Hello Mehmet | Hello Mehmet | /path/to/S01.mp3 |
	 * |auto | ref to _id of lessons | S02    | Nasilsin?      | How are you? | How-you-are  | /path/to/S02.mp3 |
	 */
	public static final String TABLE_LESSONTEXTS = "lessontexts";
	public static final String TABLE_LESSONTEXTS_ID = "_id";
	public static final String TABLE_LESSONTEXTS_LESSONID = "lessonid";
	public static final String TABLE_LESSONTEXTS_TEXTID = "textid";
	public static final String TABLE_LESSONTEXTS_TEXT = "text";
	public static final String TABLE_LESSONTEXTS_TEXTTRANS = "text_trans";
	public static final String TABLE_LESSONTEXTS_TEXTLIT = "text_lit";
	public static final String TABLE_LESSONTEXTS_AUDIOFILEPATH = "audiofile";
	private static final String ASSIMIL_CREATE_TABLE_LESSONTEXTS = 
			"create table " + TABLE_LESSONTEXTS + " (" +
			TABLE_LESSONTEXTS_ID + " integer primary key autoincrement, " +
			TABLE_LESSONTEXTS_LESSONID + " integer not null, " + //TODO: reference table "lessons"
			TABLE_LESSONTEXTS_TEXTID + " text not null," +
//			TABLE_LESSONTEXTS_LANG + " integer not null," + //0: orig, 1: translate
			TABLE_LESSONTEXTS_TEXT + " text not null," + //The actual text
			TABLE_LESSONTEXTS_TEXTTRANS + " text," + //The translated text
			TABLE_LESSONTEXTS_TEXTLIT + " text," + //The literal translation of the text
			TABLE_LESSONTEXTS_AUDIOFILEPATH + " text not null" + //The actual text
			");";
	private static final String ASSIMIL_DROP_TABLE_LESSONTEXTS = 
			"drop TABLE if exists " + TABLE_LESSONTEXTS + ";";
	private static final String ASSIMIL_INDEX_LESSONID_LESSONTEXTS = 
			"create index idx_lessonid on " + TABLE_LESSONTEXTS + "(" + TABLE_LESSONTEXTS_LESSONID + ")";
			
	/* Prefixes as used in the Assimil lesson MP3 files.
	 * 
	 */
	public static final String TITLE_PREFIX = "S00-TITLE-";
	private static final int PREFIX_LENGTH = "S01-".length();

	/* This stores the file list of all directories that contain mp3 files in order
	 * to speed up searching for translations
	 */
	private static HashMap<String, File[]> files = new HashMap<String, File[]>();

	public AssimilSQLiteHelper(Context context){
		super(context,ASSIMIL_DATABASE_NAME, null, ASSIMIL_DATABASE_VERSION);
	}

	/* (non-Javadoc)
	 * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(ASSIMIL_CREATE_TABLE_LESSONS);
		db.execSQL(ASSIMIL_CREATE_TABLE_LESSONTEXTS);
		db.execSQL(ASSIMIL_INDEX_LESSONID_LESSONTEXTS);
	}

	/* (non-Javadoc)
	 * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if((oldVersion == 1)&&(newVersion == 2)){
			//Change from Version 1 to 2: Added index
			db.execSQL(ASSIMIL_INDEX_LESSONID_LESSONTEXTS);
		}
		else{
			Log.w(this.getClass().getName(), "Unknown version upgrade. Dropping database content in order" +
					" to upgrade from version " + oldVersion + " to " + newVersion);
			db.execSQL(ASSIMIL_DROP_TABLE_LESSONTEXTS);
			db.execSQL(ASSIMIL_DROP_TABLE_LESSONS);
			onCreate(db);
		}
	}
	/**
	 * @param number
	 * @param language
	 * @param fullAlbum
	 * @param caller
	 * @param settings
	 */
	public static void createIfNotExists(String number, String language,
			String fullAlbum, Context caller, SharedPreferences settings) {
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
			AssimilSQLiteHelper dbHelper = new AssimilSQLiteHelper(caller);
			SQLiteDatabase db = dbHelper.getWritableDatabase();
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
        		if(fullTitle.startsWith(TITLE_PREFIX)){
        			text = fullTitle.substring(TITLE_PREFIX.length());
        			textNumber = fullTitle.substring(0, PREFIX_LENGTH-1);
        		}
        		else if(fullTitle.matches("S[0-9][0-9]-.*")){
        			text = fullTitle.substring(PREFIX_LENGTH);
        			textNumber = fullTitle.substring(0, PREFIX_LENGTH-1);
        		}
        		else if(fullTitle.matches("T[0-9][0-9]-.*")){
        			text = fullTitle.substring(PREFIX_LENGTH);
        			textNumber = fullTitle.substring(0, PREFIX_LENGTH-1);
        		}
        		else if(fullTitle.matches("N[0-9]*-.*")){
        			text = fullTitle.substring(fullTitle.indexOf("-")+1);
        			textNumber = fullTitle.substring(0, PREFIX_LENGTH-1);
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
	/** Find translation on SD card of the given MP3 file.
	 * @param pathStr path to the MP3 file
	 * @return rv[0] contains translation, rv[1] contains literal translation,
	 *         rv[2] contains manually corrected original,
	 *         any of which can be null if no text was found on SD card.
	 */
	private static String[] findTexts(String pathStr) {
		StringBuffer fileNamePatt = new StringBuffer(pathStr);
		fileNamePatt.delete(fileNamePatt.length()-4, fileNamePatt.length());
//		fileNamePatt.delete(0, fileNamePatt.lastIndexOf("/")+1);
		
//		StringBuffer directory = new StringBuffer(pathStr);
//		directory.delete(directory.lastIndexOf("/")+1,directory.length());
		
//		Log.d("LT", "directory: "+directory.toString());
		Log.d("LT", "fileNamePatt: "+fileNamePatt.toString());
		
		String translatedText = getFileContent(fileNamePatt+"_translate.txt");
		String translatedTextVerbatim = getFileContent(fileNamePatt+"_translate_verbatim.txt");
		String originalText = getFileContent(fileNamePatt+"_orig.txt");
		
		String[] rv = {translatedText, translatedTextVerbatim, originalText};
		return rv;
	}

	/**
	 * @param d
	 * @param filename
	 * @return
	 */
	private static String getFileContent(/*String directory, String filename,*/ String path) {
		InputStream is;
		try {
			is = new FileInputStream(path);
			InputStreamReader isr = new InputStreamReader(is,"UTF-16");
			BufferedReader br = new BufferedReader(isr);
			String nextLine;
			StringBuilder sb = new StringBuilder();
			while((nextLine=br.readLine())!=null){
				sb.append(nextLine);
			}
			br.close();
			isr.close();
			is.close();
			if(sb.length()>0){
				return sb.toString();
			}
		} catch (FileNotFoundException e1) {
			/* Silently fail, no translation was manually entered for this text */
			//Log.w("LT", e1);
		}
		catch (IOException e) {
			Log.w("LT", e);
		}
		return null;
	}
}
