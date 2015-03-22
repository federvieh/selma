/**
 *
 */
package com.github.federvieh.selma.assimillib;

import android.content.Context;
import android.util.Log;
import com.github.federvieh.selma.assimillib.dao.AssimilLessonDataSource;
import com.github.federvieh.selma.assimillib.dao.AssimilPcSQLiteHelper;
import com.github.federvieh.selma.assimillib.dao.AssimilSQLiteHelper;
import com.github.federvieh.selma.assimillib.dao.SelmaSQLiteHelper;
import com.github.federvieh.selma.assimillib.dao.SelmaSQLiteHelper.TextType;

import java.io.*;
import java.util.ArrayList;

/**
 * @author frank
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
    private ArrayList<TextType> allTextTypes = new ArrayList<TextType>();
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
    public String toString() {
        return header.getLang() + header.getNumber();
    }

    public String getNumber() {
        return header.getNumber();
    }

    /**
     * @param displayMode
     * @return
     */
    public String[] getTextList(DisplayMode displayMode) {
        switch (displayMode) {
            case ORIGINAL_TEXT:
            case ORIGINAL_LITERAL:
            case ORIGINAL_TRANSLATION:
                return allTexts.toArray(new String[0]);
            case LITERAL:
                return allTextsTranslateSimple.toArray(new String[0]);
            case TRANSLATION:
                return allTextsTranslate.toArray(new String[0]);
        }
        return allTexts.toArray(new String[0]);
    }

    public String getTextNumber(int i) {
        return this.allTracknumbers.get(i);
    }

    /**
     * Returns the list of lessons texts (i.e. not excercises).
     *
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
        if (trackNo < 0) {
            Log.w("LT", "Negative trackNo: " + trackNo);
        } else if ((trackNo < lessonTextNum) ||
                ((LessonPlayer.isPlayingTranslate()) && (trackNo < allAudioFiles.size()))) {
            return allAudioFiles.get(trackNo);
        }
        Log.d("LT", "Invalid trackNo: " + trackNo + "; lesson has " + lessonTextNum +
                " lesson files and " + allAudioFiles.size() + " total files, translate is " + (LessonPlayer.isPlayingTranslate() ? "ON" : "OFF"));
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
        SelmaSQLiteHelper helper = null;
        switch (header.getType()) {
            case ASSIMIL_MP3_PACK:
                helper = new AssimilSQLiteHelper(ctxt);
                break;
            case ASSIMIL_PC:
                helper = new AssimilPcSQLiteHelper(ctxt);
                break;
        }
        AssimilLessonDataSource ds = new AssimilLessonDataSource(helper);
        ds.open();
        ds.updateTranslation(allIds.get(pos), newTrans);
        ds.close();
        try {
            String path = allAudioFiles.get(pos);
            StringBuffer fileNamePatt = new StringBuffer(path);
            fileNamePatt.delete(fileNamePatt.length() - 4, fileNamePatt.length());
            path = fileNamePatt + "_translate.txt";
            Log.i("LT", "Writing new translation '" + newTrans + "' to file '" +
                    path + "'");
            FileOutputStream fos = new FileOutputStream(path);
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-16");
            osw.write(newTrans);
            osw.close();
            fos.close();
        } catch (FileNotFoundException e) {
            Log.e("LT", "Could not store translation on file system!", e);
        } catch (UnsupportedEncodingException e) {
            Log.e("LT", "Could not store translation on file system!", e);
        } catch (IOException e) {
            Log.e("LT", "Could not store translation on file system!", e);
        }
    }

    /**
     * @param pos
     * @param newLit
     * @param ctxt
     */
    public void setLiteralText(int pos, String newLit, Context ctxt) {
        allTextsTranslateSimple.remove(pos);
        allTextsTranslateSimple.add(pos, newLit);
        SelmaSQLiteHelper helper = null;
        switch (header.getType()) {
            case ASSIMIL_MP3_PACK:
                helper = new AssimilSQLiteHelper(ctxt);
                break;
            case ASSIMIL_PC:
                helper = new AssimilPcSQLiteHelper(ctxt);
                break;
        }
        AssimilLessonDataSource ds = new AssimilLessonDataSource(helper);
        ds.open();
        ds.updateTranslationLit(allIds.get(pos), newLit);
        ds.close();
        try {
            String path = allAudioFiles.get(pos);
            StringBuffer fileNamePatt = new StringBuffer(path);
            fileNamePatt.delete(fileNamePatt.length() - 4, fileNamePatt.length());
            path = fileNamePatt + "_translate_verbatim.txt";
            Log.i("LT", "Writing new translation '" + newLit + "' to file '" +
                    path + "'");
            FileOutputStream fos = new FileOutputStream(path);
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-16");
            osw.write(newLit);
            osw.close();
            fos.close();
        } catch (FileNotFoundException e) {
            Log.e("LT", "Could not store translation on file system!", e);
        } catch (UnsupportedEncodingException e) {
            Log.e("LT", "Could not store translation on file system!", e);
        } catch (IOException e) {
            Log.e("LT", "Could not store translation on file system!", e);
        }
    }

    /**
     * @param pos
     * @param newText
     * @param ctxt
     */
    public void setOriginalText(int pos, String newText, Context ctxt) {
        allTexts.remove(pos);
        allTexts.add(pos, newText);
        SelmaSQLiteHelper helper = null;
        switch (header.getType()) {
            case ASSIMIL_MP3_PACK:
                helper = new AssimilSQLiteHelper(ctxt);
                break;
            case ASSIMIL_PC:
                helper = new AssimilPcSQLiteHelper(ctxt);
                break;
        }
        AssimilLessonDataSource ds = new AssimilLessonDataSource(helper);
        ds.open();
        ds.updateOriginalText(allIds.get(pos), newText);
        ds.close();
        try {
            String path = allAudioFiles.get(pos);
            StringBuffer fileNamePatt = new StringBuffer(path);
            fileNamePatt.delete(fileNamePatt.length() - 4, fileNamePatt.length());
            path = fileNamePatt + "_orig.txt";
            Log.i("LT", "Writing new translation '" + newText + "' to file '" +
                    path + "'");
            FileOutputStream fos = new FileOutputStream(path);
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-16");
            osw.write(newText);
            osw.close();
            fos.close();
        } catch (FileNotFoundException e) {
            Log.e("LT", "Could not modified text on file system!", e);
        } catch (UnsupportedEncodingException e) {
            Log.e("LT", "Could not modified text on file system!", e);
        } catch (IOException e) {
            Log.e("LT", "Could not modified text on file system!", e);
        }
    }

    /**
     * @return
     */
    public AssimilLessonHeader getHeader() {
        return header;
    }

    /**
     * Add text (and its translation and audio file) to a lesson.
     *
     * @param textId    ID of the text (like S01, T01)
     * @param text      The actual text
     * @param texttrans translation of the text
     * @param textlit   the literal translation of the text
     * @param id        the database ID
     * @param audioPath The audio file
     * @param textType
     */
    public void addText(String textId, String text, String texttrans,
                        String textlit, int id, String audioPath, TextType textType) {
//		Log.d("LT", "addText(" + textId + ", " + text + ", " + texttrans +
//				", " + textlit + ", " + id + ", " + audioPath + ")");
        this.allTexts.add(text);
        this.allTextsTranslate.add(texttrans);
        this.allTextsTranslateSimple.add(textlit);
        this.allTracknumbers.add(textId);
        this.allAudioFiles.add(audioPath);
        this.allIds.add(id);
        this.allTextTypes.add(textType);
        switch (textType) {
            case HEADING:
            case LESSONNUMBER:
            case NORMAL:
                lessonTextNum++;
//			Log.d("LT", "is lesson text");
                break;
            case TRANSLATE:
            case TRANSLATE_HEADING:
                break;
        }
    }

    /**
     * @return
     */
    public TextType[] getTextTypeList() {
        return allTextTypes.toArray(new TextType[0]);
    }


}
