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
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.github.federvieh.selma.R;

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
	private ArrayList<String> allTextsTranslate = new ArrayList<String>();
	private ArrayList<String> allTextsTranslateSimple = new ArrayList<String>();
	private ArrayList<String> allTracknumbers = new ArrayList<String>();
	private ArrayList<String> allPaths = new ArrayList<String>();
	private ArrayList<String> allTranslationFilenames = new ArrayList<String>();
	private ArrayList<String> allLiteralFilenames = new ArrayList<String>();
//	private ArrayList<String> lessonTexts = new ArrayList<String>();
	private int lessonTextNum = 0;
//	private SharedPreferences settings;
	private boolean starred = false;

	private HashMap<String, File[]> files = new HashMap<String, File[]>();



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
        		android.provider.MediaStore.Audio.Media._ID,
        		android.provider.MediaStore.Audio.Media.DATA
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
        			text = fullTitle.substring(TITLE_PREFIX.length());
        			textNumber = fullTitle.substring(0, PREFIX_LENGTH-1);
            		this.allTracknumbers.add(textNumber);
            		AssimilLessonFile assimilLessonFile = new AssimilLessonFile(text, id);
            		this.lessonFiles .add(assimilLessonFile);
            		this.allTexts.add(text);
            		this.lessonTextNum++;
//            		this.lessonTexts.add(text);
            		findTranslations(path);
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
            		this.lessonTextNum++;
//            		this.lessonTexts.add(text);
            		findTranslations(path);
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
            		findTranslations(path);
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
            		this.lessonTextNum++;
//            		this.lessonTexts.add(text);
            		findTranslations(path);
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
        files.clear();
		return true;
	}

	/**
	 * @param path
	 */
	private void findTranslations(String pathStr) {
		StringBuffer fileNamePatt = new StringBuffer(pathStr);
		fileNamePatt.delete(fileNamePatt.length()-4, fileNamePatt.length());
		fileNamePatt.delete(0, fileNamePatt.lastIndexOf("/")+1);
		
		StringBuffer directory = new StringBuffer(pathStr);
		directory.delete(directory.lastIndexOf("/")+1,directory.length());
		
		Log.d("LT", "directory: "+directory.toString());
		Log.d("LT", "fileNamePatt: "+fileNamePatt.toString());
		
		String translatedText = getFileContent(directory.toString(), fileNamePatt+"_translate.txt");
		String translatedTextVerbatim = getFileContent(directory.toString(), fileNamePatt+"_translate_verbatim.txt");
		
		Log.d("LT", "_translate: "+translatedText);
		Log.d("LT", "_translate_verbatim: "+translatedTextVerbatim);
		
		allPaths.add(directory.toString());
		allTranslationFilenames.add(fileNamePatt+"_translate.txt");
		allLiteralFilenames.add(fileNamePatt+"_translate_verbatim.txt");

		if(translatedText!=null){
			allTextsTranslate.add(translatedText);			
		}
		else{
			allTextsTranslate.add(activity.getResources().getText(R.string.not_yet_translated).toString());			
		}
		if(translatedTextVerbatim!=null){
			allTextsTranslateSimple.add(translatedTextVerbatim);			
		}
		else{
			allTextsTranslateSimple.add(activity.getResources().getText(R.string.not_yet_translated).toString());			
		}
	}

	/**
	 * @param d
	 * @param filename
	 * @return
	 */
	private String getFileContent(String directory, String filename) {
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

	/**
	 * @param editor
	 */
	public void store(Editor editor) {
		editor.putBoolean(STARRED_PREFIX+album, starred);
	}

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

	/**
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

	/**
	 * @param pos
	 * @param string
	 */
	public void setTranslateText(int pos, String string) {
		allTextsTranslate.remove(pos);
		allTextsTranslate.add(pos, string);
		try {
			Log.d("LT", allPaths.get(pos)+allTranslationFilenames.get(pos));
			FileOutputStream fos = new FileOutputStream(allPaths.get(pos)+allTranslationFilenames.get(pos));
			OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-16");
			osw.write(string);
			osw.close();
			fos.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @param pos
	 * @param string
	 */
	public void setLiteralText(int pos, String string) {
		allTextsTranslateSimple.remove(pos);
		allTextsTranslateSimple.add(pos, string);
		try {
			Log.d("LT", allPaths.get(pos)+allLiteralFilenames.get(pos));
			FileOutputStream fos = new FileOutputStream(allPaths.get(pos)+allLiteralFilenames.get(pos));
			OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-16");
			osw.write(string);
			osw.close();
			fos.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
