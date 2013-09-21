/**
 * 
 */
package com.github.federvieh.selma.assimillib;

import java.io.Serializable;
import java.util.ArrayList;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * @author frank
 *
 */
public class AssimilLesson implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7018204971481881787L;
	
	private static final String TITLE_PREFIX = "S00-TITLE-";
	private static final int PREFIX_LENGTH = "S01-".length();
	private static final String STARRED_PREFIX = "STARRED_";
	private String number;
	private String language;
	private String album;
	public Activity activity;
	private ArrayList<AssimilLessonFile> lessonFiles = new ArrayList<AssimilLessonFile>();
	private ArrayList<AssimilLessonFile> translateFiles = new ArrayList<AssimilLessonFile>();
	private ArrayList<String> allTexts = new ArrayList<String>();
	private ArrayList<String> allTracknumbers = new ArrayList<String>();
	private ArrayList<String> lessonTexts = new ArrayList<String>();
//	private SharedPreferences settings;
	private boolean starred = false;

	public AssimilLesson(String number, String language, String album, Activity caller, SharedPreferences settings) {
		this.number = number;
		this.language = language;
		this.album = album;
		this.activity = caller;
//		this.settings = settings;
	}
	
	/**
	 * @return the starred
	 */
	public boolean isStarred() {
		return starred;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString(){
		return album;
	}
	
	public String getNumber(){
		return number;
	}
	/**
	 * 
	 */
	public void star() {
		this.starred = true;
	}

	/**
	 * 
	 */
	public void unstar() {
		this.starred = false;
	}

	public boolean init(SharedPreferences settings){
		starred = settings.getBoolean(STARRED_PREFIX+album, false);
        ContentResolver contentResolver = activity.getContentResolver();
        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = { android.provider.MediaStore.Audio.Media.TITLE,
        		android.provider.MediaStore.Audio.Media.ALBUM,
        		android.provider.MediaStore.Audio.Media._ID
        };
        String findLessonTexts = android.provider.MediaStore.Audio.Media.ALBUM+" = '"+album+"' AND ("+
        		android.provider.MediaStore.Audio.Media.TITLE+" LIKE 'N%-%' OR "+ //NUMBER
        		android.provider.MediaStore.Audio.Media.TITLE+" LIKE 'S%' OR "+   //Text
        		android.provider.MediaStore.Audio.Media.TITLE+" LIKE 'T%')";      //Translate
        Cursor cursor = contentResolver.query(uri, projection, findLessonTexts, null, android.provider.MediaStore.Audio.Media.TITLE);
        if(cursor == null){
        	//TODO: query failed
        	return false;
        }
        else if (!cursor.moveToFirst()){
        	// TODO: no media on device
        	return false;
        }
        else{
        	int titleColumn = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE);
        	int idColumn = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media._ID);
        	//title = S00-TITLE-İki genç
        	//album = ASSIMIL Turkish With Ease - L001
        	do{
        		String fullTitle = cursor.getString(titleColumn);
        		String id = cursor.getString(idColumn);
        		String text = null;
        		String textNumber = null;
        		if(fullTitle.startsWith(TITLE_PREFIX)){
        			text = fullTitle.substring(TITLE_PREFIX.length());
        			textNumber = fullTitle.substring(0, PREFIX_LENGTH-1);
            		this.allTracknumbers.add(textNumber);
            		AssimilLessonFile assimilLessonFile = new AssimilLessonFile(text, id);
            		this.lessonFiles .add(assimilLessonFile);
            		this.allTexts.add(text);
            		this.lessonTexts.add(text);
//            		Log.i("LT", "Title file");
//            		Log.i("LT", "text = '"+text+"'");
//            		Log.i("LT", "id =   '"+id+"'");
//            		Log.i("LT", "==============================================");
        		}
        		else if(fullTitle.matches("S[0-9][0-9]-.*")){
        			text = fullTitle.substring(PREFIX_LENGTH);
        			textNumber = fullTitle.substring(0, PREFIX_LENGTH-1);
            		this.allTracknumbers.add(textNumber);
            		AssimilLessonFile assimilLessonFile = new AssimilLessonFile(text, id);
            		this.lessonFiles .add(assimilLessonFile);
            		this.allTexts.add(text);
            		this.lessonTexts.add(text);
//            		Log.i("LT", "Normal file");
//            		Log.i("LT", "text = '"+text+"'");
//            		Log.i("LT", "id =   '"+id+"'");
//            		Log.i("LT", "==============================================");
        		}
        		else if(fullTitle.matches("T[0-9][0-9]-.*")){
        			text = fullTitle.substring(PREFIX_LENGTH);
        			textNumber = fullTitle.substring(0, PREFIX_LENGTH-1);
            		this.allTracknumbers.add(textNumber);
            		AssimilLessonFile assimilLessonFile = new AssimilLessonFile(text, id);
            		this.translateFiles.add(assimilLessonFile);
            		this.allTexts.add(text);
//            		Log.i("LT", "Translate file");
//            		Log.i("LT", "text = '"+text+"'");
//            		Log.i("LT", "id =   '"+id+"'");
//            		Log.i("LT", "==============================================");
        		}
        		else if(fullTitle.matches("N[0-9]*-.*")){
        			text = fullTitle.substring(fullTitle.indexOf("-")+1);
        			textNumber = fullTitle.substring(0, PREFIX_LENGTH-1);
            		this.allTracknumbers.add(textNumber);
            		AssimilLessonFile assimilLessonFile = new AssimilLessonFile(text, id);
            		this.lessonFiles.add(assimilLessonFile);
            		this.allTexts.add(text);
            		this.lessonTexts.add(text);
//            		Log.i("LT", "Number file");
//            		Log.i("LT", "text = '"+text+"'");
//            		Log.i("LT", "id =   '"+id+"'");
//            		Log.i("LT", "==============================================");
        		}
        		else{
        			//Something's wrong!
            		Log.w("LT", "Unknown file!");
            		Log.w("LT", "text = '"+fullTitle+"'");
            		Log.w("LT", "id =   '"+id+"'");
            		Log.w("LT", "==============================================");
        		}
        	} while (cursor.moveToNext());
        	cursor.close();
        }
		return true;
	}

	/**
	 * @param editor
	 */
	public void store(Editor editor) {
		editor.putBoolean(STARRED_PREFIX+album, starred);
	}

	/**
	 * @return
	 */
	public String[] getTextList() {
		return allTexts.toArray(new String[0]);
	}
	
	public String getTextNumber(int i){
		return this.allTracknumbers.get(i);
	}

	/**
	 * @return
	 */
	public String[] getLessonList() {
		return lessonTexts.toArray(new String[0]);
	}

	/**
	 * @param trackNo
	 * @return
	 */
	public long getIdByTrackNo(int trackNo) {
		AssimilLessonFile file = null;
		if(trackNo < 0){
			Log.w("LT", "Negative trackNo: "+trackNo);
			file = null;
		}
		else if(trackNo < lessonFiles.size()){
			file = lessonFiles.get(trackNo);
		}
		else if (PlaybarManager.isPlayingTranslate()){
			int translateNo = trackNo - lessonFiles.size();
			if(translateNo < translateFiles.size()){
				file = translateFiles.get(translateNo);
			}
			else{
				Log.d("LT", "Invalid trackNo: "+trackNo+"; lesson has "+lessonFiles.size()+
						" lesson files and "+translateFiles.size()+" translate files");
				file = null;
			}
		}
		else{
			Log.d("LT", "Invalid trackNo: "+trackNo+"; lesson has "+lessonFiles.size()+
					" lesson files and "+translateFiles.size()+" translate files, translate is OFF");
			file = null;
		}
		if(file != null){
			String strId = file.getId();
			return Long.parseLong(strId);
		}
		throw new IllegalArgumentException("Could not find track!");
	}

}
