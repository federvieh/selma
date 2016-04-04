package com.github.federvieh.selma.assimillib;

import android.content.Context;

public class LessonPlayer {
    public static final String PLAY_UPDATE_INTENT = "PLAY_UPDATE_INTENT";

    public static void setPlayMode(PlayMode playMode) {
        if(android.os.Build.VERSION.SDK_INT>=16){
            LessonPlayerExo.setPlayMode(playMode);
        } else {
            LessonPlayerMP.setPlayMode(playMode);
        }
    }

    public static PlayMode getPlayMode() {
        if(android.os.Build.VERSION.SDK_INT>=16){
            return LessonPlayerExo.getPlayMode();
        } else {
            return LessonPlayerMP.getPlayMode();
        }
    }

    public static AssimilLesson getLesson(Context context) {
        if(android.os.Build.VERSION.SDK_INT>=16){
            return LessonPlayerExo.getLesson(context);
        } else {
            return LessonPlayerMP.getLesson(context);
        }
    }

    public static int getTrackNumber(Context context) {
        if(android.os.Build.VERSION.SDK_INT>=16){
            return LessonPlayerExo.getTrackNumber(context);
        } else {
            return LessonPlayerMP.getTrackNumber(context);
        }
    }

    public static void increasePlayMode() {
        if(android.os.Build.VERSION.SDK_INT>=16){
            LessonPlayerExo.increasePlayMode();
        } else {
            LessonPlayerMP.increasePlayMode();
        }
    }

    public static boolean isPlaying() {
        if(android.os.Build.VERSION.SDK_INT>=16){
            return LessonPlayerExo.isPlaying();
        } else {
            return LessonPlayerMP.isPlaying();
        }
    }

    public static void stopPlaying(Context context) {
        if(android.os.Build.VERSION.SDK_INT>=16){
            LessonPlayerExo.stopPlaying(context);
        } else {
            LessonPlayerMP.stopPlaying(context);
        }
    }

    public static void play(AssimilLesson lesson, int trackNo, boolean cont, Context context) {
        if(android.os.Build.VERSION.SDK_INT>=16){
            LessonPlayerExo.play(lesson, trackNo, cont, context);
        } else {
            LessonPlayerMP.play(lesson, trackNo, cont, context);
        }
    }

    public static void playNextTrack(Context context) {
        if(android.os.Build.VERSION.SDK_INT>=16){
            LessonPlayerExo.playNextTrack(context);
        } else {
            LessonPlayerMP.playNextTrack(context);
        }
    }

    public static void playNextLesson(Context context) {
        if(android.os.Build.VERSION.SDK_INT>=16){
            LessonPlayerExo.playNextLesson(context);
        } else {
            LessonPlayerMP.playNextLesson(context);
        }
    }

    public static void setDelay(int delay) {
        if(android.os.Build.VERSION.SDK_INT>=16){
            LessonPlayerExo.setDelay(delay);
        } else {
            LessonPlayerMP.setDelay(delay);
        }
    }

    public static String getLessonTitle(Context context) {
        if(android.os.Build.VERSION.SDK_INT>=16){
            return LessonPlayerExo.getLessonTitle(context);
        } else {
            return LessonPlayerMP.getLessonTitle(context);
        }
    }

    public static String getTrackNumberText(Context context) {
        if(android.os.Build.VERSION.SDK_INT>=16){
            return LessonPlayerExo.getTrackNumberText(context);
        } else {
            return LessonPlayerMP.getTrackNumberText(context);
        }
    }

    public static void setListType(ListTypes lt) {
        if(android.os.Build.VERSION.SDK_INT>=16){
            LessonPlayerExo.setListType(lt);
        } else {
            LessonPlayerMP.setListType(lt);
        }
    }

    public static ListTypes getListType() {
        if(android.os.Build.VERSION.SDK_INT>=16){
            return LessonPlayerExo.getListType();
        } else {
            return LessonPlayerMP.getListType();
        }
    }

    public static int getPreviousTrack() {
        if(android.os.Build.VERSION.SDK_INT>=16){
            return LessonPlayerExo.getPreviousTrack();
        } else {
            return LessonPlayerMP.getPreviousTrack();
        }
    }

    public static boolean isPlayingTranslate() {
        if(android.os.Build.VERSION.SDK_INT>=16){
            return LessonPlayerExo.isPlayingTranslate();
        } else {
            return LessonPlayerMP.isPlayingTranslate();
        }
    }
}
