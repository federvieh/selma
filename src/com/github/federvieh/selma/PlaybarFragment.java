package com.github.federvieh.selma;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.federvieh.selma.assimillib.AssimilDatabase;
import com.github.federvieh.selma.assimillib.AssimilLesson;
import com.github.federvieh.selma.assimillib.LessonPlayer;
import com.github.federvieh.selma.assimillib.LessonPlayer.PlayMode;
import com.github.federvieh.selma.assimillib.OverlayManager;

/**
 * PlaybarFragment handles interaction with the playback function. Activities
 * that contain this fragment
 * must implement the {@link PlaybarFragment.OnPlaybarInteractionListener}
 * interface to handle interaction events. Use the
 * {@link PlaybarFragment#newInstance} factory method to create an instance of
 * this fragment.
 * 
 */
public class PlaybarFragment extends Fragment {
	//TODO: Add pause after track function (off, 1s, 2s, until user interaction)
	private TextView textViewLesson;
	private TextView textViewTrack;
	
	private ImageView imagePlay;
	private ImageView imageNextTrack;
	private ImageView imageNextLesson;

	private ImageView playmode;

	// TODO: Rename parameter arguments, choose names that match
	// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
	private static final String ARG_PARAM1 = "param1";
	private static final String ARG_PARAM2 = "param2";

	// TODO: Rename and change types of parameters
	private String mParam1;
	private String mParam2;

	private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
		  @Override
		  public void onReceive(Context context, Intent intent) {
			  //Update the content (play button, current track a.s.o.)
			  update();
		  }
		};

	private OnPlaybarInteractionListener mListener;

	/**
	 * Use this factory method to create a new instance of this fragment using
	 * the provided parameters.
	 * 
	 * @param param1
	 *            Parameter 1.
	 * @param param2
	 *            Parameter 2.
	 * @return A new instance of fragment PlaybarFragment.
	 */
	// TODO: Rename and change types and number of parameters
	public static PlaybarFragment newInstance(String param1, String param2) {
		PlaybarFragment fragment = new PlaybarFragment();
		Bundle args = new Bundle();
		args.putString(ARG_PARAM1, param1);
		args.putString(ARG_PARAM2, param2);
		fragment.setArguments(args);
		return fragment;
	}

	public PlaybarFragment() {
		// Required empty public constructor
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {
			mParam1 = getArguments().getString(ARG_PARAM1);
			mParam2 = getArguments().getString(ARG_PARAM2);
		}
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState){
		super.onActivityCreated(savedInstanceState);
		registerForContextMenu(playmode);
		updateView();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater mi = new MenuInflater(v.getContext());
		mi.inflate(R.menu.repeat, menu);
	}

	@Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
		case R.id.action_repeat_all:
			LessonPlayer.setPlayMode(PlayMode.REPEAT_ALL_LESSONS);
			updateView();
			return true;
		case R.id.action_repeat_lesson:
			LessonPlayer.setPlayMode(PlayMode.REPEAT_LESSON);
			updateView();
			return true;
		case R.id.action_repeat_none:
			LessonPlayer.setPlayMode(PlayMode.ALL_LESSONS);
			updateView();
			return true;
		case R.id.action_repeat_track:
			LessonPlayer.setPlayMode(PlayMode.REPEAT_TRACK);
			updateView();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.playbar, container, false);
		view.findViewById(R.id.titleTrackSection).setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				AssimilLesson al = LessonPlayer.getLesson();
				if(al!=null){
					//Show current lesson
					mListener.onLessonClicked(al.getHeader().getId(), LessonPlayer.getTrackNumber());
				}
			}
		});

		textViewLesson = (TextView) view.findViewById(R.id.textViewLesson);
		textViewTrack = (TextView) view.findViewById(R.id.textViewTrack);
		playmode = (ImageView) view.findViewById(R.id.playmode);
		playmode.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				increasePlayMode();
				Editor editor = getActivity().getSharedPreferences("selma", Context.MODE_PRIVATE).edit();
				//FIXME: Restore functionality
//				editor.putInt(LessonListFragment.PLAY_MODE, PlaybarManager.getPlayMode().ordinal());
				editor.commit();
			}
		});
		imagePlay = (ImageView) view.findViewById(R.id.imageButtonPlay);
		imagePlay.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				if(LessonPlayer.isPlaying()){
					LessonPlayer.stopPlaying(getActivity());
				}
				else{
					if((LessonPlayer.getLesson()==null)||(LessonPlayer.getTrackNumber()<0)){
						try{
							long lessonId = AssimilDatabase.getCurrentLessons().get(0).getId();
							LessonPlayer.play(AssimilDatabase.getLesson(lessonId, v.getContext()),0,true,v.getContext());
						}
						catch(IndexOutOfBoundsException e){
							//Empty list (e.g. no starred lessons) -> ignore
						}
					}
					else{
						LessonPlayer.play(LessonPlayer.getLesson(), LessonPlayer.getTrackNumber(), true, v.getContext());
					}
					if((LessonPlayer.getLesson()!=null)&&(LessonPlayer.getTrackNumber()>=0)){
						OverlayManager.showPlayOverlay(getActivity());
					}
				}
			}

		});
		
		imageNextTrack = (ImageView) view.findViewById(R.id.imageButtonNextTrack);
		imageNextTrack.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				if(LessonPlayer.isPlaying()){
					LessonPlayer.playNextTrack(getActivity());
				}
				else{
					//Do nuffin.
				}
			}
		});
		imageNextLesson = (ImageView) view.findViewById(R.id.imageButtonNextLesson);
		imageNextLesson.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				if(LessonPlayer.isPlaying()){
					LessonPlayer.playNextLesson(getActivity());
				}
				else{
					//Do nuffin.
				}
			}
		});
		return view;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (OnPlaybarInteractionListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnPlaybarInteractionListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}

	@Override
	public void onResume() {
	  super.onResume();

	  // Register mMessageReceiver to receive messages.
	  LocalBroadcastManager.getInstance(getActivity()).registerReceiver(messageReceiver,
	      new IntentFilter(LessonPlayer.PLAY_UPDATE_INTENT));
	}

	@Override
	public void onPause() {
	  // Unregister since the activity is not visible
	  LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(messageReceiver);
	  super.onPause();
	} 
	
	/**
	 * 
	 */
	private void updateView() {
		textViewLesson.setText(LessonPlayer.getLessonTitle());
		textViewTrack.setText(LessonPlayer.getTrackNumberText());
		switch (LessonPlayer.getPlayMode()){
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
//		case REPEAT_ALL_STARRED:
//	    	playmode.setImageResource(R.drawable.repeat_all);
//			break;
		default:
			break;
		}
		if(LessonPlayer.isPlaying()){
			imagePlay.setImageResource(android.R.drawable.ic_media_pause);
		}
		else{
			imagePlay.setImageResource(android.R.drawable.ic_media_play);
		}
	}

//////////////////////////////////////////////////////////////////////////////
/// FROM PlaybarManager //////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////

	public void increasePlayMode() {
		LessonPlayer.increasePlayMode();
		update();
	}

	/**
	 * 
	 */
	private void update() {
		updateView();
	}

	
	public void  setCurrent(){
		update();
	}
	
	/**
	 * This interface must be implemented by activities that contain this
	 * fragment to allow an interaction in this fragment to be communicated to
	 * the activity and potentially other fragments contained in that activity.
	 * <p>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html"
	 * >Communicating with Other Fragments</a> for more information.
	 */
	public interface OnPlaybarInteractionListener {
		/**
		 * @param id
		 * @param trackNumber
		 */
		public void onLessonClicked(long id, int trackNumber);

	}

}
