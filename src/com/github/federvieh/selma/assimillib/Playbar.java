package com.github.federvieh.selma.assimillib;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.TaskStackBuilder;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.federvieh.selma.R;

/**
 * Playbar.
 */
public class Playbar extends LinearLayout {
	//TODO: Click on left part (titleTrackSection) should bring the user to the lesson/track
	//TODO: Add pause after track function (off, 1s, 2s, until user interaction)
	private Context context;
	private TextView textViewLesson;
	private TextView textViewTrack;
	
	private ImageView imagePlay;
	private ImageView imageNextTrack;
	private ImageView imageNextLesson;	//TODO: Remove (never used that)

	private ImageView playmode;
	

	
	public Playbar(Context context) {
		super(context);
		
		this.context = context;
		init();
	}
	
	public Playbar(Context context, AttributeSet attrs) {
		super(context, attrs);

		this.context = context;
		init();
	}
	
	private void init(){
		LayoutInflater layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view=layoutInflater.inflate(R.layout.playbar,this);
		
		view.findViewById(R.id.titleTrackSection).setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				AssimilLesson al = PlaybarManager.getLessonInstance();
				if(al!=null){
					// Go to show lesson activity
					Intent intent = new Intent(context, ShowLesson.class);
					intent.putExtra(AssimilOnClickListener.EXTRA_LESSON_POS, AssimilDatabase.getDatabase(null).indexOf(al));
//					context.startActivity(intent);
					TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
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
			}
		});
		imagePlay = (ImageView) view.findViewById(R.id.imageButtonPlay);
		imagePlay.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				if(PlaybarManager.isPlaying()){
					LessonPlayer.stopPlaying(context);
				}
				else{
					LessonPlayer.play(PlaybarManager.getLessonInstance(), PlaybarManager.getTrackNumber(), true);
				}
			}
		});
		
		imageNextTrack = (ImageView) view.findViewById(R.id.imageButtonNextTrack);
		imageNextTrack.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				if(PlaybarManager.isPlaying()){
					LessonPlayer.playNextTrack(context);
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
					LessonPlayer.playNextLesson(context);
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
//		case SINGLE_TRACK:
//	    	Toast.makeText(this.context, "Playing single track", Toast.LENGTH_SHORT).show();
//			break;
//		case SINGLE_LESSON:
//	    	Toast.makeText(this.context, "Playing single lesson", Toast.LENGTH_SHORT).show();
//			break;
//		case ALL_LESSONS:
//	    	playmode.setImageResource(R.drawable.repeat_none);
//			break;
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
	    	playmode.setImageResource(R.drawable.repeat_star);
			break;
		default:
			break;
		}
		if (PlaybarManager.isPlaymodeUpdated()) {
			//show text
			switch (PlaybarManager.getPlayMode()){
//			case SINGLE_TRACK:
//		    	Toast.makeText(this.context, "Playing single track", Toast.LENGTH_SHORT).show();
//				break;
//			case SINGLE_LESSON:
//		    	Toast.makeText(this.context, "Playing single lesson", Toast.LENGTH_SHORT).show();
//				break;
//			case ALL_LESSONS:
//		    	Toast.makeText(this.context, "Playing all lessons", Toast.LENGTH_SHORT).show();
//				break;
			case REPEAT_TRACK:
		    	Toast.makeText(this.context, "Repeating single track", Toast.LENGTH_SHORT).show();
				break;
			case REPEAT_LESSON:
		    	Toast.makeText(this.context, "Repeating single lesson", Toast.LENGTH_SHORT).show();
				break;
			case REPEAT_ALL_LESSONS:
		    	Toast.makeText(this.context, "Repeating all lessons", Toast.LENGTH_SHORT).show();
				break;
			case REPEAT_ALL_STARRED:
		    	Toast.makeText(this.context, "Repeating starred lessons", Toast.LENGTH_SHORT).show();
				break;
			default:
				break;
			}

		}
		if(PlaybarManager.isPlaying()){
			imagePlay.setImageResource(android.R.drawable.ic_media_pause);
		}
		else{
			imagePlay.setImageResource(android.R.drawable.ic_media_play);
		}
	}
}
