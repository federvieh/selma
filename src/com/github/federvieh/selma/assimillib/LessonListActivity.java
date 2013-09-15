package com.github.federvieh.selma.assimillib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.github.federvieh.selma.R;

public class LessonListActivity extends ActionBarActivity {
	
	public static final String LIST_MODE = "LIST_MODE";

	private static ListTypes lt;// = ListTypes.LIST_TYPE_ALL_TRANSLATE;
	
	static TextView headerViewNoStarred;
	static TextView headerViewNoFiles;
	enum ActivityState{
		DATABASE_LOADING,
		READY_FOR_PLAYBACK
	}

	//private SharedPreferences settings;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(lt==null){
			Log.d("LT", this.getClass().getSimpleName()+".onCreate(); Reading settings.");
			SharedPreferences settings = getSharedPreferences("selma", Context.MODE_PRIVATE);
			int i = settings.getInt(LIST_MODE, ListTypes.LIST_TYPE_ALL_TRANSLATE.ordinal());
			Log.d("LT", this.getClass().getSimpleName()+".onCreate(); i="+i);
			lt = ListTypes.values()[i];
			PlaybarManager.setListType(lt);
		}
		Log.d("LT", this.getClass().getSimpleName()+".onCreate(); lt="+lt);

		if(headerViewNoStarred==null){
			headerViewNoStarred = new TextView(this);
			headerViewNoStarred.setPadding(10, 10, 10, 10);
			headerViewNoStarred.setTextSize(32);
			headerViewNoStarred.setText(getResources().getText(R.string.warning_no_starred));
		}
		if(headerViewNoFiles==null){
			headerViewNoFiles = new TextView(this);
			headerViewNoFiles.setPadding(10, 10, 10, 10);
			headerViewNoFiles.setTextSize(32);
			headerViewNoFiles.setText(getResources().getText(R.string.warning_no_starred));
		}
		if(!AssimilDatabase.isAllocated()){
			showWaiting(ActivityState.DATABASE_LOADING);
			new DatabaseInitTask().execute(this);
		}
		else{
			showWaiting(ActivityState.READY_FOR_PLAYBACK);
		}
	}
	
	private void updateListType(){
		//PlaybarManager.setListType(lt);
		lt = PlaybarManager.getListType();
		Log.d("LT", this.getClass().getSimpleName()+".updateListType(); lt="+lt.ordinal());
		Editor editor = getSharedPreferences("selma", Context.MODE_PRIVATE).edit();
		editor.putInt(LIST_MODE, lt.ordinal());
		editor.commit();
		AssimilDatabase ad = AssimilDatabase.getDatabase(this);
		switch(lt){
		case LIST_TYPE_ALL_NO_TRANSLATE:
			break;
		case LIST_TYPE_ALL_TRANSLATE:
			break;
		case LIST_TYPE_STARRED_NO_TRANSLATE:
			ad = AssimilDatabase.getStarredOnly(this);
			if((PlaybarManager.getLessonInstance()!=null)&&(!PlaybarManager.getLessonInstance().isStarred())){
				LessonPlayer.stopPlaying(this);
				PlaybarManager.setCurrent(null, -1);
			}
			break;
		case LIST_TYPE_STARRED_TRANSLATE:
			if((PlaybarManager.getLessonInstance()!=null)&&(!PlaybarManager.getLessonInstance().isStarred())){
				LessonPlayer.stopPlaying(this);
				PlaybarManager.setCurrent(null, -1);
			}
			ad = AssimilDatabase.getStarredOnly(this);
			break;			
		}
		/* show the content */
		AssimilLessonListAdapter assimilLessonListAdapter;
		assimilLessonListAdapter = new AssimilLessonListAdapter(this, ad, lt);
		ListView listView = (ListView) findViewById(R.id.listView1);
		listView.removeHeaderView(headerViewNoStarred);
		listView.removeHeaderView(headerViewNoFiles);
		if(ad.isEmpty()){
			listView.setAdapter(null);
			switch(lt){
			case LIST_TYPE_ALL_NO_TRANSLATE:
			case LIST_TYPE_ALL_TRANSLATE:
				listView.addHeaderView(headerViewNoFiles);
				break;
			case LIST_TYPE_STARRED_NO_TRANSLATE:
			case LIST_TYPE_STARRED_TRANSLATE:
				listView.addHeaderView(headerViewNoStarred);
				break;
			}
		}
		
		listView.setAdapter(assimilLessonListAdapter);
		Playbar playbar = (Playbar) findViewById(R.id.playbar1);
		playbar.update();
		PlaybarManager.setPbInstance(playbar);
	}
	/**
	 * @param b
	 */
	private void showWaiting(ActivityState databaseLoading) {
		Log.d("LT", "databaseLoading="+databaseLoading);
		if(databaseLoading==ActivityState.DATABASE_LOADING){
			setContentView(R.layout.activity_main);
		}
		else {
			setContentView(R.layout.activity_lesson_list);
			setTitle("");
			SpinnerAdapter mSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.playback_option_list,
					android.R.layout.simple_spinner_dropdown_item);
			ActionBar.OnNavigationListener mOnNavigationListener = new ActionBar.OnNavigationListener() {
				@Override
				public boolean onNavigationItemSelected(int position, long itemId) {
					switch(lt){
					case LIST_TYPE_ALL_NO_TRANSLATE:
					case LIST_TYPE_STARRED_NO_TRANSLATE:
						if(position==0){
							lt = ListTypes.LIST_TYPE_ALL_NO_TRANSLATE;
						}
						else{
							lt = ListTypes.LIST_TYPE_STARRED_NO_TRANSLATE;
						}
						break;
					case LIST_TYPE_STARRED_TRANSLATE:
					case LIST_TYPE_ALL_TRANSLATE:
						if(position==0){
							lt = ListTypes.LIST_TYPE_ALL_TRANSLATE;
						}
						else{
							lt = ListTypes.LIST_TYPE_STARRED_TRANSLATE;
						}
						break;
					}
					Log.d("LT", this.getClass().getSimpleName()+".onNavigationItemSelected(); lt="+lt);
					PlaybarManager.setListType(lt);
					updateListType();
					return true;
				}
			};
			getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
			getSupportActionBar().setListNavigationCallbacks(mSpinnerAdapter, mOnNavigationListener);
			int navItem = 0;
			switch(lt){
			case LIST_TYPE_ALL_NO_TRANSLATE:
			case LIST_TYPE_ALL_TRANSLATE:
				navItem = 0;
				break;
			case LIST_TYPE_STARRED_NO_TRANSLATE:
			case LIST_TYPE_STARRED_TRANSLATE:
				navItem = 1;
				break;
			}
			getSupportActionBar().setSelectedNavigationItem(navItem);
		}
	}

	@Override
	protected void onPause(){
		super.onPause();
		Log.d("LT", this.getClass().getSimpleName()+".onPause()");
		AssimilDatabase.getDatabase(this).commit(this);
	}

	@Override
	protected void onResume(){
		super.onResume();
		Log.d("LT", this.getClass().getSimpleName()+".onResume()");
		lt = PlaybarManager.getListType();
		Log.d("LT", this.getClass().getSimpleName()+".onResume(); lt="+lt);
		Playbar playbar = (Playbar) findViewById(R.id.playbar1);
		PlaybarManager.setPbInstance(playbar);
	}

	@Override
	protected void onDestroy(){
		super.onDestroy();
		Log.i("LT", this.getClass().getSimpleName()+" being destroyed");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
	        case R.id.action_license:
	            openLicense();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	/**
	 * 
	 */
	private void openLicense() {
		// 1. Instantiate an AlertDialog.Builder with its constructor
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setMessage(readRawTextFile(getApplicationContext(), R.raw.license))
		       .setTitle(R.string.action_license);

		builder.setPositiveButton(R.string.show_license, new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
//	        	   Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.gnu.org/licenses/gpl-2.0-standalone.html"));
//	        	   startActivity(browserIntent);
	        	   openGPL();
	           }
	       });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });

		AlertDialog dialog = builder.create();
		dialog.show();
	}

	/**
	 * 
	 */
	private void openGPL() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    // Get the layout inflater
	    LayoutInflater inflater = getLayoutInflater();

	    // Inflate and set the layout for the dialog
	    // Pass null as the parent view because its going in the dialog layout
	    View layoutView = inflater.inflate(R.layout.license_view, null);
	    builder.setView(layoutView);
	    TextView textView = (TextView)layoutView.findViewById(R.id.textViewLicense);
	    textView.setText(readRawTextFile(getApplicationContext(), R.raw.gpl20));
		textView.setHorizontallyScrolling(true);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User closed the dialog
            }
        });

		AlertDialog dialog = builder.create();
		dialog.show();
	}

	private static String readRawTextFile(Context ctx, int resId)
	{
	    InputStream inputStream = ctx.getResources().openRawResource(resId);

	    InputStreamReader inputreader = new InputStreamReader(inputStream);
	    BufferedReader buffreader = new BufferedReader(inputreader);
	    String line;
	    StringBuilder text = new StringBuilder();

	    try {
	        while (( line = buffreader.readLine()) != null) {
	            text.append(line);
	            text.append('\n');
	        }
	    } catch (IOException e) {
	        return null;
	    }
	    return text.toString();
	}

	private class DatabaseInitTask extends AsyncTask<Activity, Void, ActivityState> {
	    /** The system calls this to perform work in the UI thread and delivers
	      * the result from doInBackground() */
	    protected void onPostExecute(ActivityState result) {
	    	showWaiting(result);
	    }

		/* (non-Javadoc)
		 * @see android.os.AsyncTask#doInBackground(java.lang.Object[])
		 */
		@Override
		protected ActivityState doInBackground(Activity... arg0) {
			AssimilDatabase.getDatabase(arg0[0]);
			return ActivityState.READY_FOR_PLAYBACK;
		}
	}

}

/*
07-16 23:09:23.304: I/LT(7661): title =  'S00-TITLE-İki genç'
07-16 23:09:23.304: I/LT(7661): number = 'L001'
07-16 23:09:23.304: I/LT(7661): lang =   'Turkish With Ease'
07-16 23:09:23.304: I/LT(7661): album  = 'ASSIMIL Turkish With Ease - L001'
07-16 23:09:23.304: I/LT(7661): ==============================================
07-16 23:09:23.304: I/LT(7661): title =  'S00-TITLE-Denize gidelim'
07-16 23:09:23.304: I/LT(7661): number = 'L002'
07-16 23:09:23.304: I/LT(7661): lang =   'Turkish With Ease'
07-16 23:09:23.304: I/LT(7661): album  = 'ASSIMIL Turkish With Ease - L002'
07-16 23:09:23.304: I/LT(7661): ==============================================
07-16 23:09:23.304: I/LT(7661): title =  'S00-TITLE-İstanbul’da'
07-16 23:09:23.304: I/LT(7661): number = 'L003'
07-16 23:09:23.304: I/LT(7661): lang =   'Turkish With Ease'
07-16 23:09:23.304: I/LT(7661): album  = 'ASSIMIL Turkish With Ease - L003'
07-16 23:09:23.304: I/LT(7661): ==============================================
07-16 23:09:23.304: I/LT(7661): title =  'S00-TITLE-Trende meraklı bir yolcu'
07-16 23:09:23.304: I/LT(7661): number = 'L004'
07-16 23:09:23.304: I/LT(7661): lang =   'Turkish With Ease'
07-16 23:09:23.304: I/LT(7661): album  = 'ASSIMIL Turkish With Ease - L004'
07-16 23:09:23.304: I/LT(7661): ==============================================
07-16 23:09:23.304: I/LT(7661): title =  'S00-TITLE-Balıkçılar'
07-16 23:09:23.304: I/LT(7661): number = 'L005'
07-16 23:09:23.304: I/LT(7661): lang =   'Turkish With Ease'
07-16 23:09:23.304: I/LT(7661): album  = 'ASSIMIL Turkish With Ease - L005'
07-16 23:09:23.304: I/LT(7661): ==============================================
07-16 23:09:23.304: I/LT(7661): title =  'S00-TITLE-Bahçede'
07-16 23:09:23.304: I/LT(7661): number = 'L006'
07-16 23:09:23.304: I/LT(7661): lang =   'Turkish With Ease'
07-16 23:09:23.304: I/LT(7661): album  = 'ASSIMIL Turkish With Ease - L006'
07-16 23:09:23.304: I/LT(7661): ==============================================
07-16 23:09:23.304: I/LT(7661): title =  'S00-TITLE-İki fıkra'
07-16 23:09:23.304: I/LT(7661): number = 'L008'
07-16 23:09:23.304: I/LT(7661): lang =   'Turkish With Ease'
07-16 23:09:23.304: I/LT(7661): album  = 'ASSIMIL Turkish With Ease - L008'
07-16 23:09:23.304: I/LT(7661): ==============================================
07-16 23:09:23.308: I/LT(7661): title =  'S00-TITLE-Boğaz’a gidelim'
07-16 23:09:23.308: I/LT(7661): number = 'L009'
07-16 23:09:23.308: I/LT(7661): lang =   'Turkish With Ease'
07-16 23:09:23.308: I/LT(7661): album  = 'ASSIMIL Turkish With Ease - L009'
07-16 23:09:23.308: I/LT(7661): ==============================================
07-16 23:09:23.308: I/LT(7661): title =  'S00-TITLE-Lokantada'
07-16 23:09:23.308: I/LT(7661): number = 'L010'
07-16 23:09:23.308: I/LT(7661): lang =   'Turkish With Ease'
07-16 23:09:23.308: I/LT(7661): album  = 'ASSIMIL Turkish With Ease - L010'
07-16 23:09:23.308: I/LT(7661): ==============================================
07-16 23:09:23.308: I/LT(7661): title =  'S00-TITLE-Biraz dedikodu'
07-16 23:09:23.308: I/LT(7661): number = 'L011'
07-16 23:09:23.308: I/LT(7661): lang =   'Turkish With Ease'
07-16 23:09:23.308: I/LT(7661): album  = 'ASSIMIL Turkish With Ease - L011'
07-16 23:09:23.308: I/LT(7661): ==============================================
07-16 23:09:23.308: I/LT(7661): title =  'S00-TITLE-Önemli bir misafir'
07-16 23:09:23.308: I/LT(7661): number = 'L012'
07-16 23:09:23.308: I/LT(7661): lang =   'Turkish With Ease'
07-16 23:09:23.308: I/LT(7661): album  = 'ASSIMIL Turkish With Ease - L012'
07-16 23:09:23.308: I/LT(7661): ==============================================
07-16 23:09:23.308: I/LT(7661): title =  'S00-TITLE-Bir öğrenciden mektup'
07-16 23:09:23.308: I/LT(7661): number = 'L013'
07-16 23:09:23.308: I/LT(7661): lang =   'Turkish With Ease'
07-16 23:09:23.308: I/LT(7661): album  = 'ASSIMIL Turkish With Ease - L013'
07-16 23:09:23.308: I/LT(7661): ==============================================
07-16 23:09:23.308: I/LT(7661): title =  'S00-TITLE-Otelde'
07-16 23:09:23.308: I/LT(7661): number = 'L015'
07-16 23:09:23.308: I/LT(7661): lang =   'Turkish With Ease'
07-16 23:09:23.312: I/LT(7661): album  = 'ASSIMIL Turkish With Ease - L015'
07-16 23:09:23.312: I/LT(7661): ==============================================
07-16 23:09:23.312: I/LT(7661): title =  'S00-TITLE-Halıcıda'
07-16 23:09:23.312: I/LT(7661): number = 'L016'
07-16 23:09:23.312: I/LT(7661): lang =   'Turkish With Ease'
07-16 23:09:23.312: I/LT(7661): album  = 'ASSIMIL Turkish With Ease - L016'
07-16 23:09:23.312: I/LT(7661): ==============================================
07-16 23:09:23.312: I/LT(7661): title =  'S00-TITLE-İki eski arkadaş'
07-16 23:09:23.312: I/LT(7661): number = 'L017'
07-16 23:09:23.312: I/LT(7661): lang =   'Turkish With Ease'
07-16 23:09:23.316: I/LT(7661): album  = 'ASSIMIL Turkish With Ease - L017'
07-16 23:09:23.316: I/LT(7661): ==============================================
07-16 23:09:23.316: I/LT(7661): title =  'S00-TITLE-Telefonda'
07-16 23:09:23.316: I/LT(7661): number = 'L018'
07-16 23:09:23.316: I/LT(7661): lang =   'Turkish With Ease'
07-16 23:09:23.316: I/LT(7661): album  = 'ASSIMIL Turkish With Ease - L018'
07-16 23:09:23.316: I/LT(7661): ==============================================
07-16 23:09:23.316: I/LT(7661): title =  'S00-TITLE-Yeni zenginler'
07-16 23:09:23.316: I/LT(7661): number = 'L019'
07-16 23:09:23.316: I/LT(7661): lang =   'Turkish With Ease'
07-16 23:09:23.316: I/LT(7661): album  = 'ASSIMIL Turkish With Ease - L019'
07-16 23:09:23.316: I/LT(7661): ==============================================
07-16 23:09:23.316: I/LT(7661): title =  'S00-TITLE-Türkiye’de bir yabancı'
07-16 23:09:23.316: I/LT(7661): number = 'L020'
07-16 23:09:23.316: I/LT(7661): lang =   'Turkish With Ease'
07-16 23:09:23.316: I/LT(7661): album  = 'ASSIMIL Turkish With Ease - L020'
07-16 23:09:23.316: I/LT(7661): ==============================================
07-16 23:09:23.316: I/LT(7661): title =  'S00-TITLE-İşe geç kalan memur'
07-16 23:09:23.316: I/LT(7661): number = 'L022'
07-16 23:09:23.316: I/LT(7661): lang =   'Turkish With Ease'
07-16 23:09:23.316: I/LT(7661): album  = 'ASSIMIL Turkish With Ease - L022'
07-16 23:09:23.316: I/LT(7661): ==============================================
07-16 23:09:23.316: I/LT(7661): title =  'S00-TITLE-Arnavutköy’deki çarşı'
07-16 23:09:23.316: I/LT(7661): number = 'L023'
07-16 23:09:23.316: I/LT(7661): lang =   'Turkish With Ease'
07-16 23:09:23.316: I/LT(7661): album  = 'ASSIMIL Turkish With Ease - L023'
07-16 23:09:23.316: I/LT(7661): ==============================================
07-16 23:09:23.316: I/LT(7661): title =  'S00-TITLE-Ergin Bey Avrupa’da'
07-16 23:09:23.316: I/LT(7661): number = 'L024'
07-16 23:09:23.316: I/LT(7661): lang =   'Turkish With Ease'
07-16 23:09:23.316: I/LT(7661): album  = 'ASSIMIL Turkish With Ease - L024'
07-16 23:09:23.316: I/LT(7661): ==============================================
07-16 23:09:23.320: I/LT(7661): title =  'S00-TITLE-Doktorda'
07-16 23:09:23.324: I/LT(7661): number = 'L025'
07-16 23:09:23.324: I/LT(7661): lang =   'Turkish With Ease'
07-16 23:09:23.324: I/LT(7661): album  = 'ASSIMIL Turkish With Ease - L025'
07-16 23:09:23.324: I/LT(7661): ==============================================
07-16 23:09:23.324: I/LT(7661): title =  'S00-TITLE-Tembel öğrenci'
07-16 23:09:23.324: I/LT(7661): number = 'L026'
07-16 23:09:23.324: I/LT(7661): lang =   'Turkish With Ease'
07-16 23:09:23.324: I/LT(7661): album  = 'ASSIMIL Turkish With Ease - L026'
07-16 23:09:23.324: I/LT(7661): ==============================================
07-16 23:09:23.324: I/LT(7661): title =  'S00-TITLE-Köyde'
07-16 23:09:23.324: I/LT(7661): number = 'L027'
07-16 23:09:23.324: I/LT(7661): lang =   'Turkish With Ease'
07-16 23:09:23.324: I/LT(7661): album  = 'ASSIMIL Turkish With Ease - L027'
07-16 23:09:23.324: I/LT(7661): ==============================================
07-16 23:09:23.324: I/LT(7661): title =  'S00-TITLE-Yaz tatili'
07-16 23:09:23.324: I/LT(7661): number = 'L029'
07-16 23:09:23.324: I/LT(7661): lang =   'Turkish With Ease'
07-16 23:09:23.324: I/LT(7661): album  = 'ASSIMIL Turkish With Ease - L029'
07-16 23:09:23.324: I/LT(7661): ==============================================
07-16 23:09:23.324: I/LT(7661): title =  'S00-TITLE-Side’de'
07-16 23:09:23.324: I/LT(7661): number = 'L030'
07-16 23:09:23.324: I/LT(7661): lang =   'Turkish With Ease'
07-16 23:09:23.324: I/LT(7661): album  = 'ASSIMIL Turkish With Ease - L030'
07-16 23:09:23.324: I/LT(7661): ==============================================
07-16 23:09:23.324: I/LT(7661): title =  'S00-TITLE-Bir müdürlükte'
07-16 23:09:23.324: I/LT(7661): number = 'L031'
07-16 23:09:23.328: I/LT(7661): lang =   'Turkish With Ease'
07-16 23:09:23.328: I/LT(7661): album  = 'ASSIMIL Turkish With Ease - L031'
07-16 23:09:23.328: I/LT(7661): ==============================================
 */