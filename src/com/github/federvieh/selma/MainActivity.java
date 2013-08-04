package com.github.federvieh.selma;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

import com.github.federvieh.selma.assimillib.AssimilDatabase;
import com.github.federvieh.selma.assimillib.LessonListActivity;
import com.github.federvieh.selma.assimillib.ListTypes;

public class MainActivity extends ActionBarActivity {
	private Button starredInclTranslate;
	private Button starredWithoutTranslate;
	private Button allInclTranslate;
	private Button allWithoutTranslate;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		starredInclTranslate = (Button) findViewById(R.id.starredInclTranslate);
		starredWithoutTranslate = (Button) findViewById(R.id.starredWithoutTranslate);
		allInclTranslate = (Button) findViewById(R.id.allInclTranslate);
		allWithoutTranslate = (Button) findViewById(R.id.allWithoutTranslate);
		
		if(!AssimilDatabase.isAllocated()){
			showWaiting(true);
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
	private void showWaiting(boolean showWait) {
		if(showWait){
			findViewById(R.id.wait_for_database_text).setVisibility(View.VISIBLE);
			findViewById(R.id.wait_for_database_progress).setVisibility(View.VISIBLE);
			findViewById(R.id.starred_textview).setVisibility(View.GONE);;
			findViewById(R.id.all_textview).setVisibility(View.GONE);;
			starredInclTranslate.setVisibility(View.GONE);
			starredWithoutTranslate.setVisibility(View.GONE);
			allInclTranslate.setVisibility(View.GONE);
			allWithoutTranslate.setVisibility(View.GONE);
		}
		else{
			findViewById(R.id.wait_for_database_text).setVisibility(View.GONE);
			findViewById(R.id.wait_for_database_progress).setVisibility(View.GONE);
			findViewById(R.id.starred_textview).setVisibility(View.VISIBLE);;
			findViewById(R.id.all_textview).setVisibility(View.VISIBLE);;
			starredInclTranslate.setVisibility(View.VISIBLE);
			starredWithoutTranslate.setVisibility(View.VISIBLE);
			allInclTranslate.setVisibility(View.VISIBLE);
			allWithoutTranslate.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	private class DatabaseInitTask extends AsyncTask<Activity, Void, Boolean> {
	    /** The system calls this to perform work in the UI thread and delivers
	      * the result from doInBackground() */
	    protected void onPostExecute(Boolean result) {
	    	showWaiting(false);
	    }

		/* (non-Javadoc)
		 * @see android.os.AsyncTask#doInBackground(java.lang.Object[])
		 */
		@Override
		protected Boolean doInBackground(Activity... arg0) {
			AssimilDatabase.getDatabase(arg0[0]);
			return Boolean.valueOf(true);
		}
	}
}
