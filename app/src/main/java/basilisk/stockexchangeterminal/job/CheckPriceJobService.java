/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package basilisk.stockexchangeterminal.job;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

public class CheckPriceJobService extends JobService {
    private static final String TAG = CheckPriceJobService.class.getSimpleName();

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(TAG, "onStartJob");
        CheckPriceIntentService.startActionCheckPrice(getApplicationContext());
        // При делегации выполнения задачи в другие потоки из onStartJob() необходимо вернуть true,
        // а если все необходимые действия уже выполнены в теле этого метода, то вернуть нужно false.
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(TAG, "onStopJob");
        // true — то JobScheduler поставит прерванную задачу в очередь выполнения снова,
        // false — задача будет считаться выполненной и будет удалена из очереди, если она не была периодической.
        return true;
    }
}
