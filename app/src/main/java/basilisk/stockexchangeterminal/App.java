package basilisk.stockexchangeterminal;

import android.app.Application;
import android.content.Context;

import basilisk.stockexchangeterminal.alarm.AlarmReceiver;

public class App extends Application {

    private static final String TAG = App.class.getSimpleName();
    private static Context context;

    public void onCreate() {
        super.onCreate();
        App.context = getApplicationContext();

/*
        // инициализация Job
        Intent intentCheckPrice = new Intent(context, CheckPriceReceiver.class);
        intentCheckPrice.setAction(CheckPriceIntentService.ACTION_CHECK_PRICE);
        context.sendBroadcast(intentCheckPrice);
        Log.d(TAG, "onCreate: action: " + CheckPriceIntentService.ACTION_CHECK_PRICE);
*/

        AlarmReceiver.launchAlarmReceiver(context);
/*
        // Notice that in the manifest, the boot receiver is set to android:enabled="false".
        // This means that the receiver will not be called unless the application explicitly enables it.
        // This prevents the boot receiver from being called unnecessarily.
        // You can enable a receiver (for example, if the user sets an alarm) as follows:
        ComponentName receiver = new ComponentName(context, DeviceBootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
*/
    }

    public static Context getAppContext() {
        return App.context;
    }
}