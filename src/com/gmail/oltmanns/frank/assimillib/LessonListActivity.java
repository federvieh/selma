package com.gmail.oltmanns.frank.assimillib;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;

import com.gmail.oltmanns.frank.languagetrainer.R;

public class LessonListActivity extends ActionBarActivity {

	public static final String EXTRA_LIST_TYPE = "com.gmail.oltmanns.frank.assimillib.EXTRA_LIST_TYPE";
	
	private static ListTypes lt;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lesson_list);
		/* get the content for main screen */
		ListTypes listtype = (ListTypes) getIntent().getSerializableExtra(EXTRA_LIST_TYPE);
		//Might be null when using back button
		if(listtype!=null){
			lt=listtype;
		}
		PlaybarManager.setListType(lt);
		AssimilDatabase ad = AssimilDatabase.getDatabase(this);
		switch(lt){
		case LIST_TYPE_ALL_NO_TRANSLATE:
			getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.holo_purple)));
			this.setTitle(getResources().getText(R.string.play_all_lessons_short)+" "+getResources().getText(R.string.starred_without_translate));
			break;
		case LIST_TYPE_ALL_TRANSLATE:
			getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.holo_blue_dark)));
			this.setTitle(getResources().getText(R.string.play_all_lessons_short)+" "+getResources().getText(R.string.starred_with_translate));
			break;
		case LIST_TYPE_STARRED_NO_TRANSLATE:
			ad = AssimilDatabase.getStarredOnly(this);
			if((PlaybarManager.getLessonInstance()!=null)&&(!PlaybarManager.getLessonInstance().isStarred())){
				LessonPlayer.stopPlaying(this);
				PlaybarManager.setCurrent(null, -1);
			}
			getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.holo_orange_dark)));
			this.setTitle(getResources().getText(R.string.play_starred_lessons_short)+" "+getResources().getText(R.string.starred_without_translate));
			break;
		case LIST_TYPE_STARRED_TRANSLATE:
			if((PlaybarManager.getLessonInstance()!=null)&&(!PlaybarManager.getLessonInstance().isStarred())){
				LessonPlayer.stopPlaying(this);
				PlaybarManager.setCurrent(null, -1);
			}
			ad = AssimilDatabase.getStarredOnly(this);
			getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.holo_green_dark)));
			this.setTitle(getResources().getText(R.string.play_starred_lessons_short)+" "+getResources().getText(R.string.starred_with_translate));
			break;			
		}
		/* show the content */
		AssimilLessonListAdapter assimilLessonListAdapter;
		assimilLessonListAdapter = new AssimilLessonListAdapter(this, ad, lt);
		ListView listView = (ListView) findViewById(R.id.listView1);
		if(ad.isEmpty()){
			TextView tv = new TextView(this);
			//LayoutParams lp = new AbsListView.LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
			tv.setPadding(10, 10, 10, 10);
			tv.setTextSize(32);
			//tv.setLayoutParams(lp);
			tv.setText(getResources().getText(R.string.warning_no_starred));
			listView.addHeaderView(tv);
		}
		listView.setAdapter(assimilLessonListAdapter);
		Playbar playbar = (Playbar) findViewById(R.id.playbar1);
		playbar.update();
		PlaybarManager.setPbInstance(playbar);
	}

	@Override
	protected void onPause(){
		super.onPause();
		Log.d("LT", "MainActivity.onPause()");
		AssimilDatabase.getDatabase(this).commit();
	}

	@Override
	protected void onResume(){
		super.onResume();
		Log.d("LT", "MainActivity.onResume()");
		Playbar playbar = (Playbar) findViewById(R.id.playbar1);
		PlaybarManager.setPbInstance(playbar);
	}

	@Override
	protected void onDestroy(){
		super.onDestroy();
		Log.i("LT", "being destroyed");
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