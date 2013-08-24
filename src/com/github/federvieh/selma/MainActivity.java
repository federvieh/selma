package com.github.federvieh.selma;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.federvieh.selma.assimillib.AssimilDatabase;
import com.github.federvieh.selma.assimillib.LessonListActivity;
import com.github.federvieh.selma.assimillib.ListTypes;

public class MainActivity extends ActionBarActivity {
	private Button starredInclTranslate;
	private Button starredWithoutTranslate;
	private Button allInclTranslate;
	private Button allWithoutTranslate;
	
	enum ActivityState{
		DATABASE_LOADING,
		DATABASE_EMPTY,
		READY_FOR_PLAYBACK
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		starredInclTranslate = (Button) findViewById(R.id.starredInclTranslate);
		starredWithoutTranslate = (Button) findViewById(R.id.starredWithoutTranslate);
		allInclTranslate = (Button) findViewById(R.id.allInclTranslate);
		allWithoutTranslate = (Button) findViewById(R.id.allWithoutTranslate);
		
		if(!AssimilDatabase.isAllocated()){
			showWaiting(ActivityState.DATABASE_LOADING);
			new DatabaseInitTask().execute(this);
		}
		final Context context = this;

		starredInclTranslate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Go to show lesson activity
		    	Intent intent = new Intent(context, LessonListActivity.class);
		    	intent.putExtra(LessonListActivity.EXTRA_LIST_TYPE, ListTypes.LIST_TYPE_STARRED_TRANSLATE);
		    	context.startActivity(intent);
			}
		});

		starredWithoutTranslate.setOnClickListener(new View.OnClickListener() {	
			@Override
			public void onClick(View v) {
				// Go to show lesson activity
		    	Intent intent = new Intent(context, LessonListActivity.class);
		    	intent.putExtra(LessonListActivity.EXTRA_LIST_TYPE, ListTypes.LIST_TYPE_STARRED_NO_TRANSLATE);
		    	context.startActivity(intent);
			}
		});

		allInclTranslate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Go to show lesson activity
		    	Intent intent = new Intent(context, LessonListActivity.class);
		    	intent.putExtra(LessonListActivity.EXTRA_LIST_TYPE, ListTypes.LIST_TYPE_ALL_TRANSLATE);
		    	context.startActivity(intent);
			}
		});

		allWithoutTranslate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Go to show lesson activity
		    	Intent intent = new Intent(context, LessonListActivity.class);
		    	intent.putExtra(LessonListActivity.EXTRA_LIST_TYPE, ListTypes.LIST_TYPE_ALL_NO_TRANSLATE);
		    	context.startActivity(intent);
			}
		});
	}

	/**
	 * @param b
	 */
	private void showWaiting(ActivityState databaseLoading) {
		if(databaseLoading==ActivityState.DATABASE_LOADING){
			findViewById(R.id.wait_for_database_text).setVisibility(View.VISIBLE);
			findViewById(R.id.wait_for_database_progress).setVisibility(View.VISIBLE);
			findViewById(R.id.starred_textview).setVisibility(View.GONE);;
			findViewById(R.id.all_textview).setVisibility(View.GONE);;
			starredInclTranslate.setVisibility(View.GONE);
			starredWithoutTranslate.setVisibility(View.GONE);
			allInclTranslate.setVisibility(View.GONE);
			allWithoutTranslate.setVisibility(View.GONE);
		}
		else if (databaseLoading==ActivityState.READY_FOR_PLAYBACK){
			findViewById(R.id.wait_for_database_text).setVisibility(View.GONE);
			findViewById(R.id.wait_for_database_progress).setVisibility(View.GONE);
			findViewById(R.id.starred_textview).setVisibility(View.VISIBLE);;
			findViewById(R.id.all_textview).setVisibility(View.VISIBLE);;
			starredInclTranslate.setVisibility(View.VISIBLE);
			starredWithoutTranslate.setVisibility(View.VISIBLE);
			allInclTranslate.setVisibility(View.VISIBLE);
			allWithoutTranslate.setVisibility(View.VISIBLE);
		}
		else{
			//database is empty
			findViewById(R.id.wait_for_database_text).setVisibility(View.VISIBLE);
			((TextView)findViewById(R.id.wait_for_database_text)).setText(R.string.no_content_found);
			findViewById(R.id.wait_for_database_progress).setVisibility(View.GONE);
			findViewById(R.id.starred_textview).setVisibility(View.GONE);;
			findViewById(R.id.all_textview).setVisibility(View.GONE);;
			starredInclTranslate.setVisibility(View.GONE);
			starredWithoutTranslate.setVisibility(View.GONE);
			allInclTranslate.setVisibility(View.GONE);
			allWithoutTranslate.setVisibility(View.GONE);

		}
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
			AssimilDatabase ad = AssimilDatabase.getDatabase(arg0[0]);
			if(ad.size()>0){
				return ActivityState.READY_FOR_PLAYBACK;
			}
			return ActivityState.DATABASE_EMPTY;
		}
	}
}
