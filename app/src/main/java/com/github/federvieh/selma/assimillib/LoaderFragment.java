/**
 * 
 */
package com.github.federvieh.selma.assimillib;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.github.federvieh.selma.MainActivity.ActivityState;
import com.github.federvieh.selma.R;

/**
 * @author frank
 *
 */
public class LoaderFragment extends Fragment {
	public static final String FORCE_RESET = "com.github.federvieh.selma.assimillib.FORCE_RESET";
	private static final String INFO_DIALOG_ID = "INFO_DIALOG_ID";

	//TODO: Attribute mainActivity should be moved from static to reload on rotation
	private static LoaderFragmentCallbacks mainActivity;
	private ActivityState currentState = ActivityState.DATABASE_LOADING;
	private Button continueBtn;
	private Button leftBtn;
	private Button middleBtn;
	private OnClickListener quitListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			System.exit(Activity.RESULT_OK);
		}
	};
	private TextView textViewDescription1;
	private TextView textViewWaitForDB;
	private TextView textViewDescription2;
	private View progressIndicator;
	private int infoDialogId = -1;

	/**
	 * @param mainActivity
	 */
	public LoaderFragment(LoaderFragmentCallbacks mainActivity) {
		super();
		LoaderFragment.mainActivity = mainActivity;
	}

	public LoaderFragment() {
		super();
		Log.w("LT", "Empty Constructor called. Probably rotating.");
		if(mainActivity==null){
			Log.w("LT", "No mainActivity!");
		}
	}

	/**
	 * @param activity
	 * @param i
	 */
	public LoaderFragment(LoaderFragmentCallbacks mainActivity, int infoDialogId) {
		super();
		LoaderFragment.mainActivity = mainActivity;
		this.infoDialogId = infoDialogId;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
	        Bundle savedInstanceState){
        // Inflate the layout for this fragment
		Log.w("LT", this.getClass().getSimpleName()+".onCreateView(); container="+container);
        return inflater.inflate(R.layout.loader, container, false);
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(INFO_DIALOG_ID, infoDialogId);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i("LT", this.getClass().getSimpleName()+".onCreate(); savedInstanceState="+savedInstanceState);

		if(savedInstanceState!=null){
			this.infoDialogId = savedInstanceState.getInt(INFO_DIALOG_ID, -1);
			Log.d("LT", this.getClass().getSimpleName()+",onCreate(): infoDialogId="+infoDialogId);
		}
	}

	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onResume()
	 */
	@Override
	public void onResume() {
		super.onResume();
		textViewDescription1 = (TextView)getView().findViewById(R.id.selma_description1);
		textViewDescription2 = (TextView)getView().findViewById(R.id.selma_description2);
	    textViewWaitForDB = (TextView)getView().findViewById(R.id.wait_for_database_text);
	    progressIndicator = getView().findViewById(R.id.wait_for_database_progress);
		continueBtn = (Button) getView().findViewById(R.id.button_go_to_main);
		leftBtn = (Button) getView().findViewById(R.id.button_uninstall);
		middleBtn = (Button) getView().findViewById(R.id.button_middle);
		switch(infoDialogId){
		case 0:
			showFinished01(true);
			break;
		case 1:
			showFinished01(false);
			break;
		case 2:
			showNoFiles02();
			break;
		case 3:
			showNoFiles03();
			break;
		case 4:
			showNoFiles04();
			break;
		case 5:
			showNoFiles05();
			break;
		case 6:
			showNoFiles06();
			break;
		case 7:
			showNoFiles07();
			break;
		case 10:
			showNoFiles10();
			break;
		default:
			showWaiting(currentState);
			break;
		}
	}
	
	/**
	 * @param b
	 */
	public void showWaiting(ActivityState databaseLoading) {
		Log.d("LT", "databaseLoading="+databaseLoading);
		currentState  = databaseLoading;
		switch(databaseLoading){
		case DATABASE_LOADING:
		{
		    TextView textView = (TextView)getView().findViewById(R.id.wait_for_database_text);
		    textView.setText(R.string.waiting_for_database);
		    break;
		}
		case FORCED_SCANNING_FOR_LESSONS:
		{
			//On forced re-scans we don't show the intro text.
			getView().findViewById(R.id.selma_description1).setVisibility(View.GONE);
			getView().findViewById(R.id.selma_description2).setVisibility(View.GONE);
		}
		//FALLTHRU
		case INITIAL_SCANNING_FOR_LESSONS:
		{
		    TextView textView = (TextView)getView().findViewById(R.id.wait_for_database_text);
		    textView.setText(R.string.refreshing_database);
		    break;
		}
		case READY_FOR_PLAYBACK_AFTER_SCANNING:
		{
			FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
			fragmentManager.beginTransaction()
			.replace(R.id.container, new LoaderFragment(mainActivity, 1))
			.commit();
			break;
		}
		case READY_FOR_PLAYBACK_AFTER_FORCED_SCANNING:
		{
			FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
			fragmentManager.beginTransaction()
			.replace(R.id.container, new LoaderFragment(mainActivity, 0))
			.commit();
			break;
		}
		case READY_FOR_PLAYBACK_NO_SCANNING:
			mainActivity.onLoadingFinished(true);
			break;
		default:
			break;
		}
	}

	protected void showFinished01(boolean forcedScanning){
		//Show continue button to start LessonListFragment (if lessons have been found)
		continueBtn.setEnabled(true);
		//Disable progress indicators
		if(forcedScanning){
			textViewDescription1.setVisibility(View.GONE);
		}
		getView().findViewById(R.id.wait_for_database_progress).setVisibility(View.GONE);
		getView().findViewById(R.id.selma_description2).setVisibility(View.GONE);

		/* Update the texts and buttons. */
		TextView textView = (TextView)getView().findViewById(R.id.wait_for_database_text);
		boolean lessonsFound = (AssimilDatabase.getDatabase(getActivity(), false)!= null) &&
				(AssimilDatabase.getDatabase(getActivity(), false).getAllLessonHeaders().size()!=0);
		if(lessonsFound){
			leftBtn.setVisibility(View.GONE);
			textView.setText(R.string.selma_description_scanning_finished);
			textView.setTextColor(getResources().getColor(R.color.DarkGreen));
			continueBtn.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					mainActivity.onLoadingFinished(true);
				}
			});
		}
		else{
			leftBtn.setEnabled(true);
			textView.setText(R.string.selma_description_no_content_found);
			textView.setTextColor(getResources().getColor(R.color.Red));
			continueBtn.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
					fragmentManager.beginTransaction()
					.replace(R.id.container, new LoaderFragment(mainActivity, 2))
					.addToBackStack(null)
					.commit();
//					showNoFiles02();
				}
			});
			middleBtn.setVisibility(View.VISIBLE);
			middleBtn.setEnabled(true);
			middleBtn.setText(R.string.quit);
			middleBtn.setOnClickListener(quitListener );
			leftBtn.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Uri packageUri = Uri.parse("package:"+getActivity().getApplicationInfo().packageName);
					Intent uninstallIntent =
							new Intent(Intent.ACTION_DELETE, packageUri);
					startActivity(uninstallIntent);
				}
			});
		}

	}
	/** Show the second dialog: Website / Quit / Continue
	 * 
	 */
	protected void showNoFiles02() {
		progressIndicator.setVisibility(View.GONE);

		//Set-up textviews
		textViewWaitForDB.setVisibility(View.GONE);
		textViewDescription2.setVisibility(View.GONE);
		textViewDescription1.setText(R.string.no_files_dialog_2_msg);

		//Set-up buttons
		continueBtn.setVisibility(View.VISIBLE);
		continueBtn.setEnabled(true);
		continueBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
				fragmentManager.beginTransaction()
				.replace(R.id.container, new LoaderFragment(mainActivity, 3))
				.addToBackStack(null)
				.commit();
			}
		});
		middleBtn.setVisibility(View.VISIBLE);
		middleBtn.setEnabled(true);
		middleBtn.setText(R.string.quit);
		middleBtn.setOnClickListener(quitListener );
		leftBtn.setVisibility(View.VISIBLE);
		leftBtn.setEnabled(true);
		leftBtn.setText(R.string.open_web_site);
		leftBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://federvieh.github.io/selma"));
				startActivity(browserIntent);
			}
		});
	}

	/** Show the third dialog: Quit / Continue
	 * 
	 */
	protected void showNoFiles03() {
		progressIndicator.setVisibility(View.GONE);

		//Set-up textviews
		textViewWaitForDB.setVisibility(View.GONE);
		textViewDescription2.setVisibility(View.GONE);
		textViewDescription1.setText(R.string.no_files_dialog_3_msg);

		//Set-up buttons
		continueBtn.setVisibility(View.VISIBLE);
		continueBtn.setEnabled(true);
		continueBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
				fragmentManager.beginTransaction()
				.replace(R.id.container, new LoaderFragment(mainActivity, 4))
				.addToBackStack(null)
				.commit();
			}
		});
		middleBtn.setVisibility(View.GONE);
		leftBtn.setVisibility(View.VISIBLE);
		leftBtn.setEnabled(true);
		leftBtn.setText(R.string.quit);
		leftBtn.setOnClickListener(quitListener);
	}

	/** Show the forth dialog: Re-try / Quit / Continue
	 * 
	 */
	protected void showNoFiles04() {
		progressIndicator.setVisibility(View.GONE);

		//Set-up textviews
		textViewWaitForDB.setVisibility(View.GONE);
		textViewDescription2.setVisibility(View.GONE);
		textViewDescription1.setText(R.string.no_files_dialog_4_msg);

		//Set-up buttons
		continueBtn.setVisibility(View.VISIBLE);
		continueBtn.setEnabled(true);
		continueBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
				fragmentManager.beginTransaction()
				.replace(R.id.container, new LoaderFragment(mainActivity, 5))
				.addToBackStack(null)
				.commit();
			}
		});
		middleBtn.setVisibility(View.VISIBLE);
		middleBtn.setEnabled(true);
		middleBtn.setText(R.string.quit);
		middleBtn.setOnClickListener(quitListener);
		leftBtn.setVisibility(View.VISIBLE);
		leftBtn.setEnabled(true);
		leftBtn.setText(R.string.retry);
		leftBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent i = getActivity().getPackageManager()
						.getLaunchIntentForPackage( getActivity().getPackageName() );
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				i.putExtra(FORCE_RESET, true);
				startActivity(i);
			}
		});
	}

	/** Show the fifth dialog: GooglePlay / Quit / Continue
	 * 
	 */
	protected void showNoFiles05() {
		progressIndicator.setVisibility(View.GONE);

		//Set-up textviews
		textViewWaitForDB.setVisibility(View.GONE);
		textViewDescription2.setVisibility(View.GONE);
		textViewDescription1.setText(R.string.no_files_dialog_5_msg);

		//Set-up buttons
		continueBtn.setVisibility(View.VISIBLE);
		continueBtn.setEnabled(true);
		continueBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
				fragmentManager.beginTransaction()
				.replace(R.id.container, new LoaderFragment(mainActivity, 6))
				.addToBackStack(null)
				.commit();
			}
		});
		middleBtn.setVisibility(View.VISIBLE);
		middleBtn.setEnabled(true);
		middleBtn.setText(R.string.quit);
		middleBtn.setOnClickListener(quitListener);
		leftBtn.setVisibility(View.VISIBLE);
		leftBtn.setEnabled(true);
		leftBtn.setText(R.string.open);
		leftBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MediaStore.INTENT_ACTION_MUSIC_PLAYER);
				startActivity(intent);
			}
		});
	}

	/** Show the sixth dialog: No / Quit / Yes
	 * 
	 */
	protected void showNoFiles06() {
		progressIndicator.setVisibility(View.GONE);

		//Set-up textviews
		textViewWaitForDB.setVisibility(View.GONE);
		textViewDescription2.setVisibility(View.GONE);
		textViewDescription1.setText(R.string.no_files_dialog_6_msg);

		//Set-up buttons
		continueBtn.setVisibility(View.VISIBLE);
		continueBtn.setText(R.string.yes);
		continueBtn.setEnabled(true);
		continueBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
				fragmentManager.beginTransaction()
				.replace(R.id.container, new LoaderFragment(mainActivity, 7))
				.addToBackStack(null)
				.commit();
			}
		});
		middleBtn.setVisibility(View.VISIBLE);
		middleBtn.setEnabled(true);
		middleBtn.setText(R.string.quit);
		middleBtn.setOnClickListener(quitListener);
		leftBtn.setVisibility(View.VISIBLE);
		leftBtn.setEnabled(true);
		leftBtn.setText(R.string.no);
		leftBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
				fragmentManager.beginTransaction()
				.replace(R.id.container, new LoaderFragment(mainActivity, 10))
				.addToBackStack(null)
				.commit();
			}
		});
	}

	/** Show dialog 7a: Quit / SendEmail
	 * 
	 */
	protected void showNoFiles07() {
		progressIndicator.setVisibility(View.GONE);
		final EditText isbnEditText = (EditText) getView().findViewById(R.id.isbnEditText);
		isbnEditText.setVisibility(View.VISIBLE);

		//Set-up textviews
		textViewWaitForDB.setVisibility(View.GONE);
		textViewDescription2.setVisibility(View.GONE);
		textViewDescription1.setText(R.string.no_files_dialog_7a_msg);

		//Set-up buttons
		continueBtn.setVisibility(View.VISIBLE);
		continueBtn.setText(R.string.email_chooser);
		continueBtn.setEnabled(true);
		continueBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
						"mailto","frank.oltmanns+selma@gmail.com", null));
				emailIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getText(R.string.email_no_files_subject));
				emailIntent.putExtra(android.content.Intent.EXTRA_TEXT,
						getResources().getText(R.string.email_no_files_text1)+isbnEditText.getText().toString()+getResources().getText(R.string.email_no_files_text2));
				startActivity(Intent.createChooser(emailIntent, getResources().getText(R.string.email_chooser)));
			}
		});
		middleBtn.setVisibility(View.GONE);
		leftBtn.setVisibility(View.VISIBLE);
		leftBtn.setEnabled(true);
		leftBtn.setText(R.string.quit);
		leftBtn.setOnClickListener(quitListener);
	}

	/** Show dialog 7b: Quit
	 * 
	 */
	protected void showNoFiles10() {
		progressIndicator.setVisibility(View.GONE);

		//Set-up textviews
		textViewWaitForDB.setVisibility(View.GONE);
		textViewDescription2.setVisibility(View.GONE);
		textViewDescription1.setText(R.string.no_files_dialog_7b_msg);

		//Set-up buttons
		continueBtn.setVisibility(View.VISIBLE);
		continueBtn.setText(R.string.quit);
		continueBtn.setEnabled(true);
		continueBtn.setOnClickListener(quitListener);
		middleBtn.setVisibility(View.GONE);
		leftBtn.setVisibility(View.GONE);
	}

	/**
	 * Callbacks interface that all activities using this fragment must
	 * implement.
	 */
	public static interface LoaderFragmentCallbacks {
		/**
		 * Called when an item in the navigation drawer is selected.
		 * @param b 
		 */
		void onLoadingFinished(boolean b);
	}

}
