/**
 * 
 */
package com.github.federvieh.selma;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListAdapter;

import com.github.federvieh.selma.assimillib.AssimilDatabase;
import com.github.federvieh.selma.assimillib.AssimilLessonListAdapter;
import com.github.federvieh.selma.assimillib.LoaderFragment;

/**
 * @author frank
 *
 */
public class LessonListFragment extends ListFragment {
	private ShowLessonFragmentListener listener;

	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i("LT", this.getClass().getSimpleName()+".onCreate(); savedInstanceState="+savedInstanceState);

		setHasOptionsMenu(true); 
	}

	@Override
	public void onResume() {
		super.onResume();
		ListAdapter la = new AssimilLessonListAdapter(getActivity(), AssimilDatabase.getCurrentLessons());
		this.setListAdapter(la);

		listener.onResumedTitleUpdate("Selma");
	}
	
	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onAttach(android.app.Activity)
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.listener = (ShowLessonFragmentListener) activity;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.main, menu);
	    super.onCreateOptionsMenu(menu, inflater);
	}
	
	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
        case R.id.action_scan:
			Intent i = getActivity().getPackageManager()
			.getLaunchIntentForPackage( getActivity().getPackageName() );
			i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			i.putExtra(LoaderFragment.FORCE_RESET, true);
			startActivity(i);
            return true;
	    }
		return super.onOptionsItemSelected(item);
	}
}
