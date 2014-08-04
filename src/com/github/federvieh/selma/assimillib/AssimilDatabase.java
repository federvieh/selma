/**
 * 
 */
package com.github.federvieh.selma.assimillib;

import java.util.ArrayList;
import java.util.HashMap;

import com.github.federvieh.selma.assimillib.dao.AssimilLessonDataSource;
import com.github.federvieh.selma.assimillib.dao.AssimilLessonHeaderDataSource;
import com.github.federvieh.selma.assimillib.dao.AssimilSQLiteHelper;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
//import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * @author frank
 *
 */
public class AssimilDatabase extends ArrayList<AssimilLessonHeader>{

	private static AssimilDatabase assimilDatabase = null;
	
	public static void reset(){
		assimilDatabase=null;
	}
	
	public static boolean isAllocated(){
		return (assimilDatabase!=null);
	}
	
	
	/**
	 * @return the assimildatabase
	 */
	public static synchronized AssimilDatabase getDatabase(Activity caller) {
		if(assimilDatabase==null){
			assimilDatabase = new AssimilDatabase();
			if(!assimilDatabase.isInitialized()){
				assimilDatabase.init(caller);
				if(assimilDatabase.isEmpty()){
					assimilDatabase.scanForLessons(caller);//FIXME: This should also be callable from menu be done elsewhere
					assimilDatabase.init(caller);
				}
			}
		}
		return assimilDatabase;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 3578723726780150820L;
	public static final String LAST_LESSON_PLAYED = "LAST_LESSON_PLAYED";
	public static final String LAST_TRACK_PLAYED = "LAST_TRACK_PLAYED";
	private boolean initialized = false;
	//private SharedPreferences settings = null; 
	private HashMap<Long, AssimilLessonHeader> lessonMap;

	private AssimilDatabase(){
		Log.d("LT", "new database");
	}
	
	private boolean scanForLessons(Activity caller){
        ContentResolver contentResolver = caller.getContentResolver();
        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = { android.provider.MediaStore.Audio.Media.TITLE,
        		android.provider.MediaStore.Audio.Media.ALBUM,
        		android.provider.MediaStore.Audio.Media._ID
        };
        String findLessons = android.provider.MediaStore.Audio.Media.ALBUM+" LIKE '%ASSIMIL %' AND "+
        		android.provider.MediaStore.Audio.Media.TITLE+" LIKE 'S00-TITLE-%'";
        
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
        	SharedPreferences settings = caller.getSharedPreferences("selma", Context.MODE_PRIVATE);
        	//title = S00-TITLE-İki genç
        	//album = ASSIMIL Turkish With Ease - L001
        	do{
        		//TODO: Do something if different languages are installed (maybe let the user choose, which one to handle?)
        		String fullTitle = cursor.getString(titleColumn);
        		String fullAlbum = cursor.getString(albumColumn);
        		String number = fullAlbum.substring(fullAlbum.lastIndexOf("L"));
        		String language = fullAlbum.substring(fullAlbum.indexOf(" ")+1,fullAlbum.indexOf(" - L"));
//        		AssimilLesson assimilLesson = new AssimilLesson(number, language, fullAlbum, caller, settings);
//        		Log.i("LT", "title =  '"+fullTitle+"'");
//        		Log.i("LT", "number = '"+number+"'");
//        		Log.i("LT", "lang =   '"+language+"'");
//        		Log.i("LT", "album  = '"+fullAlbum+"'");
//        		Log.i("LT", "==============================================");
        		AssimilSQLiteHelper.createIfNotExists(number, language, fullAlbum, caller, settings);
//        		this.add(assimilLesson);
        	} while (cursor.moveToNext());
            cursor.close();
            return true;
        }
	}
	
	private boolean init(Activity caller){
		lessonMap = new HashMap<Long, AssimilLessonHeader>();
		this.clear();
		AssimilLessonHeaderDataSource headerDS = new AssimilLessonHeaderDataSource(caller);
		headerDS.open();
		for(AssimilLessonHeader header: headerDS.getLessonHeaders(null)){
			this.add(header);
			lessonMap.put(header.getId(), header);
		}
		headerDS.close();
        initialized  = true;
        SharedPreferences settings = caller.getSharedPreferences("selma", Context.MODE_PRIVATE);
        long lastPlayedLesson = settings.getLong(LAST_LESSON_PLAYED, this.get(0).getId());
        AssimilDatabase ad;
        switch(PlaybarManager.getListType()){
        case LIST_TYPE_STARRED_NO_TRANSLATE:
        case LIST_TYPE_STARRED_TRANSLATE:
        	ad = getStarredOnly(caller);
        	break;
        case LIST_TYPE_ALL_NO_TRANSLATE:
        case LIST_TYPE_ALL_TRANSLATE:
        default:
        	ad = this;
        	break;
        }
        AssimilLessonDataSource lessonDS = new AssimilLessonDataSource(caller);
        AssimilLessonHeader header = lessonMap.get(Long.valueOf(lastPlayedLesson));
        AssimilLesson al = null;
        int lastPlayedTrack;
    	lessonDS.open();
        if(header!=null){
        	al = lessonDS.getLesson(header);
        	lastPlayedTrack = settings.getInt(LAST_TRACK_PLAYED, 0);
        }
        else {
        	try{
        		header = ad.get(0);
        		al = lessonDS.getLesson(header);
        		lastPlayedTrack = 0;
        	}
        	catch(IndexOutOfBoundsException e){
        		//This may happen when starting
        		// * the first time with a valid database
        		// * playmode "starred only"
        		// * no starred items
        		al = null;
        		lastPlayedTrack = -1;
        	}
        }
    	lessonDS.close();
        PlaybarManager.setCurrent(al, lastPlayedTrack);
        return true;
	}

	public boolean isInitialized(){
		return initialized;
	}

	/**
	 * @return
	 */
	public static AssimilDatabase getStarredOnly(Activity caller) {
		AssimilDatabase rv = new AssimilDatabase();
		for (AssimilLessonHeader assimilLesson : getDatabase(caller)) {
			if(assimilLesson.isStarred()){
				rv.add(assimilLesson);
			}
		}
		return rv;
	}

	/** Reads lesson from the database. This may be slow! 
	 * TODO: Think about re-design to use callback (might be complacated in LessonPlayer).
	 * 
	 * @param lessonId
	 * @param ctxt 
	 * @return
	 */
	public static AssimilLesson getLesson(long lessonId, Context ctxt) {
		// TODO Auto-generated method stub
		AssimilLessonHeader header = getDatabase(null).lessonMap.get(Long.valueOf(lessonId));
		AssimilLessonDataSource ds =  new AssimilLessonDataSource(ctxt);
		ds.open();
		AssimilLesson lesson = ds.getLesson(header);
		ds.close();
		return lesson;
	}

}
