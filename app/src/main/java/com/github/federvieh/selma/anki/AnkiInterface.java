package com.github.federvieh.selma.anki;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import com.github.federvieh.selma.assimillib.AssimilDatabase;
import com.github.federvieh.selma.assimillib.AssimilLesson;
import com.github.federvieh.selma.assimillib.AssimilLessonHeader;
import com.github.federvieh.selma.assimillib.DisplayMode;

/**
 * Created by frank on 3/23/15.
 */
public class AnkiInterface {
    public static final String MODELNAME = "Selma app";
    public static final String FIRST_LANGUAGE_FIELD = "FirstLanguage";
    public static final String TARGET_LANGUAGE_FIELD = "TargetLanguage";
    public static final String FIRST_LANGUAGE_LITERAL_FIELD = "FirstLanguageLiteral";
    public static final String LANG_ID_FIELD = "LangID";
    public static final String TEXT_ID_FIELD = "TextID";

    private static final long CHECK_INTERVAL = 30000;
    private static final String ANKIDROID_SYNC_ENABLED = "ANKIDROID_SYNC_ENABLED";
    private static final String LAST_REMINDED_INSTALL = "ANKIDROID_LAST_REMINDED_INSTALL";
    private static final String ANKIDROID_INSTALL_REMIND_DISABLED = "ANKIDROID_INSTALL_REMIND_DISABLED";
    private static final String LAST_REMINDED_ENABLE = "LAST_REMINDED_ENABLE";
    private static final String ANKIDROID_ENABLE_REMIND_DISABLED = "ANKIDROID_ENABLE_REMIND_DISABLED";

    private static boolean ankiInstalled = false;
    private static Boolean syncEnabled = null; //FIXME: This needs to be initialized
    private static long lastCheckInstalled = 0;
    private static long lastRemindedInstall = -1;
    private static Boolean installRemindDisabled = null;
    private static long lastRemindedEnable = -1;
    private static Boolean enableRemindDisabled = null;

    /**
     * Writes a lesson to AnkiDroid. If the lesson does not yet exist, it is inserted as new notes. If the lesson
     * already exists, it is read back from AnkiDroid and updates the lesson texts in Selma.
     *
     * @param ctxt     Context is required for accessing AnkiDroid
     * @param lesson   The lesson that shall be stored in AnkiDroid
     * @param doInsert if a text does not exist in AnkiDroid, should it be inserted?
     */
    public static void syncLessonWithAnki(Context ctxt, AssimilLesson lesson, boolean doInsert) {
        //Write text to Anki
        final ContentResolver cr = ctxt.getContentResolver();
        long modelId = findModelId(cr);
        if (modelId >= 0) {
            int nbrTexts = lesson.getTextList(DisplayMode.ORIGINAL_TEXT).length;
            for (int i = 0; i < nbrTexts; i++) {
                //FIXME: Temp
//                tempSync(ctxt, lesson, i);
                String selection = AnkiInterface.TEXT_ID_FIELD + ":" + lesson.getNumber() + "_" +
                        lesson.getTextNumber(i) + " "
                        + AnkiInterface.LANG_ID_FIELD + ":\"" + lesson.getHeader().getLang() + "\"";
                Cursor currentTextNoteCursor = cr.query(FlashCardsContract.Note.CONTENT_URI, null,
                        selection, null, null);
                if (currentTextNoteCursor == null || currentTextNoteCursor.getCount() == 0) {
                    if (doInsert) {
                        insertNote(cr, modelId, lesson, i);
                    }
                } else {
                    //Note already exists
                    currentTextNoteCursor.moveToFirst();
                    long currentTextNoteId = currentTextNoteCursor.getLong(
                            currentTextNoteCursor.getColumnIndex(FlashCardsContract.Note._ID));
                    Uri currentTextNoteDataUri = FlashCardsContract.Note.CONTENT_URI.buildUpon().
                            appendPath(Long.toString(currentTextNoteId)).
                            appendPath("data").
                            build();
                    Cursor currentTextNoteDataCursor = cr.query(currentTextNoteDataUri, null, null, null, null);
                    int fieldNameColumn =
                            currentTextNoteDataCursor.getColumnIndex(FlashCardsContract.Data.Field.FIELD_NAME);
                    int fieldValueColumn =
                            currentTextNoteDataCursor.getColumnIndex(FlashCardsContract.Data.Field.FIELD_CONTENT);
                    while (currentTextNoteDataCursor.moveToNext()) {
                        if (currentTextNoteDataCursor.getString(fieldNameColumn).
                                equals(AnkiInterface.FIRST_LANGUAGE_FIELD)) {
                            String firstLanguageAnki = currentTextNoteDataCursor.getString(fieldValueColumn);
                            if(!firstLanguageAnki.equals(lesson.getTextList(DisplayMode.TRANSLATION)[i])) {
                                lesson.setTranslateText(i, firstLanguageAnki, ctxt);
                            }
                        } else if (currentTextNoteDataCursor.getString(fieldNameColumn).
                                equals(AnkiInterface.FIRST_LANGUAGE_LITERAL_FIELD)) {
                            String firstLanguageLitAnki = currentTextNoteDataCursor.getString(fieldValueColumn);
                            if (!firstLanguageLitAnki.equals(lesson.getTextList(DisplayMode.LITERAL)[i])){
                                lesson.setLiteralText(i, firstLanguageLitAnki, ctxt);
                            }
                        } else if (currentTextNoteDataCursor.getString(fieldNameColumn).
                                equals(AnkiInterface.TARGET_LANGUAGE_FIELD)) {
                            String originalAnki = currentTextNoteDataCursor.getString(fieldValueColumn);
                            if (!originalAnki.equals(lesson.getTextList(DisplayMode.ORIGINAL_TEXT)[i])){
                                lesson.setOriginalText(i, originalAnki, ctxt);
                            }
                        } else {
                            //We ignore tags or other fields
                        }
                    }
                }
            }
        } else {
            //Model does not exist, so we can't sync with Anki
        }
    }

    private static long findModelId(ContentResolver cr) {
        /*
         * First let's look for the model, in case we have to add a new note.
         */
        Cursor allModelsCursor = null;
        long modelId = -1; //TODO: This could be buffered
        try {
            String[] columns = {FlashCardsContract.Model._ID, FlashCardsContract.Model.NAME};
            // Query all available models
            allModelsCursor = cr.query(FlashCardsContract.Model.CONTENT_URI, columns, null, null, null);
            if (allModelsCursor == null) {
                //No models: It's very likely that the user does not have AnkiDroid installed
                //TODO: Maybe we should inform the user about the possibility to synchronize with AnkiDroid
            } else {
                int idColumnIndex = allModelsCursor.getColumnIndexOrThrow(FlashCardsContract.Model._ID);
                int nameColumnIndex = allModelsCursor.getColumnIndexOrThrow(FlashCardsContract.Model.NAME);
                try {
                    while (allModelsCursor.moveToNext()) {
                        String modelName = allModelsCursor.getString(nameColumnIndex);
                        if (!modelName.equals(AnkiInterface.MODELNAME)) {
                            continue;
                        }
                        //else we found the right model
                        modelId = allModelsCursor.getLong(idColumnIndex);
                    }
                } finally {
                    allModelsCursor.close();
                }
                if (modelId < 0) {
                    //FIXME: We should insert the model, for now just do nothing
                }
            }
        } finally {
            if(allModelsCursor != null) {
                allModelsCursor.close();
            }
        }
        return modelId;
    }

    private static void insertNote(ContentResolver cr, long modelId, AssimilLesson lesson, int textIdx) {
                                /* Note does not exist, insert */
        ContentValues values = new ContentValues();
        values.clear();
        values.put(FlashCardsContract.Note.MID, modelId);
        Uri newNoteUri = cr.insert(FlashCardsContract.Note.CONTENT_URI, values);
        Uri newNoteDataUri = Uri.withAppendedPath(newNoteUri, "data");

        // Now set the source language field ...
        values.clear();
        values.put(FlashCardsContract.DataColumns.MIMETYPE, FlashCardsContract.Data.Field.CONTENT_ITEM_TYPE);
        values.put(FlashCardsContract.Data.Field.FIELD_NAME, AnkiInterface.FIRST_LANGUAGE_FIELD);
        values.put(FlashCardsContract.Data.Field.FIELD_CONTENT, lesson.getTextList(DisplayMode.TRANSLATION)[textIdx]);
        cr.update(newNoteDataUri, values, null, null);
        // ... the target language field ...
        values.clear();
        values.put(FlashCardsContract.DataColumns.MIMETYPE, FlashCardsContract.Data.Field.CONTENT_ITEM_TYPE);
        values.put(FlashCardsContract.Data.Field.FIELD_NAME, AnkiInterface.TARGET_LANGUAGE_FIELD);
        values.put(FlashCardsContract.Data.Field.FIELD_CONTENT, lesson.getTextList(DisplayMode.ORIGINAL_TEXT)[textIdx]);
        cr.update(newNoteDataUri, values, null, null);
        // ... the target language literal field ...
        values.clear();
        values.put(FlashCardsContract.DataColumns.MIMETYPE, FlashCardsContract.Data.Field.CONTENT_ITEM_TYPE);
        values.put(FlashCardsContract.Data.Field.FIELD_NAME, AnkiInterface.FIRST_LANGUAGE_LITERAL_FIELD);
        values.put(FlashCardsContract.Data.Field.FIELD_CONTENT, lesson.getTextList(DisplayMode.LITERAL)[textIdx]);
        cr.update(newNoteDataUri, values, null, null);
        // ... the album ID field ...
        values.clear();
        values.put(FlashCardsContract.DataColumns.MIMETYPE, FlashCardsContract.Data.Field.CONTENT_ITEM_TYPE);
        values.put(FlashCardsContract.Data.Field.FIELD_NAME, AnkiInterface.LANG_ID_FIELD);
        values.put(FlashCardsContract.Data.Field.FIELD_CONTENT, lesson.getHeader().getLang());
        cr.update(newNoteDataUri, values, null, null);
        // ... the text ID field ...
        values.clear();
        values.put(FlashCardsContract.DataColumns.MIMETYPE, FlashCardsContract.Data.Field.CONTENT_ITEM_TYPE);
        values.put(FlashCardsContract.Data.Field.FIELD_NAME, AnkiInterface.TEXT_ID_FIELD);
        values.put(FlashCardsContract.Data.Field.FIELD_CONTENT,
                lesson.getNumber() + "_" + lesson.getTextNumber(textIdx));
        cr.update(newNoteDataUri, values, null, null);
    }

    public static boolean isAnkiInstalled(Context ctxt) {
        long now = System.currentTimeMillis();
        if(now - lastCheckInstalled > CHECK_INTERVAL) {
            ContentResolver cr = ctxt.getContentResolver();
            // Query all available models
            Cursor allModelsCursor = null;
            try {
                allModelsCursor = cr.query(FlashCardsContract.Model.CONTENT_URI, null, null, null, null);
                lastCheckInstalled = now;
                //If there are no models, it's very likely that the user does not have AnkiDroid installed
                ankiInstalled = (allModelsCursor != null) && (allModelsCursor.getCount() > 0);
            } finally {
                if(allModelsCursor != null) {
                    allModelsCursor.close();
                }
            }
        }
        return ankiInstalled;
    }

    public static boolean mayRemindInstallAnki(Context ctxt) {
        long now = System.currentTimeMillis();
        return (!((now - getLastRemindedInstall(ctxt)) < 24 * 60 * 60 * 1000) && !isInstallRemindDisabled(ctxt));
    }

    public static boolean mayRemindEnableAnki(Context ctxt) {
        long now = System.currentTimeMillis();
        return  (!((now - getLastRemindedEnable(ctxt)) < 24 * 60 * 60 * 1000) && !isEnableRemindDisabled(ctxt));

//        return (!/*lastRemindedLessThan24hAgo*/ && !/*permanently disabled*/)
    }

    /** Check if all texts of this lesson exist as notes in AnkiDroid.
     *
     * @param assimilLessonHeader
     * @param ctxt
     * @return true if all texts from this lessons exist in Anki, false otherwise
     */
    public static boolean isLessonInAnki(AssimilLessonHeader assimilLessonHeader, Context ctxt) {
        final ContentResolver cr = ctxt.getContentResolver();
        AssimilLesson lesson = AssimilDatabase.getLesson(assimilLessonHeader.getId(), ctxt);
        int nbrTexts = lesson.getTextList(DisplayMode.ORIGINAL_TEXT).length;
        for (int i = 0; i < nbrTexts; i++) {
            String selection = AnkiInterface.TEXT_ID_FIELD + ":" + lesson.getNumber() + "_" +
                    lesson.getTextNumber(i) + " "
                    + AnkiInterface.LANG_ID_FIELD + ":\"" + lesson.getHeader().getLang() + "\"";
            Cursor currentTextNoteCursor = null;
            try {
                currentTextNoteCursor = cr.query(FlashCardsContract.Note.CONTENT_URI, null,
                        selection, null, null);
                if (currentTextNoteCursor == null || currentTextNoteCursor.getCount() == 0) {
                    return false;
                }
            } finally {
                if (currentTextNoteCursor != null) {
                    currentTextNoteCursor.close();
                }
            }
        }
        //All texts seem to exist
        return true;
    }

    public static void setRemindedInstallNow(Context ctxt) {
        SharedPreferences.Editor editor = ctxt.getSharedPreferences("selma", Context.MODE_PRIVATE).edit();
        lastRemindedInstall = System.currentTimeMillis();
        editor.putLong(LAST_REMINDED_INSTALL, lastRemindedInstall);
        editor.commit();
    }

    public static long getLastRemindedInstall(Context ctxt) {
        if(lastRemindedInstall < 0) {
            lastRemindedInstall = ctxt.getSharedPreferences("selma", Context.MODE_PRIVATE)
                    .getLong(LAST_REMINDED_INSTALL, 0);
        }
        return lastRemindedInstall;
    }

    public static boolean isInstallRemindDisabled(Context ctxt) {
        if (installRemindDisabled == null){
            installRemindDisabled = new Boolean(
                    ctxt.getSharedPreferences("selma", Context.MODE_PRIVATE).getBoolean(ANKIDROID_INSTALL_REMIND_DISABLED, false));
        }
        return installRemindDisabled;
    }

    public static long getLastRemindedEnable(Context ctxt) {
        if(lastRemindedEnable < 0) {
            lastRemindedEnable = ctxt.getSharedPreferences("selma", Context.MODE_PRIVATE)
                    .getLong(LAST_REMINDED_ENABLE, 0);
        }
        return lastRemindedEnable;
    }

    public static boolean isEnableRemindDisabled(Context ctxt) {
        if (enableRemindDisabled == null){
            enableRemindDisabled = new Boolean(
                    ctxt.getSharedPreferences("selma", Context.MODE_PRIVATE).getBoolean(ANKIDROID_ENABLE_REMIND_DISABLED, false));
        }
        return enableRemindDisabled;
    }

    public static boolean isSyncEnabled(Context ctxt) {
        if (syncEnabled == null){
            syncEnabled = new Boolean(
                    ctxt.getSharedPreferences("selma", Context.MODE_PRIVATE).getBoolean(ANKIDROID_SYNC_ENABLED, false));
        }
        return syncEnabled;
    }

    public static void enableSync(Context ctxt) {
        syncEnabled = new Boolean(true);
        SharedPreferences.Editor editor = ctxt.getSharedPreferences("selma", Context.MODE_PRIVATE).edit();
        editor.putBoolean(ANKIDROID_SYNC_ENABLED, syncEnabled);
        editor.commit();
    }

    /**
     *
     * @param ctxt
     * @param language
     * @param lessonNumber
     * @param textNumber
     * @return rv[0] contains translation, rv[1] contains literal translation,
     *         rv[2] contains manually corrected original,
     *         any of which can be null if no text was found on SD card.
     */
    public static String[] getTexts(Context ctxt, String language, String lessonNumber, String textNumber) {
        final ContentResolver cr = ctxt.getContentResolver();
        String selection = AnkiInterface.TEXT_ID_FIELD + ":" + lessonNumber + "_" + textNumber + " "
                + AnkiInterface.LANG_ID_FIELD + ":\"" + language + "\"";
        Cursor noteCursor = null;
        Cursor detailsCursor = null;
        String[] rv = { null, null, null };
        try {
            noteCursor = cr.query(FlashCardsContract.Note.CONTENT_URI, null,
                    selection, null, null);
            if ((noteCursor != null)
                    && (noteCursor.getCount() > 0)
                    && noteCursor.moveToFirst()) {
                Uri detailsUri = FlashCardsContract.Note.CONTENT_URI
                        .buildUpon()
                        .appendPath(noteCursor.getString(noteCursor.getColumnIndex(FlashCardsContract.Note._ID)))
                        .appendPath("data")
                        .build();
                detailsCursor = cr.query(detailsUri, null, null, null, null);
                if((detailsCursor != null) && (detailsCursor.getCount() > 0)){
                    int mimeTypeColumn = detailsCursor.getColumnIndex(FlashCardsContract.DataColumns.MIMETYPE);
                    int fieldNameColumn = detailsCursor.getColumnIndex(FlashCardsContract.Data.Field.FIELD_NAME);
                    int fieldContentColumn = detailsCursor.getColumnIndex(FlashCardsContract.Data.Field.FIELD_CONTENT);
                    while (detailsCursor.moveToNext()){
                        String dataType = detailsCursor.getString(mimeTypeColumn);
                        if(dataType.equals(FlashCardsContract.Data.Field.CONTENT_ITEM_TYPE)){
                            String fieldName = detailsCursor.getString(fieldNameColumn);
                            if(fieldName.equals(AnkiInterface.TARGET_LANGUAGE_FIELD)){
                                rv[2] = detailsCursor.getString(fieldContentColumn);
                            } else if (fieldName.equals(AnkiInterface.FIRST_LANGUAGE_FIELD)){
                                rv[0] = detailsCursor.getString(fieldContentColumn);
                            } else if (fieldName.equals(AnkiInterface.FIRST_LANGUAGE_LITERAL_FIELD)){
                                rv[1] = detailsCursor.getString(fieldContentColumn);
                            } else {
                                /* ignore this field */
                            }
                        }
                    }
                }
            }
            else {
                /* text not found */
            }
        } finally {
            if (noteCursor != null) {
                noteCursor.close();
            }
            if (detailsCursor != null) {
                detailsCursor.close();
            }
        }
        return rv;
    }

    public static void updateText(Context ctxt, AssimilLesson lesson, String textNumber, String fieldName, String fieldValue) {
        ContentResolver cr = ctxt.getContentResolver();
        String selection = AnkiInterface.TEXT_ID_FIELD + ":" + lesson.getNumber() + "_" + textNumber + " "
                + AnkiInterface.LANG_ID_FIELD + ":\"" + lesson.getHeader().getLang() + "\"";
        Cursor currentTextNoteCursor = cr.query(FlashCardsContract.Note.CONTENT_URI, null,
                selection, null, null);
        if (currentTextNoteCursor == null || currentTextNoteCursor.getCount() == 0) {
            /* Nothing to do */
        } else {
            /* Update text */
            ContentValues values = new ContentValues();
            int idIndex = currentTextNoteCursor.getColumnIndex(FlashCardsContract.Note._ID);
            currentTextNoteCursor.moveToFirst();
            String noteId = currentTextNoteCursor.getString(idIndex);
            Uri dataUri = FlashCardsContract.Note.CONTENT_URI
                    .buildUpon()
                    .appendPath(noteId)
                    .appendPath("data")
                    .build();

            values.clear();
            values.put(FlashCardsContract.DataColumns.MIMETYPE, FlashCardsContract.Data.Field.CONTENT_ITEM_TYPE);
            values.put(FlashCardsContract.Data.Field.FIELD_NAME, fieldName);
            values.put(FlashCardsContract.Data.Field.FIELD_CONTENT, fieldValue);
            cr.update(dataUri, values, null, null);
        }
    }

    /* TODO: Delete this!*/
    private static void tempSync(Context ctxt, AssimilLesson lesson, int pos){
        ContentResolver cr = ctxt.getContentResolver();
        String targetLanguageText = lesson.getTextList(DisplayMode.ORIGINAL_TEXT)[pos];
        String textNumber = lesson.getTextNumber(pos);

        String selection = "\"" + AnkiInterface.TARGET_LANGUAGE_FIELD + ":" + targetLanguageText + "\"";
        Cursor currentTextNoteCursor = cr.query(FlashCardsContract.Note.CONTENT_URI, null,
                selection, null, null);
        if (currentTextNoteCursor == null || currentTextNoteCursor.getCount() == 0) {
            Log.w("LT_Temp", "Could not find text \"" + targetLanguageText +"\" (" + lesson.getHeader().getNumber()
                    + "_" + textNumber);
        } else {
            /* Update text */
            ContentValues values = new ContentValues();
            int idIndex = currentTextNoteCursor.getColumnIndex(FlashCardsContract.Note._ID);
            currentTextNoteCursor.moveToFirst();
            String noteId = currentTextNoteCursor.getString(idIndex);
            Uri dataUri = FlashCardsContract.Note.CONTENT_URI
                    .buildUpon()
                    .appendPath(noteId)
                    .appendPath("data")
                    .build();

            values.clear();
            values.put(FlashCardsContract.DataColumns.MIMETYPE, FlashCardsContract.Data.Field.CONTENT_ITEM_TYPE);
            values.put(FlashCardsContract.Data.Field.FIELD_NAME, AnkiInterface.LANG_ID_FIELD);
            values.put(FlashCardsContract.Data.Field.FIELD_CONTENT, lesson.getHeader().getLang());
            cr.update(dataUri, values, null, null);

            values.clear();
            values.put(FlashCardsContract.DataColumns.MIMETYPE, FlashCardsContract.Data.Field.CONTENT_ITEM_TYPE);
            values.put(FlashCardsContract.Data.Field.FIELD_NAME, AnkiInterface.TEXT_ID_FIELD);
            values.put(FlashCardsContract.Data.Field.FIELD_CONTENT, lesson.getNumber() + "_" + textNumber);
            cr.update(dataUri, values, null, null);
        }
    }
}
