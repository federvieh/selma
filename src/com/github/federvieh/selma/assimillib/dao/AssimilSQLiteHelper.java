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
import android.app.Activity;
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
 */
public class AssimilSQLiteHelper extends SQLiteOpenHelper {
	
	private static final int ASSIMIL_DATABASE_VERSION = 1;
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
	 * 
	 * Old idea: TODO: Remove comment!
	 * |auto | ref to _id of lessons | S01    | 1    | Hello Mehmet   | /path/to/S01.mp3 |
	 * |auto | ref to _id of lessons | S02    | 1    | How are you?   | /path/to/S02.mp3 |
	 */
	public static final String TABLE_LESSONTEXTS = "lessontexts";
	public static final String TABLE_LESSONTEXTS_ID = "_id";
	public static final String TABLE_LESSONTEXTS_LESSONID = "lessonid";
	public static final String TABLE_LESSONTEXTS_TEXTID = "textid";
//TODO: Remove! Old idea	public static final String TABLE_LESSONTEXTS_LANG = "language";
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
	}

	/* (non-Javadoc)
	 * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Right now not really needed (only one version exists). For now
		// just drop the tables and re-create.
		// Improve when needed.
		Log.w(this.getClass().getName(), "Dropping database content in order" +
				" to upgrade from version " + oldVersion + " to " + newVersion);
		db.execSQL(ASSIMIL_DROP_TABLE_LESSONTEXTS);
		db.execSQL(ASSIMIL_DROP_TABLE_LESSONS);
		onCreate(db);
	}
	/**
	 * @param number
	 * @param language
	 * @param fullAlbum
	 * @param caller
	 * @param settings
	 */
	public static void createIfNotExists(String number, String language,
			String fullAlbum, Activity caller, SharedPreferences settings) {
		// TODO Auto-generated method stub
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
			//TODO: Should the following be moved to a DAO class?
			AssimilSQLiteHelper dbHelper = new AssimilSQLiteHelper(caller);
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			//Find the lesson in the lesson table
			String[] columns = {AssimilSQLiteHelper.TABLE_LESSONS_ID};
			Cursor cursorAlbum = db.query(AssimilSQLiteHelper.TABLE_LESSONS, columns,
					AssimilSQLiteHelper.TABLE_LESSONS_LESSONNAME + "= '" + fullAlbum + "'", null, null, null, null);
			if(!cursorAlbum.moveToFirst()){
				//No result, i.e. we need to create a new entry for this album
				ContentValues valuesLessonTable = new ContentValues();
				valuesLessonTable.put(AssimilSQLiteHelper.TABLE_LESSONS_COURSENAME, language);
				valuesLessonTable.put(AssimilSQLiteHelper.TABLE_LESSONS_LESSONNAME, fullAlbum);
				valuesLessonTable.put(AssimilSQLiteHelper.TABLE_LESSONS_STARRED, 0);
				db.insert(AssimilSQLiteHelper.TABLE_LESSONS, null, valuesLessonTable);
				cursorAlbum = db.query(AssimilSQLiteHelper.TABLE_LESSONS, columns,
						AssimilSQLiteHelper.TABLE_LESSONS_LESSONNAME + "= '" + fullAlbum + "'", null, null, null, null);
			}
			if(!cursorAlbum.moveToFirst()){
				//Still no result!
				//FIXME: What now!?
				return;
			}
			if(cursorAlbum.getCount()!=1){
				//Hmm... also not expected
				//FIXME: What now!?
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
//        		Log.i("LT", "Path: "+path);
        		String text = null;
        		String textNumber = null;
        		if(fullTitle.startsWith(TITLE_PREFIX)){
        			//TODO: Should the following be moved to a DAO class?
        			//TODO: Check if the entry already exists
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
           		String[] translations = findTranslations(path);
    			ContentValues values = new ContentValues();
    			values.put(AssimilSQLiteHelper.TABLE_LESSONTEXTS_LESSONID,  albumId);
    			values.put(AssimilSQLiteHelper.TABLE_LESSONTEXTS_TEXTID,    textNumber);
//TODO: Remove, Old idea:    			values.put(AssimilSQLiteHelper.TABLE_LESSONTEXTS_LANG,      0);
    			values.put(AssimilSQLiteHelper.TABLE_LESSONTEXTS_TEXT,      text);
    			if(translations[0] != null){
    				values.put(AssimilSQLiteHelper.TABLE_LESSONTEXTS_TEXTTRANS, translations[0]);
    			}
    			if(translations[1] != null){
    				values.put(AssimilSQLiteHelper.TABLE_LESSONTEXTS_TEXTLIT, translations[1]);
    			}
    			values.put(AssimilSQLiteHelper.TABLE_LESSONTEXTS_AUDIOFILEPATH, path);
    			db.insert(AssimilSQLiteHelper.TABLE_LESSONTEXTS, null, values);
    			
                /* TODO: Old idea, remove! */
//        		String[] translations = findTranslations(path);
//
//        		values = new ContentValues();
//        		values.put(AssimilSQLiteHelper.TABLE_LESSONTEXTS_LESSONID,  albumId);
//    			values.put(AssimilSQLiteHelper.TABLE_LESSONTEXTS_TEXTID,    textNumber);
//    			values.put(AssimilSQLiteHelper.TABLE_LESSONTEXTS_LANG,      1);
//    			if(translations[0] != null){
//    				values.put(AssimilSQLiteHelper.TABLE_LESSONTEXTS_TEXT,      translations[0]);
//    			}
//    			values.put(AssimilSQLiteHelper.TABLE_LESSONTEXTS_AUDIOFILEPATH, path);
//    			db.insert(AssimilSQLiteHelper.TABLE_LESSONTEXTS, null, values);
//
//        		values = new ContentValues();
//        		values.put(AssimilSQLiteHelper.TABLE_LESSONTEXTS_LESSONID,  albumId);
//    			values.put(AssimilSQLiteHelper.TABLE_LESSONTEXTS_TEXTID,    textNumber);
//    			values.put(AssimilSQLiteHelper.TABLE_LESSONTEXTS_LANG,      2);
//    			if(translations[1] != null){
//    				values.put(AssimilSQLiteHelper.TABLE_LESSONTEXTS_TEXT,      translations[1]);
//    			}
//    			values.put(AssimilSQLiteHelper.TABLE_LESSONTEXTS_AUDIOFILEPATH, path);
//    			db.insert(AssimilSQLiteHelper.TABLE_LESSONTEXTS, null, values);
        	} while (cursor.moveToNext());
        	cursor.close();
        }
        files.clear();
	}
	/** Find translation on SD card of the given MP3 file.
	 * @param pathStr path to the MP3 file
	 * @return rv[0] contains translation, rv[1] contains literal translation,
	 *         any of which can be null if no translation was found.
	 */
	private static String[] findTranslations(String pathStr) {
		StringBuffer fileNamePatt = new StringBuffer(pathStr);
		fileNamePatt.delete(fileNamePatt.length()-4, fileNamePatt.length());
		fileNamePatt.delete(0, fileNamePatt.lastIndexOf("/")+1);
		
		StringBuffer directory = new StringBuffer(pathStr);
		directory.delete(directory.lastIndexOf("/")+1,directory.length());
		
		Log.d("LT", "directory: "+directory.toString());
		Log.d("LT", "fileNamePatt: "+fileNamePatt.toString());
		
		String translatedText = getFileContent(directory.toString(), fileNamePatt+"_translate.txt");
		String translatedTextVerbatim = getFileContent(directory.toString(), fileNamePatt+"_translate_verbatim.txt");
		
		//FIXME: Handle null values in caller! activity.getResources().getText(R.string.not_yet_translated)
		String[] rv = {translatedText, translatedTextVerbatim};
		return rv;
//		Log.d("LT", "_translate: "+translatedText);
//		Log.d("LT", "_translate_verbatim: "+translatedTextVerbatim);
//		
//		allPaths.add(directory.toString());
//		allTranslationFilenames.add(fileNamePatt+"_translate.txt");
//		allLiteralFilenames.add(fileNamePatt+"_translate_verbatim.txt");
//
//		if(translatedText!=null){
//			allTextsTranslate.add(translatedText);			
//		}
//		else{
//			allTextsTranslate.add(activity.getResources().getText(R.string.not_yet_translated).toString());			
//		}
//		if(translatedTextVerbatim!=null){
//			allTextsTranslateSimple.add(translatedTextVerbatim);			
//		}
//		else{
//			allTextsTranslateSimple.add(activity.getResources().getText(R.string.not_yet_translated).toString());			
//		}
	}

	/**
	 * @param d
	 * @param filename
	 * @return
	 */
	private static String getFileContent(String directory, String filename) {
		//FIXME: Why don't we directly access the files here? Why scan the dir?
		File d = new File(directory);
		if(d.exists()&&d.isDirectory()){
			File[] dirList = files.get(directory);
			if(dirList==null){
				dirList = d.listFiles();
				files.put(directory, dirList);
			}
			for(File f : dirList){
				if(f.getName().equalsIgnoreCase(filename)){
					InputStream is;
					try {
						is = new FileInputStream(f);
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
						Log.w("LT", e1);
					}
					catch (IOException e) {
						Log.w("LT", e);
					}
				}
			}
		}
		else{
			Log.w("LT", "\"" + d.toString() +"\" is not a valid directory.");
		}
		return null;
	}
}
