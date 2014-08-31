/**
 * 
 */
package com.github.federvieh.selma.assimillib;

import java.io.Serializable;
import java.util.ArrayList;

import android.content.Context;
import android.util.Log;

import com.github.federvieh.selma.assimillib.dao.AssimilLessonDataSource;
import com.github.federvieh.selma.assimillib.dao.AssimilSQLiteHelper;

/**
 * @author frank
 *
 */
public class AssimilLesson implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7018204971481881787L;
	
	private ArrayList<String> allTexts = new ArrayList<String>();
	private ArrayList<String> allTextsTranslate = new ArrayList<String>();
	private ArrayList<String> allTextsTranslateSimple = new ArrayList<String>();
	private ArrayList<String> allTracknumbers = new ArrayList<String>();
	private ArrayList<String> allAudioFiles = new ArrayList<String>();
	private ArrayList<Integer> allIds = new ArrayList<Integer>();
	private int lessonTextNum = 0;

	private AssimilLessonHeader header;
	
	/**
	 * @param header
	 */
	public AssimilLesson(AssimilLessonHeader header) {
		this.header = header;
	}

	/**
	 * @return the starred
	 */
	public boolean isStarred() {
		return header.isStarred();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString(){
		return header.getLang() + header.getNumber();
	}
	
	public String getNumber(){
		return header.getNumber();
	}

	/** 
	 * @param displayMode 
	 * @return
	 */
	public String[] getTextList(DisplayMode displayMode) {
		switch (displayMode) {
		case ORIGINAL_TEXT:
			return allTexts.toArray(new String[0]);
		case LITERAL:
			return allTextsTranslateSimple.toArray(new String[0]);
		case TRANSLATION:
			return allTextsTranslate.toArray(new String[0]);
		}
		return allTexts.toArray(new String[0]);
	}
	
	public String getTextNumber(int i){
		return this.allTracknumbers.get(i);
	}

	/** Returns the list of lessons texts (i.e. not excercises).
	 * @param displayMode 
	 * @return
	 */
	public String[] getLessonList(DisplayMode displayMode) {
		ArrayList<String> baseList = allTexts;
		switch (displayMode) {
		case ORIGINAL_TEXT:
			baseList = allTexts;
			break;
		case LITERAL:
			baseList = allTextsTranslateSimple;
			break;
		case TRANSLATION:
			baseList = allTextsTranslate;
			break;
		default:
			break;
		}
		
		return (baseList.subList(0, lessonTextNum)).toArray(new String[0]);
	}

	/**
	 * @param trackNo
	 * @return
	 */
	public String getPathByTrackNo(int trackNo) {
		if(trackNo < 0){
			Log.w("LT", "Negative trackNo: "+trackNo);
		}
		else if((trackNo < lessonTextNum)||
				((PlaybarManager.isPlayingTranslate())&&(trackNo<allAudioFiles.size()))){
			return allAudioFiles.get(trackNo);
		}
		Log.d("LT", "Invalid trackNo: "+trackNo+"; lesson has "+lessonTextNum+
				" lesson files and "+allAudioFiles.size()+" total files, translate is " + (PlaybarManager.isPlayingTranslate()?"ON":"OFF"));
		throw new IllegalArgumentException("Could not find track!");
	}

	/**
	 * @param pos
	 * @param newTrans
	 * @param ctxt 
	 */
	public void setTranslateText(int pos, String newTrans, Context ctxt) {
		allTextsTranslate.remove(pos);
		allTextsTranslate.add(pos, newTrans);
		AssimilLessonDataSource ds = new AssimilLessonDataSource(ctxt);
		ds.open();
		ds.updateTranslation(allIds.get(pos), newTrans);
		ds.close();
	}

	/**
	 * @param pos
	 * @param newLit
	 * @param ctxt 
	 */
	public void setLiteralText(int pos, String newLit, Context ctxt) {
		allTextsTranslateSimple.remove(pos);
		allTextsTranslateSimple.add(pos, newLit);
		AssimilLessonDataSource ds = new AssimilLessonDataSource(ctxt);
		ds.open();
		ds.updateTranslationLit(allIds.get(pos), newLit);
		ds.close();
	}

	/**
	 * @param pos
	 * @param newText
	 * @param ctxt 
	 */
	public void setOriginalText(int pos, String newText, Context ctxt) {
		allTexts.remove(pos);
		allTexts.add(pos, newText);
		AssimilLessonDataSource ds = new AssimilLessonDataSource(ctxt);
		ds.open();
		ds.updateOriginalText(allIds.get(pos), newText);
		ds.close();
	}

	/**
	 * @return
	 */
	public AssimilLessonHeader getHeader() {
		return header;
	}

	/** Add text (and its translation and audio file) to a lesson.
	 * 
	 * @param textId ID of the text (like S01, T01)
	 * @param text The actual text
	 * @param texttrans translation of the text
	 * @param textlit the literal translation of the text
	 * @param id the database ID
	 * @param audioPath The audio file
	 */
	public void addText(String textId, String text, String texttrans,
			String textlit, int id, String audioPath) {
		Log.d("LT", "addText(" + textId + ", " + text + ", " + texttrans +
				", " + textlit + ", " + id + ", " + audioPath + ")");
		this.allTexts.add(text);
		this.allTextsTranslate.add(texttrans);
		this.allTextsTranslateSimple.add(textlit);
		this.allTracknumbers.add(textId);
		this.allAudioFiles.add(audioPath);
		this.allIds.add(id);
		if((textId.startsWith(AssimilSQLiteHelper.TITLE_PREFIX))||
				(textId.matches("S[0-9][0-9]"))){
			lessonTextNum++;
		}
	}


}
