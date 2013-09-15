package com.github.federvieh.selma.assimillib;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SpinnerAdapter;

import com.github.federvieh.selma.R;

public class ShowLesson extends ActionBarActivity implements OnItemClickListener{
	private AssimilLesson lesson = null;
	private static ListTypes lt = PlaybarManager.getListType();
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		Log.d("LT", "ShowLesson.onCreate()");

		setContentView(R.layout.activity_show_lesson);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
        Intent intend = getIntent();
		int lessonTemp = intend.getIntExtra(AssimilOnClickListener.EXTRA_LESSON_POS,0);
		lt=PlaybarManager.getListType();
		Log.d("LT", "ShowLesson.onCreate(); lt="+lt);
		
		lesson = AssimilDatabase.getDatabase(null).get(lessonTemp);
		this.setTitle(lesson.getNumber());

		SpinnerAdapter mSpinnerAdapter = ArrayAdapter.createFromResource(this, R.array.exercise_option_list,
				android.R.layout.simple_spinner_dropdown_item);
		ActionBar.OnNavigationListener mOnNavigationListener = new ActionBar.OnNavigationListener() {
			@Override
			public boolean onNavigationItemSelected(int position, long itemId) {
				switch(lt){
				case LIST_TYPE_ALL_NO_TRANSLATE:
				case LIST_TYPE_ALL_TRANSLATE:
					if(position==0){
						lt = ListTypes.LIST_TYPE_ALL_TRANSLATE;
					}
					else{
						lt = ListTypes.LIST_TYPE_ALL_NO_TRANSLATE;
					}
					break;
				case LIST_TYPE_STARRED_NO_TRANSLATE:
				case LIST_TYPE_STARRED_TRANSLATE:
					if(position==0){
						lt = ListTypes.LIST_TYPE_STARRED_TRANSLATE;
					}
					else{
						lt = ListTypes.LIST_TYPE_STARRED_NO_TRANSLATE;
					}
					break;
				}
				PlaybarManager.setListType(lt);
				updateListType();
				return true;
			}
		};
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		getSupportActionBar().setListNavigationCallbacks(mSpinnerAdapter, mOnNavigationListener);
		int navItem = 0;
		switch(lt){
		case LIST_TYPE_ALL_TRANSLATE:
		case LIST_TYPE_STARRED_TRANSLATE:
			navItem = 0;
			break;
		case LIST_TYPE_STARRED_NO_TRANSLATE:
		case LIST_TYPE_ALL_NO_TRANSLATE:
			navItem = 1;
			break;
		}
		getSupportActionBar().setSelectedNavigationItem(navItem);
    }

    private void updateListType(){
		PlaybarManager.setListType(lt);
		Log.d("LT", "ShowLesson.updateListType(); lt="+lt);
		AssimilShowLessonListAdapter assimilShowLessonListAdapter;
		assimilShowLessonListAdapter = new AssimilShowLessonListAdapter(this, lesson, lt);
		ListView listView = (ListView) findViewById(R.id.listViewLessons);
		listView.setAdapter(assimilShowLessonListAdapter);
		listView.setOnItemClickListener(this);

		Playbar playbar = (Playbar) findViewById(R.id.playbar1);
		PlaybarManager.setPbInstance(playbar);
		PlaybarManager.setLessonInstance(this);
    }


	@Override
	protected void onPause(){
		super.onPause();
		Log.d("LT", "ShowLesson.onPause()");
	}

	@Override
	protected void onResume(){
		super.onResume();
		Log.d("LT", "ShowLesson.onResume()");
		Playbar playbar = (Playbar) findViewById(R.id.playbar1);
		PlaybarManager.setPbInstance(playbar);
	}
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

	/* (non-Javadoc)
	 * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
	 */
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//    	String item = (String) parent.getItemAtPosition(position);
//    	Toast.makeText(this, item + " selected", Toast.LENGTH_LONG).show();
    	LessonPlayer.play(lesson, position, false);
	}


	/**
	 * @return
	 */
	public AssimilLesson getLesson() {
		return lesson;
	}


	/**
	 * @param trackNumber
	 */
	public void highlight(int trackNumber) {
		try{
			ListView listView = (ListView) findViewById(R.id.listViewLessons);
			listView.invalidateViews();
		}
		catch(Exception e){}
	}
}
