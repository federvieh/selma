/**
 * 
 */
package com.github.federvieh.selma.assimillib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
//import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.github.federvieh.selma.assimillib.dao.AssimilLessonDataSource;
import com.github.federvieh.selma.assimillib.dao.AssimilLessonHeaderDataSource;
import com.github.federvieh.selma.assimillib.dao.AssimilPcSQLiteHelper;
import com.github.federvieh.selma.assimillib.dao.AssimilSQLiteHelper;
import com.github.federvieh.selma.assimillib.dao.SelmaSQLiteHelper;

/** 
 * @author frank
 *
 */
public class AssimilDatabase {
	
	public enum LessonType{
		ASSIMIL_MP3_PACK,
		ASSIMIL_PC,
	}

	private static AssimilDatabase assimilDatabase = null;
	
	private ArrayList<AssimilLessonHeader> allLessons = new ArrayList<AssimilLessonHeader>();
	
	public static void reset(){
		assimilDatabase=null;
	}
	
	public static boolean isAllocated(){
		return (assimilDatabase!=null);
	}
	
	
	/**
	 * @return the assimildatabase
	 */
	private static synchronized AssimilDatabase getDatabase(Context caller) {
		return getDatabase(caller, false);
	}

	/**
	 * @param forceScan 
	 * @return the assimildatabase
	 */
	public static synchronized AssimilDatabase getDatabase(Context caller, boolean forceScan) {
		if(forceScan){
			SelmaSQLiteHelper sqlHelper = new AssimilSQLiteHelper(caller);//For now all lessons are stored in the same database, so only one call to "deleteDatabase" is needed.
			if(sqlHelper.deleteDatabase()){
				Log.i("LT", "Database deleted");
			}
			else{
				Log.w("LT", "Database could not be deleted!");
			}
			assimilDatabase = new AssimilDatabase();
			assimilDatabase.scanForLessons(caller);
			assimilDatabase.init(caller);
		}
		else if(assimilDatabase==null){
			assimilDatabase = new AssimilDatabase();
			if(!assimilDatabase.isInitialized()){
				assimilDatabase.init(caller);
//				if(assimilDatabase.isEmpty()){
//						assimilDatabase.scanForLessons(caller);
//						assimilDatabase.init(caller);
//				}
			}
		}
		return assimilDatabase;
	}

	/**
	 * 
	 */
	public static final String LAST_LESSON_PLAYED = "LAST_LESSON_PLAYED";
	public static final String LAST_TRACK_PLAYED = "LAST_TRACK_PLAYED";
	private boolean initialized = false;
	//private SharedPreferences settings = null; 
	private HashMap<Long, AssimilLessonHeader> lessonMap;

	private Object lock = new Object();

	private boolean tainted = true;

	private String currentLang;

	private boolean starredOnly = false;

	private ArrayList<AssimilLessonHeader> currentLessons;

	private AssimilDatabase(){
		Log.d("LT", "new database");
	}
	
	private boolean scanForLessons(Context caller){
        ContentResolver contentResolver = caller.getContentResolver();
        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = { android.provider.MediaStore.Audio.Media.TITLE,
        		android.provider.MediaStore.Audio.Media.ALBUM,
        		android.provider.MediaStore.Audio.Media._ID,
        		android.provider.MediaStore.Audio.Media.ARTIST
        };
        String findLessons = "(" + android.provider.MediaStore.Audio.Media.ALBUM+" LIKE '%ASSIMIL%' AND "+
        		android.provider.MediaStore.Audio.Media.TITLE+" LIKE 'S00-TITLE-%') OR ("+
        		android.provider.MediaStore.Audio.Media.TITLE+" GLOB 'l[0-9][0-9][0-9]_0a')";
        
        Cursor cursor = contentResolver.query(uri, projection, findLessons, null, android.provider.MediaStore.Audio.Media.ALBUM);
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
        	int albumColumn = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ALBUM);
        	int artistColumn = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ARTIST);
        	SharedPreferences settings = caller.getSharedPreferences("selma", Context.MODE_PRIVATE);
        	//title = S00-TITLE-İki genç
        	//album = ASSIMIL Turkish With Ease - L001
        	AssimilSQLiteHelper assimilSqlHelper = new AssimilSQLiteHelper(caller);
        	AssimilPcSQLiteHelper assimilPcSqlHelper = new AssimilPcSQLiteHelper(caller);
        	do{
        		String fullTitle = cursor.getString(titleColumn);
        		String fullAlbum = cursor.getString(albumColumn);
        		String language = cursor.getString(artistColumn);//fullAlbum.substring(fullAlbum.indexOf(" ")+1,fullAlbum.indexOf(" - L"));
        		Log.i("LT", "title =  '"+fullTitle+"'");
        		Log.i("LT", "lang =   '"+language+"'");
        		Log.i("LT", "album  = '"+fullAlbum+"'");
        		Log.i("LT", "==============================================");
        		Pattern patternAssimilPcMp3 = Pattern.compile("l([0-9][0-9][0-9])_0a");
        		Matcher matcherAssimilPcMp3 = patternAssimilPcMp3.matcher(fullTitle);
        		if(matcherAssimilPcMp3.find()){
        			String number = null;
            		Matcher matcherAssimilPcMp3Lesson = patternAssimilPcMp3.matcher(fullTitle);
        			if(matcherAssimilPcMp3Lesson.find()){
        				number = matcherAssimilPcMp3Lesson.group(1);
        			}
        			else{
        				Log.w("LT", "Could not find lesson number");
        				continue;
        			}
        			assimilPcSqlHelper.createIfNotExists(number, language, fullAlbum, settings);
        		}
        		else{
        			Pattern patternAssimilLesson = Pattern.compile("L[0-9]+");
        			Matcher m = patternAssimilLesson.matcher(fullAlbum);
        			String number = null; //fullAlbum.substring(fullAlbum.lastIndexOf("L"));
        			if(m.find()){
        				number = m.group();
        			}
        			else{
        				Log.w("LT", "Could not find lesson number");
        				continue;
        			}
        			assimilSqlHelper.createIfNotExists(number, language, fullAlbum, settings);
        		}
        	} while (cursor.moveToNext());
            cursor.close();
            return true;
        }
	}
	
	private boolean init(Context caller){
		lessonMap = new HashMap<Long, AssimilLessonHeader>();
		allLessons.clear();
		LessonType type = LessonType.ASSIMIL_MP3_PACK; //When reading data it's actually not needed to choose a lesson type
		AssimilLessonHeaderDataSource headerDS = new AssimilLessonHeaderDataSource(type, caller);
		headerDS.open();
		for(AssimilLessonHeader header: headerDS.getLessonHeaders(null)){
			allLessons.add(header);
			lessonMap.put(header.getId(), header);
		}
		headerDS.close();
        initialized  = true;
        return true;
	}

	private boolean isInitialized(){
		return initialized;
	}

	/** Reads lesson from the database. This may be slow! 
	 * TODO: Think about re-design to use callback (might be complicated in LessonPlayer).
	 * 
	 * @param lessonId
	 * @param ctxt 
	 * @return
	 */
	public static AssimilLesson getLesson(long lessonId, Context ctxt) {
		AssimilLessonHeader header = getDatabase(ctxt).lessonMap.get(Long.valueOf(lessonId));
		SelmaSQLiteHelper helper = null;
		switch(header.getType()){
		case ASSIMIL_MP3_PACK:
			helper = new AssimilSQLiteHelper(ctxt);
			break;
		case ASSIMIL_PC:
			helper = new AssimilPcSQLiteHelper(ctxt);
			break;
		}
		AssimilLessonDataSource ds =  new AssimilLessonDataSource(helper);
		ds.open();
		AssimilLesson lesson = ds.getLesson(header);
		ds.close();
		return lesson;
	}

	/**
	 * @return
	 */
	public List<AssimilLessonHeader> getAllLessonHeaders() {
		return allLessons;
	}
	
	public static void setLang(String lang){
		AssimilDatabase ad = getDatabase(null);
		synchronized(ad.lock){
			ad.tainted  = true;
			ad.currentLang = lang;
		}
	}
	
	public static void setStarredOnly(boolean starredOnly){
		AssimilDatabase ad = getDatabase(null);
		synchronized(ad.lock ){
			ad.tainted  = true;
			ad.starredOnly  = starredOnly;
		}
	}

	public static ArrayList<AssimilLessonHeader> getCurrentLessons(){
		return getDatabase(null).getCurrentLessons_priv();
	}

	private ArrayList<AssimilLessonHeader> getCurrentLessons_priv(){
		if(tainted){
			ArrayList<AssimilLessonHeader> temp = new ArrayList<AssimilLessonHeader>();
			synchronized(lock){
				for(AssimilLessonHeader alh: allLessons){
					if(currentLang==null || alh.getLang().equals(currentLang)){
						if((!starredOnly)||(alh.isStarred())){
							temp.add(alh);
						}
					}
				}
				currentLessons = temp;
				tainted = false;
			}
		}
		return currentLessons;
	}

	/**
	 * @return
	 */
	public static ArrayList<String> getAllCourses() {
		HashSet<String> temp = new HashSet<String>();
		AssimilDatabase ad = AssimilDatabase.getDatabase(null);
		for(AssimilLessonHeader lh : ad.allLessons){
			temp.add(lh.getLang());
		}
		ArrayList<String> rv = new ArrayList<String>();
		if(temp.size()>1){   //If more than one course
			rv.add(null);    //Add entry for "All courses"
		}
		rv.addAll(temp); //Add rest
		return rv;
	}

	/**
	 * @return
	 */
	public static boolean isStarredOnly() {
		AssimilDatabase ad = getDatabase(null);
		return ad.starredOnly;
	}

	/**
	 * @return
	 */
	public static String getLang() {
		AssimilDatabase ad = getDatabase(null);
		return ad.currentLang;
	}

}
