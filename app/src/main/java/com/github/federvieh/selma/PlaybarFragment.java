/*
 * Copyright (C) 2016 Frank Oltmanns (frank.oltmanns+selma(at)gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.federvieh.selma;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import com.github.federvieh.selma.LessonPlayer.PlayMode;

/**
 * PlaybarFragment handles interaction with the playback function. Activities
 * that contain this fragment
 * must implement the {@link PlaybarFragment.OnPlaybarInteractionListener}
 * interface to handle interaction events. Use the
 * {@link PlaybarFragment#newInstance} factory method to create an instance of
 * this fragment.
 */
public class PlaybarFragment extends Fragment {
    private TextView textViewLesson;
    private TextView textViewTrack;

    private ImageView imagePlay;
    private ImageView imageNextTrack;
    private ImageView imageNextLesson;

    private ImageView playmode;

    public static final String PLAY_MODE = "PLAY_MODE";

    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Update the content (play button, current track a.s.o.)
            update();
        }
    };

    //FIXME: Implement listener
//    private OnPlaybarInteractionListener mListener;
//FIXME: Restore functionality
//    private TextView textViewCurrentPause;
    private View pauseModeLayout;
    private View titleTrackSection;

    /**
     * Use this factory method to create a new instance of this fragment using
     * the provided parameters.
     *
     * @return A new instance of fragment PlaybarFragment.
     */
    public static PlaybarFragment newInstance() {
        PlaybarFragment fragment = new PlaybarFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public PlaybarFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
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
        boolean rv = false;
        switch (item.getItemId()) {
            case R.id.action_repeat_all:
                LessonPlayer.setPlayMode(PlayMode.REPEAT_ALL_LESSONS);
                rv = true;
                break;
            case R.id.action_repeat_lesson:
                LessonPlayer.setPlayMode(PlayMode.REPEAT_LESSON);
                rv = true;
                break;
            case R.id.action_repeat_none:
                LessonPlayer.setPlayMode(PlayMode.ALL_LESSONS);
                rv = true;
                break;
            case R.id.action_repeat_track:
                LessonPlayer.setPlayMode(PlayMode.REPEAT_TRACK);
                rv = true;
                break;
            default:
                return super.onContextItemSelected(item);
        }
        updateView();
        Editor editor = getActivity().getSharedPreferences("selma", Context.MODE_PRIVATE).edit();
        editor.putInt(PLAY_MODE, LessonPlayer.getPlayMode().ordinal());
        editor.commit();
        return rv;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.playbar, container, false);
        titleTrackSection = view.findViewById(R.id.titleTrackSection);
        titleTrackSection.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                Lesson lesson = LessonPlayer.getLesson(getActivity());
                if (lesson != null) {
                    //Show current lesson
//                    mListener.onLessonClicked(lesson.getId(), LessonPlayer.getTrackNumber(getActivity()));
                }
            }
        });

        textViewLesson = (TextView) view.findViewById(R.id.textViewLesson);
        textViewTrack = (TextView) view.findViewById(R.id.textViewTrack);
        playmode = (ImageView) view.findViewById(R.id.playmode);
        playmode.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                LessonPlayer.increasePlayMode();
                update();
                Editor editor = getActivity().getSharedPreferences("selma", Context.MODE_PRIVATE).edit();
                editor.putInt(PLAY_MODE, LessonPlayer.getPlayMode().ordinal());
                editor.commit();
            }
        });
        imagePlay = (ImageView) view.findViewById(R.id.imageButtonPlay);
        imagePlay.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                if (LessonPlayer.isPlaying()) {
                    LessonPlayer.stopPlaying(getActivity());
                } else {
                    if ((LessonPlayer.getLesson(getActivity()) == null) || (LessonPlayer.getTrackNumber(getActivity()) < 0)) {
                        try {
                            //FIXME: Play first visible lesson
//                            long lessonId = AssimilDatabase.getCurrentLessons().get(0).getId();
//                            LessonPlayer.play(AssimilDatabase.getLesson(lessonId, v.getContext()), 0, true, v.getContext());
                        } catch (IndexOutOfBoundsException e) {
                            //Empty list (e.g. no starred lessons) -> ignore
                        }
                    } else {
                        LessonPlayer.play(LessonPlayer.getLesson(getActivity()), LessonPlayer.getTrackNumber(getActivity()), true, v.getContext());
                    }
                    //FIXME: Overlay?
//                    if ((LessonPlayer.getLesson(getActivity()) != null) && (LessonPlayer.getTrackNumber(getActivity()) >= 0)) {
//                        OverlayManager.showPlayOverlay(getActivity());
//                    }
                }
            }

        });

        imageNextTrack = (ImageView) view.findViewById(R.id.imageButtonNextTrack);
        imageNextTrack.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                if (LessonPlayer.isPlaying()) {
                    LessonPlayer.playNextTrack(getActivity());
                } else {
                    //Do nuffin.
                }
            }
        });
        imageNextLesson = (ImageView) view.findViewById(R.id.imageButtonNextLesson);
        imageNextLesson.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                if (LessonPlayer.isPlaying()) {
                    LessonPlayer.playNextLesson(getActivity());
                } else {
                    //Do nuffin.
                }
            }
        });
        pauseModeLayout = view.findViewById(R.id.pauseMode_layout);
        ImageView pauseMode = (ImageView) view.findViewById(R.id.pauseMode);
        Log.d(this.getClass().getSimpleName(), "Looking for ID " + R.id.pauseMode_layout);
//        textViewCurrentPause = (TextView) view.findViewById(R.id.textViewCurrentDelay);
//        textViewCurrentPause.setMinWidth(textViewCurrentPause.getWidth());
        pauseMode.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                showPauseDialog();
            }
        });
        //FIXME: Restore functionality
//        SeekBar seekBar = (SeekBar) view.findViewById(R.id.sbDelay);
//        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            @Override
//            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
////                textViewCurrentPause.setText(i + "%");
//                LessonPlayer.setDelay(i);
//            }
//
//            @Override
//            public void onStartTrackingTouch(SeekBar seekBar) {
//
//            }
//
//            @Override
//            public void onStopTrackingTouch(SeekBar seekBar) {
//
//            }
//        });
//        seekBar.setProgress(10);
//        seekBar.setProgress(0);
        return view;
    }

    private void showPauseDialog() {
        FragmentManager manager = getFragmentManager();
        Fragment frag = manager.findFragmentByTag("fragment_adjust_pause");
        if (frag != null) {
            manager.beginTransaction().remove(frag).commit();
        }
        AdjustPauseDialogFragment dialog = new AdjustPauseDialogFragment();
        dialog.show(manager,"fragment_adjust_pause");
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
//            mListener = (OnPlaybarInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnPlaybarInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
//        mListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(messageReceiver,
                new IntentFilter(LessonPlayer.PLAY_UPDATE_INTENT));
//        textViewCurrentPause.setMinWidth(pauseModeLayout.findViewById(R.id.textViewMaxDelay).getWidth());
        //FIXME: Restore functionality
//        pauseModeLayout.invalidate();
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
        textViewLesson.setText(LessonPlayer.getLessonTitle(getActivity()));
        textViewTrack.setText(LessonPlayer.getTrackNumberText(getActivity()));
        switch (LessonPlayer.getPlayMode()) {
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
        if (LessonPlayer.isPlaying()) {
            imagePlay.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            imagePlay.setImageResource(android.R.drawable.ic_media_play);
        }
    }

//////////////////////////////////////////////////////////////////////////////
/// FROM PlaybarManager //////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////

    /**
     *
     */
    private void update() {
        updateView();
    }


    public void setCurrent() {
        update();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated to
     * the activity and potentially other fragments contained in that activity.
     * <p/>
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
