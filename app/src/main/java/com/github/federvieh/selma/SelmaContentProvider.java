package com.github.federvieh.selma;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;

public class SelmaContentProvider extends ContentProvider {
    //TODO: Some of this could be moved to a contract class
    private SelmaSQLiteHelper2 databaseHelper;

    // used for the UriMacher
    private static final int COURSES = 10;  //Note that courses don't have an ID
    private static final int LESSONS = 110;
    private static final int LESSON_ID = 120; //Will probably not be used
    private static final int LESSONS_TEXT = 210; //Will be used for reading complete lesson
    private static final int LESSON_TEXT_ID = 220; //Will be used for reading/writing a single text
    private static final int LESSON_LESSON_TEXT = 230; //Will be used for reading/writing a single text in a lesson

    private static final String AUTHORITY = "com.github.federvieh.selma";

    private static final String BASE_PATH_COURSES = "courses";
    public static final Uri CONTENT_URI_COURSES = Uri.parse("content://" + AUTHORITY
            + "/" + BASE_PATH_COURSES);

    private static final String BASE_PATH_LESSONS = "lessons";
    public static final Uri CONTENT_URI_LESSONS = Uri.parse("content://" + AUTHORITY
            + "/" + BASE_PATH_LESSONS);

    public static final String BASE_PATH_LESSON_TEXT = "lesson_text";
    public static final Uri CONTENT_URI_LESSON_CONTENT = Uri.parse("content://" + AUTHORITY
            + "/" + BASE_PATH_LESSON_TEXT);

    public static final String CONTENT_TYPE_COURSES = ContentResolver.CURSOR_DIR_BASE_TYPE
            + "/courses";
    public static final String CONTENT_ITEM_TYPE_COURSES = ContentResolver.CURSOR_ITEM_BASE_TYPE
            + "/course";
    public static final String CONTENT_TYPE_LESSONS = ContentResolver.CURSOR_DIR_BASE_TYPE
            + "/lessons";
    public static final String CONTENT_ITEM_TYPE_LESSONS = ContentResolver.CURSOR_ITEM_BASE_TYPE
            + "/lesson";
    public static final String CONTENT_TYPE_LESSON_TEXTS = ContentResolver.CURSOR_DIR_BASE_TYPE
            + "/lesson_texts";
    public static final String CONTENT_ITEM_TYPE_LESSON_TEXTS = ContentResolver.CURSOR_ITEM_BASE_TYPE
            + "/lesson_text";

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_COURSES, COURSES);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_LESSONS, LESSONS);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_LESSONS + "/#", LESSON_ID);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_LESSON_TEXT, LESSONS_TEXT);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_LESSON_TEXT + "/#", LESSON_TEXT_ID);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH_LESSONS + "/#/" + BASE_PATH_LESSON_TEXT, LESSON_LESSON_TEXT);
        //content://com.github.federvieh.selma/lessons/1/lesson_text
    }

    public SelmaContentProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Implement this to handle requests to delete one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public String getType(Uri uri) {
        // TODO: Implement this to handle requests for the MIME type of the data
        // at the given URI.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        long newId;

        int uriType = sURIMatcher.match(uri);
        switch (uriType) {
            case COURSES:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
                //break;
            case LESSONS:
                newId = db.insert(
                        SelmaSQLiteHelper2.TABLE_LESSONS,
                        null,
                        values);
                //Notify about change for both the lesson and the courses URI
                getContext().getContentResolver().notifyChange(uri, null);
                getContext().getContentResolver().notifyChange(CONTENT_URI_COURSES, null);//TODO: Room for improvement: Only notify if there actually was a change in the courses
                return uri
                        .buildUpon()
                        .appendPath(Long.toString(newId))
                        .build();
            //break;
            case LESSON_ID:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
                //break;
            case LESSONS_TEXT:
                newId = db.insert(
                        SelmaSQLiteHelper2.TABLE_LESSONTEXTS,
                        null,
                        values);
//                Log.d("LT", "Notifying about URI "+uri);
                getContext().getContentResolver().notifyChange(uri, null);
                return uri
                        .buildUpon()
                        .appendPath(Long.toString(newId))
                        .build();
            //break;
            case LESSON_TEXT_ID:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
                //break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public boolean onCreate() {
        databaseHelper = new SelmaSQLiteHelper2(
                getContext(),        // the application context
                null                 // uses the default SQLite cursor
        );

        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        String id;
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        int uriType = sURIMatcher.match(uri);
        switch (uriType) {
            case COURSES:
                return db.query(true,
                        SelmaSQLiteHelper2.TABLE_LESSONS,
                        projection,
                        selection,
                        selectionArgs,
                        null, //no grouping ...
                        null, //..ergo no filter for grouping
                        sortOrder,
                        null  //no limit
                );
            case LESSONS:
                return db.query(SelmaSQLiteHelper2.TABLE_LESSONS,
                        projection,
                        selection,
                        selectionArgs,
                        null, //no grouping ...
                        null, //..ergo no filter for grouping
                        sortOrder);
            //break;
            case LESSON_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    return db.query(SelmaSQLiteHelper2.TABLE_LESSONS,
                            projection,
                            SelmaSQLiteHelper2.TABLE_LESSONS_ID + "=" + id,
                            null,
                            null, //no grouping ...
                            null, //..ergo no filter for grouping
                            sortOrder);
                } else {
                    return db.query(SelmaSQLiteHelper2.TABLE_LESSONS,
                            projection,
                            SelmaSQLiteHelper2.TABLE_LESSONS_ID + "=" + id
                                    + " and (" + selection + " )",
                            selectionArgs,
                            null, //no grouping ...
                            null, //..ergo no filter for grouping
                            sortOrder);
                }
                //break;
            case LESSONS_TEXT:
                return db.query(SelmaSQLiteHelper2.TABLE_LESSONTEXTS,
                        projection,
                        selection,
                        selectionArgs,
                        null, //no grouping ...
                        null, //..ergo no filter for grouping
                        sortOrder);
            //break;
            case LESSON_TEXT_ID:
                id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    return db.query(SelmaSQLiteHelper2.TABLE_LESSONTEXTS,
                            projection,
                            SelmaSQLiteHelper2.TABLE_LESSONTEXTS_ID + "=" + id,
                            null,
                            null, //no grouping ...
                            null, //..ergo no filter for grouping
                            sortOrder);
                } else {
                    return db.query(SelmaSQLiteHelper2.TABLE_LESSONTEXTS,
                            projection,
                            SelmaSQLiteHelper2.TABLE_LESSONTEXTS_ID + "=" + id
                                    + " and (" + selection + " )",
                            selectionArgs,
                            null, //no grouping ...
                            null, //..ergo no filter for grouping
                            sortOrder);
                }
                //break;
            case LESSON_LESSON_TEXT:
                String lesson = uri.getPathSegments().get(1);
                if (TextUtils.isEmpty(selection)) {
                    return db.query(SelmaSQLiteHelper2.TABLE_LESSONTEXTS,
                            projection,
                            SelmaSQLiteHelper2.TABLE_LESSONTEXTS_LESSONID + "=" + lesson,
                            null,
                            null, //no grouping ...
                            null, //..ergo no filter for grouping
                            sortOrder);
                } else {
                    String completeSelection = SelmaSQLiteHelper2.TABLE_LESSONTEXTS_LESSONID + "=" + lesson
                            + " and (" + selection + " )";
                    return db.query(SelmaSQLiteHelper2.TABLE_LESSONTEXTS,
                            projection,
                            completeSelection,
                            selectionArgs,
                            null, //no grouping ...
                            null, //..ergo no filter for grouping
                            sortOrder);
                }
                //break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        // TODO: Implement this to handle requests to update one or more rows.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
