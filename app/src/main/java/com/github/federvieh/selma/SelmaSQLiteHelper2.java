package com.github.federvieh.selma;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by frank on 10/22/15.
 */
public class SelmaSQLiteHelper2 extends SQLiteOpenHelper {
    public enum TextType {
        NORMAL,
        HEADING,
        LESSONNUMBER,
        TRANSLATE,
        TRANSLATE_HEADING
    };

    /* Table "lessons"
     * | _id | coursename        | lessonname | starred |
     * +-----+-------------------+------------+---------+
     * |auto | Turkish with Ease | L001       | 1       |
     * |auto | Turkish with Ease | L002       | 1       |
     * |auto | Spanish with Ease | L001       | 0       |
     */
    public static final String TABLE_LESSONS = "lessons";
    public static final String TABLE_LESSONS_ID = "_id";
    public static final String TABLE_LESSONS_COURSENAME = "coursename";
    public static final String TABLE_LESSONS_LESSONNAME = "lessonname";
    public static final String TABLE_LESSONS_STARRED = "starred";
    private static final String ASSIMIL_CREATE_TABLE_LESSONS =
            "create table " + TABLE_LESSONS + " (" +
                    TABLE_LESSONS_ID + " integer primary key autoincrement, " +
                    TABLE_LESSONS_COURSENAME + " text not null, " +
                    TABLE_LESSONS_LESSONNAME + " int not null, " +
                    TABLE_LESSONS_STARRED + " int not null" +
                    ");";

    private static final String ASSIMIL_DROP_TABLE_LESSONS =
            "drop TABLE if exists " + TABLE_LESSONS + ";";

    /* Table "lessontexts"
     * | _id | lessonid              | textid | text           | text_trans   | text_lit     | audiofile        | texttype |
     * +-----+-----------------------+--------+----------------+--------------+--------------+------------------+----------+
     * |auto | ref to _id of lessons | S01    | Merhaba Mehmet | Hello Mehmet | Hello Mehmet | /path/to/S01.mp3 | 0        |
     * |auto | ref to _id of lessons | S02    | Nasilsin?      | How are you? | How-you-are  | /path/to/S02.mp3 | 0        |
     */
    public static final String TABLE_LESSONTEXTS = "lessontexts";
    public static final String TABLE_LESSONTEXTS_ID = "_id";
    public static final String TABLE_LESSONTEXTS_LESSONID = "lessonid";
    public static final String TABLE_LESSONTEXTS_TEXTID = "textid";
    public static final String TABLE_LESSONTEXTS_TEXT = "text";
    public static final String TABLE_LESSONTEXTS_TEXTTRANS = "text_trans";
    public static final String TABLE_LESSONTEXTS_TEXTLIT = "text_lit";
    public static final String TABLE_LESSONTEXTS_AUDIOFILEPATH = "audiofile";
    public static final String TABLE_LESSONTEXTS_TEXTTYPE = "texttype";
    private static final String ASSIMIL_CREATE_TABLE_LESSONTEXTS =
            "create table " + TABLE_LESSONTEXTS + " (" +
                    TABLE_LESSONTEXTS_ID + " integer primary key autoincrement, " +
                    TABLE_LESSONTEXTS_LESSONID + " integer not null, " + //TODO: reference table "lessons"
                    TABLE_LESSONTEXTS_TEXTID + " text not null," +
//			TABLE_LESSONTEXTS_LANG + " integer not null," + //0: orig, 1: translate
                    TABLE_LESSONTEXTS_TEXT + " text not null," + //The actual text
                    TABLE_LESSONTEXTS_TEXTTRANS + " text," + //The translated text
                    TABLE_LESSONTEXTS_TEXTLIT + " text," + //The literal translation of the text
                    TABLE_LESSONTEXTS_AUDIOFILEPATH + " text not null," + //The actual text
                    TABLE_LESSONTEXTS_TEXTTYPE + " integer not null" + //see enum TextType
                    ");";

    private static final String ASSIMIL_INDEX_LESSONID_LESSONTEXTS =
            "create index idx_lessonid on " + TABLE_LESSONTEXTS + "(" + TABLE_LESSONTEXTS_LESSONID + ")";

    private static final String ASSIMIL_DROP_TABLE_LESSONTEXTS =
            "drop TABLE if exists " + TABLE_LESSONTEXTS + ";";

    private static final String ASSIMIL_DATABASE_NAME = "assimil.db";
    private static final int SELMA_DATABASE_VERSION = 3;

    /**
     * @param context {@see SQLiteOpenHelper}
     * @param factory {@see SQLiteOpenHelper}
     */
    public SelmaSQLiteHelper2(Context context, SQLiteDatabase.CursorFactory factory) {
        super(context, ASSIMIL_DATABASE_NAME, factory, SELMA_DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(ASSIMIL_CREATE_TABLE_LESSONS);
        sqLiteDatabase.execSQL(ASSIMIL_CREATE_TABLE_LESSONTEXTS);
        sqLiteDatabase.execSQL(ASSIMIL_INDEX_LESSONID_LESSONTEXTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if((oldVersion == 1)&&(newVersion == 2)){
            //Change from Version 1 to 2: Added index
            db.execSQL(ASSIMIL_INDEX_LESSONID_LESSONTEXTS);
        }
        else if (newVersion == 3){
            Log.w(this.getClass().getName(), "There is not reasonable way to upgrade from version " +
                    oldVersion + " to " + newVersion + ". Dropping database content.");
            db.execSQL(ASSIMIL_DROP_TABLE_LESSONTEXTS);
            db.execSQL(ASSIMIL_DROP_TABLE_LESSONS);
            onCreate(db);
        }
        else{
            Log.w(this.getClass().getName(), "Unknown version upgrade. Dropping database content in order" +
                    " to upgrade from version " + oldVersion + " to " + newVersion);
            db.execSQL(ASSIMIL_DROP_TABLE_LESSONTEXTS);
            db.execSQL(ASSIMIL_DROP_TABLE_LESSONS);
            onCreate(db);
        }
    }
}
