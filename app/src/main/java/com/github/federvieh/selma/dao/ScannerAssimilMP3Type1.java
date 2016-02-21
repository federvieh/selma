package com.github.federvieh.selma.dao;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import com.github.federvieh.selma.SelmaContentProvider;
import com.github.federvieh.selma.SelmaSQLiteHelper2;
import com.github.federvieh.selma.SelmaSQLiteHelper2.TextType;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Used for scanning files in the format of the Assimil MP3 courses. Examples that are known to
 * work with this format:
 *  - Turkish with Ease
 *  - New Russian with Ease
 *  - Hebrew with Ease
 *  - Greek with Ease
 *  - Spanish
 */
public class ScannerAssimilMP3Type1 extends AsyncTask {
    /* Prefixes as used in the Assimil lesson MP3 files.
     *
     */
    public static final String TITLE_PREFIX = "S00-TITLE-";
    private static final int PREFIX_LENGTH = "S01-".length();
    private static ScannerAssimilMP3Type1 instance = null;
    private static Object lock = new Object();
    private boolean running = false;


    public static void startScanning(Context ctxt) {
        if(instance == null){
            synchronized (lock){
                if(instance == null){
                    instance = new ScannerAssimilMP3Type1(ctxt.getContentResolver());
                }
            }
        }
        if(instance.isRunning()) {
            //Nothing to do
            Log.i(instance.getClass().getSimpleName(), "Scan already running.");
        } else {
            instance.running = true;
            instance.execute();
            Log.i(instance.getClass().getSimpleName(), "Started scan");
        }
    }

    private final ContentResolver contentResolver;

    private ScannerAssimilMP3Type1(ContentResolver cr){
        this.contentResolver = cr;
    }

    @Override
    protected void onPostExecute(Object o) {
        Log.i(instance.getClass().getSimpleName(), "Scan finished");
        synchronized (lock) {
            running = false;
            instance = null;
        }
        //FIXME: Inform caller that scanning has finished
    }

    @Override
    protected Object doInBackground(Object[]objects){
            Log.d(this.getClass().getSimpleName(), "Start scanning");

        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {android.provider.MediaStore.Audio.Media.TITLE,
                android.provider.MediaStore.Audio.Media.ALBUM,
                android.provider.MediaStore.Audio.Media._ID,
                android.provider.MediaStore.Audio.Media.ARTIST
        };
        String findLessons = "(" + android.provider.MediaStore.Audio.Media.ALBUM + " LIKE '%ASSIMIL%' AND " +
                android.provider.MediaStore.Audio.Media.TITLE + " LIKE 'S00-TITLE-%')";

        Cursor cursor = contentResolver.query(uri, projection, findLessons, null, android.provider.MediaStore.Audio.Media.ALBUM);
        if (cursor == null) {
            Log.i(getClass().getSimpleName(), "no cursor");
            //TODO: query failed
            return false;
        } else if (!cursor.moveToFirst()) {
            Log.i(getClass().getSimpleName(), "empty cursor");
            // TODO: no media on device
            return false;
        } else {
            int titleColumn = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE);
            int albumColumn = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ALBUM);
            int artistColumn = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ARTIST);
            do{
                String fullTitle = cursor.getString(titleColumn);
                String fullAlbum = cursor.getString(albumColumn);
                String language = cursor.getString(artistColumn);//fullAlbum.substring(fullAlbum.indexOf(" ")+1,fullAlbum.indexOf(" - L"));
                Log.i("LT", "title =  '" + fullTitle + "'");
                Log.i("LT", "lang =   '" + language + "'");
                Log.i("LT", "album  = '" + fullAlbum + "'");
                Log.i("LT", "==============================================");
                    Pattern patternAssimilLesson = Pattern.compile("L[0-9]+");
                    Matcher m = patternAssimilLesson.matcher(fullAlbum);
                    String number = null; //fullAlbum.substring(fullAlbum.lastIndexOf("L"));
                    if(m.find()){
                        number = m.group();
                    }
                    else{
                        Log.w("LT", "Could not find lesson number");
                        continue;
                    }
                    createIfNotExists(number, language, fullAlbum);
            } while (cursor.moveToNext());
            cursor.close();
            return true;
        }
    }

    /**
     * This method creates a new text entry in the selma database, if it doesn't already exist.
     * If the entry does exist, it is updated. TODO: Is that so?
     * @param number The lesson number used in Selma's database
     * @param language The language name used in Selma's database
     * @param fullAlbum The album name used in the Android media store (each lesson is in it's own
     *                  album)
     */
    public void createIfNotExists(String number, String language, String fullAlbum) {
        /* First find all texts for this album ( i.e. lesson).
         */
        Uri mediaUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {android.provider.MediaStore.Audio.Media.TITLE,
                android.provider.MediaStore.Audio.Media.ALBUM,
                android.provider.MediaStore.Audio.Media._ID,
                android.provider.MediaStore.Audio.Media.DATA
        };
        String findLessonTexts = android.provider.MediaStore.Audio.Media.ALBUM + " = '" + fullAlbum + "' AND (" +
                android.provider.MediaStore.Audio.Media.TITLE + " LIKE 'N%-%' OR " + //NUMBER
                android.provider.MediaStore.Audio.Media.TITLE + " LIKE 'S%' OR " +   //Text
                android.provider.MediaStore.Audio.Media.TITLE + " LIKE 'T%')";      //Translate
        Cursor cursor = contentResolver.query(mediaUri, projection, findLessonTexts, null, android.provider.MediaStore.Audio.Media.TITLE);

        /* Check that we found a least one entry. Note that this should never fail, because why
         * should this function have been called if there are no texts?
         */
        if (cursor == null) {
            Log.e(this.getClass().getSimpleName(), "Query for album returned null");
            return;
        } else if (!cursor.moveToFirst()) {
            Log.e(this.getClass().getSimpleName(), "Query for album returned empty cursor");
            return;
        } else {
            /* Check if the lesson already exists in Selma's database.
             */
            Uri selmaUri = SelmaContentProvider.CONTENT_URI_LESSONS;
            String[] columns = {SelmaSQLiteHelper2.TABLE_LESSONS_ID};
            Cursor cursorAlbum = contentResolver.query(
                    selmaUri,
                    columns,
                    SelmaSQLiteHelper2.TABLE_LESSONS_LESSONNAME + " = '" + number + "'" +
                            " AND " + SelmaSQLiteHelper2.TABLE_LESSONS_COURSENAME + "= '" + language + "'",
                    null,
                    null);

            if (!cursorAlbum.moveToFirst()) {
                /* The lesson does not yet exist in the database, i.e. we need to create a new entry
                 * for this album.
                 */
                Log.d("LT", "Creating new lesson for " + fullAlbum);
                ContentValues valuesLessonTable = new ContentValues();
                valuesLessonTable.put(SelmaSQLiteHelper2.TABLE_LESSONS_COURSENAME, language);
                valuesLessonTable.put(SelmaSQLiteHelper2.TABLE_LESSONS_LESSONNAME, number);
                valuesLessonTable.put(SelmaSQLiteHelper2.TABLE_LESSONS_STARRED, 0);
                Uri newLessonUri = contentResolver.insert(selmaUri,valuesLessonTable);
                cursorAlbum = contentResolver.query(
                        newLessonUri,
                        columns,
                        null,
                        null,
                        null);
            }
            if (!cursorAlbum.moveToFirst()) {
                //Still no result!
                Log.wtf("LT", "Creating new lesson header was unsuccessful!");
                return;
            }
            if (cursorAlbum.getCount() != 1) {
                //Hmm... also not expected
                Log.wtf("LT", "Query for lesson header returned " + cursorAlbum.getCount() + ", but expected is 1!");
                return;
            }
            long albumId = cursorAlbum.getLong(cursorAlbum.getColumnIndex(SelmaSQLiteHelper2.TABLE_LESSONS_ID));
            cursorAlbum.close();

            /* Now go through each entry for this album (i.e. lesson) and see if the lesson is
             * already in Selma's database.
             */
            int titleColumn = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media._ID);
            int dataColumn = cursor.getColumnIndex(android.provider.MediaStore.Audio.Media.DATA);
            //title = S00-TITLE-İki genç
            //album = ASSIMIL Turkish With Ease - L001
            do {
                String fullTitle = cursor.getString(titleColumn);
                String id = cursor.getString(idColumn);
                String path = cursor.getString(dataColumn);
//      		Log.i("LT", "Path: "+path);
                String text = null;
                String textNumber = null;
                TextType textType;
                if (fullTitle.startsWith(TITLE_PREFIX)) {
                    text = fullTitle.substring(TITLE_PREFIX.length());
                    textNumber = fullTitle.substring(0, PREFIX_LENGTH - 1);
                    textType = TextType.HEADING;
                } else if (fullTitle.matches("S[0-9][0-9]-.*")) {
                    text = fullTitle.substring(PREFIX_LENGTH);
                    textNumber = fullTitle.substring(0, PREFIX_LENGTH - 1);
                    textType = TextType.NORMAL;
                } else if (fullTitle.matches("T[0-9][0-9]-.*")) {
                    text = fullTitle.substring(PREFIX_LENGTH);
                    textNumber = fullTitle.substring(0, PREFIX_LENGTH - 1);
                    if (textNumber.equals("T00")) {
                        textType = TextType.TRANSLATE_HEADING;
                    } else {
                        textType = TextType.TRANSLATE;
                    }
                } else if (fullTitle.matches("N[0-9]*-.*")) {
                    text = fullTitle.substring(fullTitle.indexOf("-") + 1);
                    textNumber = fullTitle.substring(0, fullTitle.indexOf("-"));
                    textType = TextType.LESSONNUMBER;
                } else {
                    //Something's wrong!
                    Log.w("LT", "Unknown file!");
                    Log.w("LT", "text = '" + fullTitle + "'");
                    Log.w("LT", "id =   '" + id + "'");
                    Log.w("LT", "==============================================");
                    continue;
                }
                //Find the lesson in the lesson table
                String[] columnsTexts = {SelmaSQLiteHelper2.TABLE_LESSONTEXTS_TEXTID};
                Uri textUri = SelmaContentProvider.CONTENT_URI_LESSONS
                        .buildUpon()
                        .appendPath(Long.toString(albumId))
                        .appendPath(SelmaContentProvider.BASE_PATH_LESSON_TEXT)
                        .build();
                Cursor cursorLessontext = contentResolver.query(
                        textUri,
                        columnsTexts,
                        SelmaSQLiteHelper2.TABLE_LESSONTEXTS_TEXTID + " = ?",
                        new String[] {textNumber},
                        null);
                if (cursorLessontext.moveToFirst()) {
                    //Found a result, i.e. we don't need to create a new entry for this text
                    Log.d("LT", "Text " + textNumber + " for lesson \"" + fullAlbum + "\" already exists. Skipping...");
                    //FIXME: We should probably update the entry
                } else {
                    String[] translations = findTexts(path);
                    String[] ankitexts = {null, null, null};//AnkiInterface.getTexts(getContext(), language, number, textNumber);
                    ContentValues values = new ContentValues();
                    values.put(SelmaSQLiteHelper2.TABLE_LESSONTEXTS_LESSONID, albumId);
                    values.put(SelmaSQLiteHelper2.TABLE_LESSONTEXTS_TEXTID, textNumber);
                    values.put(SelmaSQLiteHelper2.TABLE_LESSONTEXTS_TEXTTYPE, textType.ordinal());
                    if (ankitexts[2] != null) {
                        text = ankitexts[2];
                    } else if (translations[2] != null) {
                        text = translations[2];
                    }
                    values.put(SelmaSQLiteHelper2.TABLE_LESSONTEXTS_TEXT, text);
                    if (ankitexts[0] != null) {
                        values.put(SelmaSQLiteHelper2.TABLE_LESSONTEXTS_TEXTTRANS, ankitexts[0]);
                    } else if (translations[0] != null) {
                        values.put(SelmaSQLiteHelper2.TABLE_LESSONTEXTS_TEXTTRANS, translations[0]);
                    }
                    if (ankitexts[1] != null) {
                        values.put(SelmaSQLiteHelper2.TABLE_LESSONTEXTS_TEXTLIT, ankitexts[1]);
                    } else if (translations[1] != null) {
                        values.put(SelmaSQLiteHelper2.TABLE_LESSONTEXTS_TEXTLIT, translations[1]);
                    }
                    values.put(SelmaSQLiteHelper2.TABLE_LESSONTEXTS_AUDIOFILEPATH, path);
                    contentResolver.insert(SelmaContentProvider.CONTENT_URI_LESSON_CONTENT, values);
                }
                cursorLessontext.close();
            } while (cursor.moveToNext());
            cursor.close();
        }
    }

    /** Find translation on SD card of the given MP3 file.
     * @param pathStr path to the MP3 file
     * @return rv[0] contains translation, rv[1] contains literal translation,
     *         rv[2] contains manually corrected original,
     *         any of which can be null if no text was found on SD card.
     */
    protected static String[] findTexts(String pathStr) {
        StringBuffer fileNamePatt = new StringBuffer(pathStr);
        fileNamePatt.delete(fileNamePatt.length()-4, fileNamePatt.length());
        Log.d("LT", "fileNamePatt: " + fileNamePatt.toString());

        String translatedText = getFileContent(fileNamePatt+"_translate.txt");
        String translatedTextVerbatim = getFileContent(fileNamePatt + "_translate_verbatim.txt");
        String originalText = getFileContent(fileNamePatt+"_orig.txt");

        String[] rv = {translatedText, translatedTextVerbatim, originalText};
        return rv;
    }

    private static String getFileContent(/*String directory, String filename,*/ String path) {
        InputStream is;
        try {
            is = new FileInputStream(path);
            InputStreamReader isr = new InputStreamReader(is,"UTF-16");
            BufferedReader br = new BufferedReader(isr);
            String nextLine;
            StringBuilder sb = new StringBuilder();
            while((nextLine=br.readLine())!=null){
                sb.append(nextLine);
            }
            br.close();
            isr.close();
            is.close();
            if(sb.length()>0){
                return sb.toString();
            }
        } catch (FileNotFoundException e1) {
			/* Silently fail, no translation was manually entered for this text */
            //Log.w("LT", e1);
        }
        catch (IOException e) {
            Log.w("LT", e);
        }
        return null;
    }

    public boolean isRunning() {
        return running;
    }
}
