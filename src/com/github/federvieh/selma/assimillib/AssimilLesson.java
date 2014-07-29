/**
 * 
 */
package com.github.federvieh.selma.assimillib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.github.federvieh.selma.R;
import com.github.federvieh.selma.assimillib.dao.AssimilSQLiteHelper;

/**
 * @author frank
 *
 */
public class AssimilLesson implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7018204971481881787L;
	
	private static final String STARRED_PREFIX = "STARRED_";
	private String number;
	private String language;
	private String album;
//	public Activity activity;
//	private ArrayList<AssimilLessonFile> lessonFiles = new ArrayList<AssimilLessonFile>();
//	private ArrayList<AssimilLessonFile> translateFiles = new ArrayList<AssimilLessonFile>();
	private ArrayList<String> allTexts = new ArrayList<String>();
	private ArrayList<String> allTextsTranslate = new ArrayList<String>();
	private ArrayList<String> allTextsTranslateSimple = new ArrayList<String>();
	private ArrayList<String> allTracknumbers = new ArrayList<String>();
	private ArrayList<String> allAudioFiles = new ArrayList<String>();
	private ArrayList<Integer> allIds = new ArrayList<Integer>();
//	private ArrayList<String> allPaths = new ArrayList<String>();
//	private ArrayList<String> allTranslationFilenames = new ArrayList<String>();
//	private ArrayList<String> allLiteralFilenames = new ArrayList<String>();
//	private ArrayList<String> lessonTexts = new ArrayList<String>();
	private int lessonTextNum = 0;
//	private SharedPreferences settings;
//	private boolean starred = false;

	private AssimilLessonHeader header;





//	public AssimilLesson(String number, String language, String album, Activity caller, SharedPreferences settings) {
//		this.number = number;
//		this.language = language;
//		this.album = album;
//		this.activity = caller;
////		this.settings = settings;
//	}
	
	/**
	 * @param header
	 */
	public AssimilLesson(AssimilLessonHeader header) {
		this.header = header;
	}

	/**
	 * @return the starred
	 */
	public boolean isStarred() {
		return header.isStarred();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString(){
		return header.getName();
	}
	
	public String getNumber(){
		return header.getName();
	}
//	/**
//	 * 
//	 */
//	public void star() {
//		this.starred = true;
//	}
//
//	/**
//	 * 
//	 */
//	public void unstar() {
//		this.starred = false;
//	}

//	//TODO: Delete me!?
//	public boolean create(SharedPreferences settings){
//		//FIXME: Write this info to table lessons and lesson_texts
//		starred = settings.getBoolean(STARRED_PREFIX+album, false);
//        ContentResolver contentResolver = activity.getContentResolver();
//        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
//        String[] projection = { android.provider.MediaStore.Audio.Media.TITLE,
//        		android.provider.MediaStore.Audio.Media.ALBUM,
//        		android.provider.MediaStore.Audio.Media._ID,
//        		android.provider.MediaStore.Audio.Media.DATA
//        };
//        String findLessonTexts = android.provider.MediaStore.Audio.Media.ALBUM+" = '"+album+"' AND ("+
//        		android.provider.MediaStore.Audio.Media.TITLE+" LIKE 'N%-%' OR "+ //NUMBER
//        		android.provider.MediaStore.Audio.Media.TITLE+" LIKE 'S%' OR "+   //Text
//        		android.provider.MediaStore.Audio.Media.TITLE+" LIKE 'T%')";      //Translate
//        Cursor cursor = contentResolver.query(uri, projection, findLessonTexts, null, android.provider.MediaStore.Audio.Media.TITLE);
//        if(cursor == null){
//        	//TODO: query failed
//        	return false;
//        }
//        else if (!cursor.moveToFirst()){
//        	// TODO: no media on device
//        	return false;
//        }
//        else{
//        	int titleColumn = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE);
//        	int idColumn = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media._ID);
//        	int dataColumn = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.DATA);
//        	//title = S00-TITLE-İki genç
//        	//album = ASSIMIL Turkish With Ease - L001
//        	do{
//        		String fullTitle = cursor.getString(titleColumn);
//        		String id = cursor.getString(idColumn);
//        		String path = cursor.getString(dataColumn);
////        		Log.i("LT", "Path: "+path);
//        		String text = null;
//        		String textNumber = null;
//        		if(fullTitle.startsWith(TITLE_PREFIX)){
//        			text = fullTitle.substring(TITLE_PREFIX.length());
//        			textNumber = fullTitle.substring(0, PREFIX_LENGTH-1);
//            		this.allTracknumbers.add(textNumber);
//            		AssimilLessonFile assimilLessonFile = new AssimilLessonFile(text, id);
//            		this.lessonFiles .add(assimilLessonFile);
//            		this.allTexts.add(text);
//            		this.lessonTextNum++;
////            		this.lessonTexts.add(text);
//            		findTranslations(path);
////            		Log.i("LT", "Title file");
////            		Log.i("LT", "text = '"+text+"'");
////            		Log.i("LT", "id =   '"+id+"'");
////            		Log.i("LT", "==============================================");
//        		}
//        		else if(fullTitle.matches("S[0-9][0-9]-.*")){
//        			text = fullTitle.substring(PREFIX_LENGTH);
//        			textNumber = fullTitle.substring(0, PREFIX_LENGTH-1);
//            		this.allTracknumbers.add(textNumber);
//            		AssimilLessonFile assimilLessonFile = new AssimilLessonFile(text, id);
//            		this.lessonFiles .add(assimilLessonFile);
//            		this.allTexts.add(text);
//            		this.lessonTextNum++;
////            		this.lessonTexts.add(text);
//            		findTranslations(path);
////            		Log.i("LT", "Normal file");
////            		Log.i("LT", "text = '"+text+"'");
////            		Log.i("LT", "id =   '"+id+"'");
////            		Log.i("LT", "==============================================");
//        		}
//        		else if(fullTitle.matches("T[0-9][0-9]-.*")){
//        			text = fullTitle.substring(PREFIX_LENGTH);
//        			textNumber = fullTitle.substring(0, PREFIX_LENGTH-1);
//            		this.allTracknumbers.add(textNumber);
//            		AssimilLessonFile assimilLessonFile = new AssimilLessonFile(text, id);
//            		this.translateFiles.add(assimilLessonFile);
//            		this.allTexts.add(text);
//            		findTranslations(path);
////            		Log.i("LT", "Translate file");
////            		Log.i("LT", "text = '"+text+"'");
////            		Log.i("LT", "id =   '"+id+"'");
////            		Log.i("LT", "==============================================");
//        		}
//        		else if(fullTitle.matches("N[0-9]*-.*")){
//        			text = fullTitle.substring(fullTitle.indexOf("-")+1);
//        			textNumber = fullTitle.substring(0, PREFIX_LENGTH-1);
//            		this.allTracknumbers.add(textNumber);
//            		AssimilLessonFile assimilLessonFile = new AssimilLessonFile(text, id);
//            		this.lessonFiles.add(assimilLessonFile);
//            		this.allTexts.add(text);
//            		this.lessonTextNum++;
////            		this.lessonTexts.add(text);
//            		findTranslations(path);
////            		Log.i("LT", "Number file");
////            		Log.i("LT", "text = '"+text+"'");
////            		Log.i("LT", "id =   '"+id+"'");
////            		Log.i("LT", "==============================================");
//        		}
//        		else{
//        			//Something's wrong!
//            		Log.w("LT", "Unknown file!");
//            		Log.w("LT", "text = '"+fullTitle+"'");
//            		Log.w("LT", "id =   '"+id+"'");
//            		Log.w("LT", "==============================================");
//        		}
//        		
//        	} while (cursor.moveToNext());
//        	cursor.close();
//        }
//        files.clear();
//		return true;
//	}


//	/**
//	 * @param editor
//	 */
//	public void store(Editor editor) {
//		editor.putBoolean(STARRED_PREFIX+album, starred);
//	}

	/** 
	 * @param displayMode 
	 * @return
	 */
	public String[] getTextList(DisplayMode displayMode) {
		switch (displayMode) {
		case ORIGINAL_TEXT:
			return allTexts.toArray(new String[0]);
		case LITERAL:
			return allTextsTranslateSimple.toArray(new String[0]);
		case TRANSLATION:
			return allTextsTranslate.toArray(new String[0]);
		}
		return allTexts.toArray(new String[0]);
	}
	
	public String getTextNumber(int i){
		return this.allTracknumbers.get(i);
	}

	/** Returns the list of lessons texts (i.e. not excercises).
	 * @param displayMode 
	 * @return
	 */
	public String[] getLessonList(DisplayMode displayMode) {
		ArrayList<String> baseList = allTexts;
		switch (displayMode) {
		case ORIGINAL_TEXT:
			baseList = allTexts;
			break;
		case LITERAL:
			baseList = allTextsTranslateSimple;
			break;
		case TRANSLATION:
			baseList = allTextsTranslate;
			break;
		default:
			break;
		}
		
		return (baseList.subList(0, lessonTextNum)).toArray(new String[0]);
	}

	/**
	 * @param trackNo
	 * @return
	 */
	public String getPathByTrackNo(int trackNo) {
		if(trackNo < 0){
			Log.w("LT", "Negative trackNo: "+trackNo);
		}
		else if((trackNo < lessonTextNum)||
				((PlaybarManager.isPlayingTranslate())&&(trackNo<allAudioFiles.size()))){
			return allAudioFiles.get(trackNo);
		}
		Log.d("LT", "Invalid trackNo: "+trackNo+"; lesson has "+lessonTextNum+
				" lesson files and "+allAudioFiles.size()+" total files, translate is " + (PlaybarManager.isPlayingTranslate()?"ON":"OFF"));
		throw new IllegalArgumentException("Could not find track!");
	}

	/**
	 * @param pos
	 * @param string
	 */
	public void setTranslateText(int pos, String string) {
		allTextsTranslate.remove(pos);
		allTextsTranslate.add(pos, string);
		//FIXME: Store in DB
//		try {
//			Log.d("LT", allPaths.get(pos)+allTranslationFilenames.get(pos));
//			FileOutputStream fos = new FileOutputStream(allPaths.get(pos)+allTranslationFilenames.get(pos));
//			OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-16");
//			osw.write(string);
//			osw.close();
//			fos.close();
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (UnsupportedEncodingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

	/**
	 * @param pos
	 * @param string
	 */
	public void setLiteralText(int pos, String string) {
		allTextsTranslateSimple.remove(pos);
		allTextsTranslateSimple.add(pos, string);
		//FIXME: Store in DB
//		try {
//			Log.d("LT", allPaths.get(pos)+allLiteralFilenames.get(pos));
//			FileOutputStream fos = new FileOutputStream(allPaths.get(pos)+allLiteralFilenames.get(pos));
//			OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-16");
//			osw.write(string);
//			osw.close();
//			fos.close();
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (UnsupportedEncodingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

	/**
	 * @return
	 */
	public AssimilLessonHeader getHeader() {
		return header;
	}

	/** Add text (and its translation and audio file) to a lesson.
	 * 
	 * @param textId ID of the text (like S01, T01)
	 * @param text The actual text
	 * @param texttrans translation of the text
	 * @param textlit the literal translation of the text
	 * @param id the database ID
	 * @param audioPath The audio file
	 */
	public void addText(String textId, String text, String texttrans,
			String textlit, int id, String audioPath) {
		Log.d("LT", "addText(" + textId + ", " + text + ", " + texttrans +
				", " + textlit + ", " + id + ", " + audioPath + ")");
		this.allTexts.add(text);
		this.allTextsTranslate.add(texttrans);
		this.allTextsTranslateSimple.add(textlit);
		this.allTracknumbers.add(textId);
		this.allAudioFiles.add(audioPath);
		this.allIds.add(id);
		if((textId.startsWith(AssimilSQLiteHelper.TITLE_PREFIX))||
				(textId.matches("S[0-9][0-9]"))){
			lessonTextNum++;
		}
	}


}
