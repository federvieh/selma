package com.github.federvieh.selma.assimillib;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.support.v4.app.TaskStackBuilder;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.federvieh.selma.R;

/**
 * Playbar.
 */
public class Playbar extends LinearLayout {
	//TODO: Add pause after track function (off, 1s, 2s, until user interaction)
	private TextView textViewLesson;
	private TextView textViewTrack;
	
	private ImageView imagePlay;
	private ImageView imageNextTrack;
	private ImageView imageNextLesson;	//TODO: Remove (never used that)

	private ImageView playmode;
	

	
	public Playbar(Context context) {
		super(context);
		
		init();
	}
	
	public Playbar(Context context, AttributeSet attrs) {
		super(context, attrs);

		init();
	}
	
	private void init(){
		LayoutInflater layoutInflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view=layoutInflater.inflate(R.layout.playbar,this);
		
		view.findViewById(R.id.titleTrackSection).setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				AssimilLesson al = PlaybarManager.getLessonInstance();
				if(al!=null){
					// Go to show lesson activity
					Intent intent = new Intent(getContext(), ShowLesson.class);
					intent.putExtra(AssimilOnClickListener.EXTRA_LESSON_ID, al.getHeader().getId());
					TaskStackBuilder stackBuilder = TaskStackBuilder.create(getContext());
					// Adds the back stack
					stackBuilder.addParentStack(ShowLesson.class);
					// Adds the Intent to the top of the stack
					stackBuilder.addNextIntent(intent);
					stackBuilder.startActivities();
				}
			}
		});

		textViewLesson = (TextView) view.findViewById(R.id.textViewLesson);
		textViewTrack = (TextView) view.findViewById(R.id.textViewTrack);
		playmode = (ImageView) view.findViewById(R.id.playmode);
		playmode.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				PlaybarManager.increasePlayMode();
				Editor editor = getContext().getSharedPreferences("selma", Context.MODE_PRIVATE).edit();
				//FIXME: Restore function
//				editor.putInt(LessonListFragment.PLAY_MODE, PlaybarManager.getPlayMode().ordinal());
				editor.commit();
			}
		});
		imagePlay = (ImageView) view.findViewById(R.id.imageButtonPlay);
		imagePlay.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				if(PlaybarManager.isPlaying()){
					LessonPlayer.stopPlaying(getContext());
				}
				else{
					LessonPlayer.play(PlaybarManager.getLessonInstance(), PlaybarManager.getTrackNumber(), true, v.getContext());
					if((PlaybarManager.getLessonInstance()!=null)&&(PlaybarManager.getTrackNumber()>=0)){
						OverlayManager.showPlayOverlay(getContext());
					}
				}
			}

		});
		
		imageNextTrack = (ImageView) view.findViewById(R.id.imageButtonNextTrack);
		imageNextTrack.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				if(PlaybarManager.isPlaying()){
					LessonPlayer.playNextTrack(getContext());
				}
				else{
					//Do nuffin.
				}
			}
		});
		imageNextLesson = (ImageView) view.findViewById(R.id.imageButtonNextLesson);
		imageNextLesson.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				if(PlaybarManager.isPlaying()){
					LessonPlayer.playNextLesson(getContext());
				}
				else{
					//Do nuffin.
				}
			}
		});
	}

	/**
	 * 
	 */
	public void update() {
		textViewLesson.setText(PlaybarManager.getLessonText());
		textViewTrack.setText(PlaybarManager.getTrackNumberText());
		switch (PlaybarManager.getPlayMode()){
		case ALL_LESSONS:
	    	playmode.setImageResource(R.drawable.repeat_none);
			break;
		case REPEAT_TRACK:
	    	playmode.setImageResource(R.drawable.repeat_one);
			break;
		case REPEAT_LESSON:
	    	playmode.setImageResource(R.drawable.repeat_lesson);
			break;
		case REPEAT_ALL_LESSONS:
	    	playmode.setImageResource(R.drawable.repeat_all);
			break;
		case REPEAT_ALL_STARRED:
	    	playmode.setImageResource(R.drawable.repeat_all);
			break;
		default:
			break;
		}
		if(PlaybarManager.isPlaying()){
			imagePlay.setImageResource(android.R.drawable.ic_media_pause);
		}
		else{
			imagePlay.setImageResource(android.R.drawable.ic_media_play);
		}
	}
}
