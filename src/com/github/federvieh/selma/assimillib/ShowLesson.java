package com.github.federvieh.selma.assimillib;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SpinnerAdapter;

import com.github.federvieh.selma.R;
import com.github.federvieh.selma.assimillib.LessonPlayer.PlayMode;

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
		Editor editor = getSharedPreferences("selma", Context.MODE_PRIVATE).edit();
		editor.putInt(LessonListActivity.LIST_MODE, lt.ordinal());
		editor.commit();
		Log.d("LT", "ShowLesson.updateListType(); lt="+lt.ordinal());
		AssimilShowLessonListAdapter assimilShowLessonListAdapter;
		assimilShowLessonListAdapter = new AssimilShowLessonListAdapter(this, lesson, lt);
		ListView listView = (ListView) findViewById(R.id.listViewLessons);
		listView.setAdapter(assimilShowLessonListAdapter);
		listView.setOnItemClickListener(this);

		Playbar playbar = (Playbar) findViewById(R.id.playbar1);
		PlaybarManager.setPbInstance(playbar);
		PlaybarManager.setLessonInstance(this);
		PlaybarManager.setPbInstance(playbar);
		registerForContextMenu(playbar.findViewById(R.id.playmode));
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

    @Override
	public void onCreateContextMenu(ContextMenu menu, View v,
	                                ContextMenuInfo menuInfo) {
	    super.onCreateContextMenu(menu, v, menuInfo);
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.repeat, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_repeat_all:
			if((lt==ListTypes.LIST_TYPE_ALL_NO_TRANSLATE)||(lt==ListTypes.LIST_TYPE_ALL_TRANSLATE)){
				PlaybarManager.setPlayMode(PlayMode.REPEAT_ALL_LESSONS);
			}
			else{
				PlaybarManager.setPlayMode(PlayMode.REPEAT_ALL_STARRED);
			}
			return true;
		case R.id.action_repeat_lesson:
			PlaybarManager.setPlayMode(PlayMode.REPEAT_LESSON);
			return true;
		case R.id.action_repeat_none:
			PlaybarManager.setPlayMode(PlayMode.ALL_LESSONS);
			return true;
		case R.id.action_repeat_track:
			PlaybarManager.setPlayMode(PlayMode.REPEAT_TRACK);
			return true;
		default:
			return super.onContextItemSelected(item);
		}
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
