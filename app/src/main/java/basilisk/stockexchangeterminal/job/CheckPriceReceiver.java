/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package basilisk.stockexchangeterminal.job;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.concurrent.TimeUnit;

/*
    точка входа, BroadcastReceiver для инициализации задач
*/
public class CheckPriceReceiver extends BroadcastReceiver {
    private static final String TAG = CheckPriceReceiver.class.getSimpleName();

    private static final int JOB_ID = 1001;
    private static final long REFRESH_INTERVAL = 15 * 60 * 1000;

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(TAG, "onReceive: action: " + intent.getAction());

        switch (intent.getAction()) {
            case CheckPriceIntentService.ACTION_CHECK_PRICE:
                scheduleJob(context);

                break;
            default:
                throw new IllegalArgumentException("Unknown action.");
        }
    }

    private void scheduleJob(Context context) {
        ComponentName jobService = new ComponentName(context, CheckPriceJobService.class);
        JobInfo.Builder jobInfo = new JobInfo.Builder(JOB_ID, jobService)
                //.setPeriodic(TimeUnit.MINUTES.toMillis(15))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setRequiresDeviceIdle(false)
                .setMinimumLatency(TimeUnit.MINUTES.toMillis(1))
                .setOverrideDeadline(TimeUnit.MINUTES.toMillis(5))
                .setBackoffCriteria(TimeUnit.MINUTES.toMillis(1), JobInfo.BACKOFF_POLICY_LINEAR);

        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        int ret = jobScheduler.schedule(jobInfo.build());
        if (ret == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "Job scheduled successfully");
        } else {
            Log.d(TAG, "Job scheduling failed");
        }
    }
}
