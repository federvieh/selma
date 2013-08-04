/**
 * 
 */
package com.github.federvieh.selma.assimillib;

import java.util.ArrayList;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;

/**
 * @author frank
 *
 */
public class AssimilDatabase extends ArrayList<AssimilLesson>{

	private static AssimilDatabase assimilDatabase = null;
	
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
			}
		}
		return assimilDatabase;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 3578723726780150820L;
	private boolean initialized = false;
	private SharedPreferences settings = null; 

	private AssimilDatabase(){
	}
	
	public boolean init(Activity caller){
        ContentResolver contentResolver = caller.getContentResolver();
        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = { android.provider.MediaStore.Audio.Media.TITLE,
        		android.provider.MediaStore.Audio.Media.ALBUM,
        		android.provider.MediaStore.Audio.Media._ID
        };
        String findLessons = android.provider.MediaStore.Audio.Media.ALBUM+" LIKE '%ASSIMIL Turkish With Ease%' AND "+
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
        	settings = caller.getPreferences(Context.MODE_PRIVATE);
        	//title = S00-TITLE-İki genç
        	//album = ASSIMIL Turkish With Ease - L001
        	do{
        		//TODO: Do something if different languages are installed (maybe let the user choose, which one to handle?)
        		String fullTitle = cursor.getString(titleColumn);
        		String fullAlbum = cursor.getString(albumColumn);
        		String number = fullAlbum.substring(fullAlbum.lastIndexOf("L"));
        		String language = fullAlbum.substring(fullAlbum.indexOf(" ")+1,fullAlbum.indexOf(" - L"));
        		AssimilLesson assimilLesson = new AssimilLesson(number, language, fullAlbum, caller, settings);
//        		Log.i("LT", "title =  '"+fullTitle+"'");
//        		Log.i("LT", "number = '"+number+"'");
//        		Log.i("LT", "lang =   '"+language+"'");
//        		Log.i("LT", "album  = '"+fullAlbum+"'");
//        		Log.i("LT", "==============================================");
        		assimilLesson.init();
        		this.add(assimilLesson);
        	} while (cursor.moveToNext());
            cursor.close();
        }
        initialized  = true;
		return true;
	}

	public boolean isInitialized(){
		return initialized;
	}

	/**
	 * 
	 */
	public void commit() {
		SharedPreferences.Editor editor = settings.edit();
		for (AssimilLesson al : this) {
			al.store(editor);
		}

		// Commit the edits!
		editor.commit();
	}

	/**
	 * @return
	 */
	public static AssimilDatabase getStarredOnly(Activity caller) {
		AssimilDatabase rv = new AssimilDatabase();
		for (AssimilLesson assimilLesson : getDatabase(caller)) {
			if(assimilLesson.isStarred()){
				rv.add(assimilLesson);
			}
		}
		return rv;
	}

}
