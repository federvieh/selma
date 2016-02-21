package com.github.federvieh.selma;

import android.os.AsyncTask;
import android.util.Log;

/**
 * Created by frank on 7/7/15.
 */
public class DelayService extends AsyncTask<Long, Long, Boolean> {
    private static final Integer INTERVAL = 50; //Interval for callbacks in milliseconds
    private final DelayServiceListener listener;
    private long remainingTime;

    public DelayService(DelayServiceListener listener) {
        this.listener = listener;
    }

    @Override
    protected Boolean doInBackground(Long... waitTime) {
        remainingTime = waitTime[0];
        try {
            for (; ; ) {
                publishProgress(remainingTime);
                if (remainingTime > INTERVAL) {
                    Thread.sleep(INTERVAL);
                } else if (remainingTime > 0) {
                    Thread.sleep(remainingTime);
                    return true;
                } else {
                    return true;
                }
                remainingTime -= INTERVAL;
            }
        } catch (InterruptedException e) {
            Log.d("LT", "interrupted");
        }
        return false;
    }

    @Override
    protected void onProgressUpdate(Long... remaining) {
        listener.onWaitingRemainderUpdate(remaining[0]);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        listener.onWaitingFinished(result);
    }

    public long getRemainingTime() {
        return remainingTime;
    }

    public interface DelayServiceListener {
        void onWaitingRemainderUpdate(long remainingTime);

        void onWaitingFinished(boolean result);
    }
}
