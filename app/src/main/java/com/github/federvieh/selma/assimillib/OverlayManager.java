/**
 * 
 */
package com.github.federvieh.selma.assimillib;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.federvieh.selma.R;

/**
 * @author frank
 *
 */
public class OverlayManager {
	private static final String OVERLAY_PLAYSHOWN = "com.github.federvieh.selma.assimillib.OVERLAY_PLAYSHOWN";
	private static final String OVERLAY_HINTDISPLAYED = "com.github.federvieh.selma.assimillib.OVERLAY_HINTDISPLAYED";
	private static final String OVERLAY_LESSONCONTENTSHOWN = "com.github.federvieh.selma.assimillib.OVERLAY_LESSONCONTENTSHOWN";
	private static boolean initialized = false;
	private static void init(Context context){
		if(!initialized){
			SharedPreferences settings = context.getSharedPreferences("selma", Context.MODE_PRIVATE);
			playShown = settings.getBoolean(OVERLAY_PLAYSHOWN, false);
			try{
				lessonContentShown = settings.getInt(OVERLAY_LESSONCONTENTSHOWN, 0);
			}
			catch(Exception e){
				//Loading may fail, because this was a boolean value in earlier versions.
				lessonContentShown = 0;
			}
			hintDisplayed = settings.getInt(OVERLAY_HINTDISPLAYED, 0);
			initialized = true;
		}
	}

	private static boolean playShown = false;
	public static void showPlayOverlay(Context context){
		init(context);
		if(!playShown){
			final Dialog dialog = new Dialog(context, android.R.style.Theme_Translucent_NoTitleBar);
			dialog.setContentView(R.layout.overlay_view_playbar_text);
			LinearLayout layout = (LinearLayout) dialog.findViewById(R.id.overlayLayout);

			layout.setOnClickListener(new OnClickListener() {

				@Override

				public void onClick(View arg0) {
					dialog.dismiss();
				}
			});
			dialog.show();
			playShown=true;
			//Store SharedPreferences
			SharedPreferences settings = context.getSharedPreferences("selma", Context.MODE_PRIVATE);
			Editor edit = settings.edit();
			edit.putBoolean(OVERLAY_PLAYSHOWN, playShown);
			edit.commit();
		}
	}

	private static int hintDisplayed = 0;
	public static void showOverlayLessonList(final Context context){
		init(context);
		if(hintDisplayed<4){

			final Dialog dialog = new Dialog(context, android.R.style.Theme_Translucent_NoTitleBar);
			dialog.setContentView(R.layout.overlay_view_starred);
			LinearLayout layout = (LinearLayout) dialog.findViewById(R.id.overlayLayout);
			hintDisplayed = 1;

			layout.setOnClickListener(new OnClickListener() {

				@Override

				public void onClick(View arg0) {
					LinearLayout layout;
					Context ctxt = dialog.getContext();
					switch(hintDisplayed){
					case 1:
						dialog.setContentView(R.layout.overlay_view_top_left);
						layout = (LinearLayout) dialog.findViewById(R.id.overlayLayout);
						layout.setOnClickListener(this);
						break;
					case 2:
						dialog.setContentView(R.layout.overlay_view_playbar_buttons);
						layout = (LinearLayout) dialog.findViewById(R.id.overlayLayout);
						layout.setOnClickListener(this);
						break;
					case 3:
						dialog.setContentView(R.layout.overlay_complete_view);
						TextView tv = (TextView) dialog.findViewById(R.id.overlayCompleteViewText);
						if(tv!=null){
							tv.setText(R.string.how_to_video);
						}
						Button btnOpenVideo = (Button) dialog.findViewById(R.id.buttonOpenVideo);
						if(btnOpenVideo!=null){
							btnOpenVideo.setVisibility(View.VISIBLE);
							btnOpenVideo.setOnClickListener(new OnClickListener() {
								
								@Override
								public void onClick(View v) {
									Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://youtu.be/Zcs657QejaY"));
									context.startActivity(browserIntent);
									
								}
							});
						}
						Button btnNoThanks = (Button) dialog.findViewById(R.id.buttonNoThanks);
						if(btnNoThanks!=null){
							btnNoThanks.setVisibility(View.VISIBLE);
							btnNoThanks.setOnClickListener(this);
						}
						layout = (LinearLayout) dialog.findViewById(R.id.overlayLayout);
						layout.setOnClickListener(this);
						break;
					default:
						dialog.dismiss();
					}
					hintDisplayed++;
					//Store SharedPreferences
					SharedPreferences settings = ctxt.getSharedPreferences("selma", Context.MODE_PRIVATE);
					Editor edit = settings.edit();
					edit.putInt(OVERLAY_HINTDISPLAYED, hintDisplayed);
					edit.commit();
				}
			});

			dialog.show();
		}
	}

	public static void resetOverlays() {
		playShown = false;
		lessonContentShown = 0;
		hintDisplayed = 0;
	}

	private static int lessonContentShown = 0;

	/**
	 * @param showLesson
	 */
	public static void showOverlayLessonContent(Context context) {
		init(context);
		if(lessonContentShown == 0){
			final Dialog dialog = new Dialog(context, android.R.style.Theme_Translucent_NoTitleBar);
			dialog.setContentView(R.layout.overlay_view_top_spinner);
			TextView tv = (TextView) dialog.findViewById(R.id.overlayTopSpinnerText);
			if(tv!=null){
				tv.setText(R.string.how_to_exercises);
			}
			LinearLayout layout = (LinearLayout) dialog.findViewById(R.id.overlayLayout);

			lessonContentShown = 1;
			
			layout.setOnClickListener(new OnClickListener() {

				@Override

				public void onClick(View arg0) {
					LinearLayout layout;
					Context ctxt = dialog.getContext();
					switch(lessonContentShown){
					case 1:
						dialog.setContentView(R.layout.overlay_complete_view);
						layout = (LinearLayout) dialog.findViewById(R.id.overlayLayout);
						layout.setOnClickListener(this);
						break;
					default:
						dialog.dismiss();
					}
					lessonContentShown++;
					//Store SharedPreferences
					SharedPreferences settings = ctxt.getSharedPreferences("selma", Context.MODE_PRIVATE);
					Editor edit = settings.edit();
					edit.putInt(OVERLAY_LESSONCONTENTSHOWN, lessonContentShown);
					edit.commit();
				}
			});
			dialog.show();
		}
	}
}
