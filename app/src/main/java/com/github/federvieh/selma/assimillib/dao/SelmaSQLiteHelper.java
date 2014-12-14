/**
 * 
 */
package com.github.federvieh.selma.assimillib.dao;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/** TODO: This design is not very clever. Using different SQLiteOpenHelper sub-classes for different course types raises the
 * impression that they are storing data in different database. This was the initial idea, but was given up due to the fact
 * that unique lesson ids are needed. But SQLite does not support sequences, so the approach is to store everything in the same
 * database. But now this is actually quite reasonable, because the only difference is actually when scanning for lessons, i.e.
 * when creating the database content. But now some refactoring is needed around the helper class (probably only have one helper
 * class and several "scanner" classes).
 * @author frank
 * 
 *
 */
abstract public class SelmaSQLiteHelper extends SQLiteOpenHelper {
	/**
	 * @author frank
	 *
	 */
	public enum TextType {
		NORMAL,
		HEADING,
		LESSONNUMBER,
		TRANSLATE,
		TRANSLATE_HEADING
	};

	private static final String ASSIMIL_DATABASE_NAME = "assimil.db";
	private static final int SELMA_DATABASE_VERSION = 3;
	
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
	 * | _id | lessonid              | textid | text           | text_trans   | text_lit     | audiofile        | texttype |
	 * +-----+-----------------------+--------+----------------+--------------+--------------+------------------+----------+
	 * |auto | ref to _id of lessons | S01    | Merhaba Mehmet | Hello Mehmet | Hello Mehmet | /path/to/S01.mp3 | 0        |
	 * |auto | ref to _id of lessons | S02    | Nasilsin?      | How are you? | How-you-are  | /path/to/S02.mp3 | 0        |
	 */
	public static final String TABLE_LESSONTEXTS = "lessontexts";
	public static final String TABLE_LESSONTEXTS_ID = "_id";
	public static final String TABLE_LESSONTEXTS_LESSONID = "lessonid";
	public static final String TABLE_LESSONTEXTS_TEXTID = "textid";
	public static final String TABLE_LESSONTEXTS_TEXT = "text";
	public static final String TABLE_LESSONTEXTS_TEXTTRANS = "text_trans";
	public static final String TABLE_LESSONTEXTS_TEXTLIT = "text_lit";
	public static final String TABLE_LESSONTEXTS_AUDIOFILEPATH = "audiofile";
	public static final String TABLE_LESSONTEXTS_TEXTTYPE = "texttype";
	private static final String ASSIMIL_CREATE_TABLE_LESSONTEXTS = 
			"create table " + TABLE_LESSONTEXTS + " (" +
			TABLE_LESSONTEXTS_ID + " integer primary key autoincrement, " +
			TABLE_LESSONTEXTS_LESSONID + " integer not null, " + //TODO: reference table "lessons"
			TABLE_LESSONTEXTS_TEXTID + " text not null," +
//			TABLE_LESSONTEXTS_LANG + " integer not null," + //0: orig, 1: translate
			TABLE_LESSONTEXTS_TEXT + " text not null," + //The actual text
			TABLE_LESSONTEXTS_TEXTTRANS + " text," + //The translated text
			TABLE_LESSONTEXTS_TEXTLIT + " text," + //The literal translation of the text
			TABLE_LESSONTEXTS_AUDIOFILEPATH + " text not null," + //The actual text
			TABLE_LESSONTEXTS_TEXTTYPE + " integer not null" + //see enum TextType
			");";
	private static final String ASSIMIL_DROP_TABLE_LESSONTEXTS = 
			"drop TABLE if exists " + TABLE_LESSONTEXTS + ";";
	private static final String ASSIMIL_INDEX_LESSONID_LESSONTEXTS = 
			"create index idx_lessonid on " + TABLE_LESSONTEXTS + "(" + TABLE_LESSONTEXTS_LESSONID + ")";

	private Context ctxt;
			
	/**
	 * @param context
	 * @param name
	 * @param factory
	 * @param version
	 */
	public SelmaSQLiteHelper(Context context, CursorFactory factory) {
		super(context, ASSIMIL_DATABASE_NAME, factory, SELMA_DATABASE_VERSION);
		this.ctxt = context;
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
		else if (newVersion == 3){
			Log.w(this.getClass().getName(), "There is not reasonable way to upgrade from version " +
					oldVersion + " to " + newVersion + ". Dropping database content.");
			db.execSQL(ASSIMIL_DROP_TABLE_LESSONTEXTS);
			db.execSQL(ASSIMIL_DROP_TABLE_LESSONS);
			onCreate(db);
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
	 * @param courseName
	 * @param fullAlbum
	 * @param caller
	 * @param settings
	 */
	abstract public void createIfNotExists(String number, String courseName,
			String fullAlbum, SharedPreferences settings);
	
	public boolean deleteDatabase(){
		return ctxt.deleteDatabase(ASSIMIL_DATABASE_NAME);
	}
	
	/** Find translation on SD card of the given MP3 file.
	 * @param pathStr path to the MP3 file
	 * @return rv[0] contains translation, rv[1] contains literal translation,
	 *         rv[2] contains manually corrected original,
	 *         any of which can be null if no text was found on SD card.
	 */
	protected static String[] findTexts(String pathStr) {
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

	/**
	 * @return
	 */
	public Context getContext() {
		return ctxt;
	}
}
