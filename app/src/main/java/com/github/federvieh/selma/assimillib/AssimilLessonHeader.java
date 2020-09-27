/**
 *
 */
package com.github.federvieh.selma.assimillib;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import com.github.federvieh.selma.R;
import com.github.federvieh.selma.anki.AnkiInterface;
import com.github.federvieh.selma.assimillib.AssimilDatabase.LessonType;
import com.github.federvieh.selma.assimillib.dao.AssimilLessonHeaderDataSource;

/**
 * @author frank
 */
public class AssimilLessonHeader {
    private long id;
    private String lang;
    private boolean starred = false;
    private String number;
    private LessonType lessonType;

    /**
     * @param id      ID of the lesson
     * @param lang    Language of the lesson, e.g. "ASSIMIL Turkish with Ease"
     * @param number  Lesson number, e.g. "L001"
     * @param starred Is the lesson starred or not
     * @param lt      type of the lesson ({@link com.github.federvieh.selma.assimillib.AssimilDatabase.LessonType}
     */
    public AssimilLessonHeader(long id, String lang, String number, boolean starred, LessonType lt) {
        this.id = id;
        this.starred = starred;
        this.number = number;
        this.lang = lang;
        this.lessonType = lt;
    }

    public long getId() {
        return id;
    }

//	/**
//	 * @param id
//	 */
//	public void setId(long id) {
//		this.id = id;
//	}
//	/**
//	 * @param name
//	 */
//	public void setName(String name) {
//		this.name = name;
//		this.number = name.substring(name.lastIndexOf("L"));
//	}

    /**
     * @return
     */
    public boolean isStarred() {
        return starred;
    }

    /**
     * @param ctxt
     */
    public void unstar(final Context ctxt) {
        starred = false;
        AssimilLessonHeaderDataSource ds = new AssimilLessonHeaderDataSource(lessonType, ctxt);
        ds.open();
        ds.unstar(this.id);
        ds.close();
    }

    /**
     * @param ctxt
     */
    public void star(final Context ctxt) {
        starred = true;
        AssimilLessonHeaderDataSource ds = new AssimilLessonHeaderDataSource(lessonType, ctxt);
        ds.open();
        ds.star(this.id);
        ds.close();
        //handleAnki(ctxt);
    }

    private void handleAnki(final Context ctxt) {
        if (!AnkiInterface.isAnkiInstalled(ctxt)) {
            if (AnkiInterface.mayRemindInstallAnki(ctxt)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ctxt);
                builder.setTitle(ctxt.getText(R.string.did_you_know));
                builder.setMessage(ctxt.getText(R.string.no_flashcard_app));
                builder.setPositiveButton(ctxt.getText(R.string.install), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            ctxt.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.ichi2.anki")));
                        } catch (android.content.ActivityNotFoundException anfe) {
                            ctxt.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=com.ichi2.anki")));
                        }
                    }
                });
                builder.setNegativeButton(ctxt.getText(R.string.cancel), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AnkiInterface.setRemindedInstallNow(ctxt);
                    }
                });

                //FIXME: We need a "never remind me again" button!
                builder.setNeutralButton(ctxt.getText(R.string.learn_more), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://federvieh.github.io/selma")); //FIXME: Directly reference the help page: Maybe not from a button?
                        ctxt.startActivity(browserIntent);
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        } else if (AnkiInterface.isSyncEnabled(ctxt)) {
            if(AnkiInterface.isLessonInAnki(this, ctxt)){
                /* Nothing to do */
            } else {
                /*
                 * Ask user if Lesson shall be synced
                 */
                AlertDialog.Builder builder = new AlertDialog.Builder(ctxt);
                builder.setTitle(ctxt.getText(R.string.lesson_not_synced));
                builder.setMessage(ctxt.getText(R.string.ask_if_lesson_shall_be_synced));
                builder.setPositiveButton(ctxt.getText(R.string.yes), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final AssimilLesson assimilLesson = AssimilDatabase.getLesson(getId(), ctxt);
                        AnkiInterface.syncLessonWithAnki(ctxt, assimilLesson, true);
                    }
                });

                builder.setNegativeButton(ctxt.getText(R.string.no), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Nothing to do
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        } else if (AnkiInterface.mayRemindEnableAnki(ctxt)){
            AlertDialog.Builder builder = new AlertDialog.Builder(ctxt);
            builder.setTitle(ctxt.getText(R.string.did_you_know));
            builder.setMessage(ctxt.getText(R.string.flashcard_app_not_enabled));
            builder.setPositiveButton(ctxt.getText(R.string.enable), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    AnkiInterface.enableSync(ctxt);
                }
            });
            //FIXME: We need a "never remind me again" button!
            builder.setNegativeButton(ctxt.getText(R.string.cancel), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //Nothing to do
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    /**
     * @return
     */
    public String getNumber() {
        return number;
    }

    /**
     * @return
     */
    public String getLang() {
        return lang;
    }

    /**
     * @return
     */
    public LessonType getType() {
        return this.lessonType;
    }
}
