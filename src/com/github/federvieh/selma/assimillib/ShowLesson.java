package com.github.federvieh.selma.assimillib;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.github.federvieh.selma.R;

public class ShowLesson extends ActionBarActivity implements OnItemClickListener{
	private AssimilLesson lesson = null;
	private static ListTypes lt;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		Log.d("LT", "ShowLesson.onCreate()");

		setContentView(R.layout.activity_show_lesson);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
        Intent intend = getIntent();
		int lessonTemp = intend.getIntExtra(AssimilOnClickListener.EXTRA_LESSON_POS,0);
		ListTypes listtype = (ListTypes)intend.getSerializableExtra(LessonListActivity.EXTRA_LIST_TYPE);
		if(listtype!=null){
			lt=listtype;
		}
		Log.d("LT", "ShowLesson.onCreate(); lt="+lt);
		
		lesson = AssimilDatabase.getDatabase(null).get(lessonTemp);
		switch(lt){
		case LIST_TYPE_ALL_NO_TRANSLATE:
			getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.holo_purple)));
			this.setTitle(lesson.getNumber()+": "+getResources().getText(R.string.play_all_lessons_short)+" "+getResources().getText(R.string.starred_without_translate));
			break;
		case LIST_TYPE_ALL_TRANSLATE:
			getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.holo_blue_dark)));
			this.setTitle(lesson.getNumber()+": "+getResources().getText(R.string.play_all_lessons_short)+" "+getResources().getText(R.string.starred_with_translate));
			break;
		case LIST_TYPE_STARRED_NO_TRANSLATE:
			getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.holo_orange_dark)));
			this.setTitle(lesson.getNumber()+": "+getResources().getText(R.string.play_starred_lessons_short)+" "+getResources().getText(R.string.starred_without_translate));
			break;
		case LIST_TYPE_STARRED_TRANSLATE:
			getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.holo_green_dark)));
			this.setTitle(lesson.getNumber()+": "+getResources().getText(R.string.play_starred_lessons_short)+" "+getResources().getText(R.string.starred_with_translate));
			break;			
		}
/*		String[] values;
		if((lt == ListTypes.LIST_TYPE_ALL_NO_TRANSLATE)||(lt ==ListTypes.LIST_TYPE_STARRED_NO_TRANSLATE)){
			values = lesson.getLessonList();
		}
		else{
			values = lesson.getTextList();
		}

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, values);
		ListView listView = (ListView) findViewById(R.id.listViewLessons);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(this);
		*/
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
/*			for(int i=0;i<listView.getChildCount();i++){
				TextView tv = (TextView)((LinearLayout)listView.getChildAt(i)).getChildAt(0);
				if(tv.getText().toString().equals(lesson.getTextList()[trackNumber])){
					tv.setTypeface(null,Typeface.BOLD);					
				}
				else{
					tv.setTypeface(null,Typeface.NORMAL);					
				}
				
			}*/
		}
		catch(Exception e){}
	}
}
