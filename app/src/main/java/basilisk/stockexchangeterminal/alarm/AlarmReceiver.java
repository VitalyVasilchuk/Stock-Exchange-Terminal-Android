package basilisk.stockexchangeterminal.alarm;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import basilisk.stockexchangeterminal.BuildConfig;
import basilisk.stockexchangeterminal.R;
import basilisk.stockexchangeterminal.activity.DetailActivity;
import basilisk.stockexchangeterminal.activity.MainActivity;
import basilisk.stockexchangeterminal.activity.SettingsActivity;
import basilisk.stockexchangeterminal.database.AlertPrice;
import basilisk.stockexchangeterminal.database.DatabaseAdapter;
import basilisk.stockexchangeterminal.entity.PriceList;
import basilisk.stockexchangeterminal.api.HttpServerApi;
import basilisk.stockexchangeterminal.api.Interceptors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.Context.NOTIFICATION_SERVICE;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";

    private ArrayList priceList;
    private String currencyTrade, currencyBase, currencyPair, title;
    private int icon;
    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        priceList = new ArrayList();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean switchWatcher = sharedPreferences.getBoolean(SettingsActivity.APP_PREF_SWITCH_WATCHER, true);
        Boolean switchWatcherPrice = sharedPreferences.getBoolean(SettingsActivity.APP_PREF_SWITCH_WATCHER_PRICE, true);
        Boolean switchWatcherOrder = sharedPreferences.getBoolean(SettingsActivity.APP_PREF_SWITCH_WATCHER_ORDER, true);
        int watcherFrequency = Integer.parseInt(sharedPreferences.getString(SettingsActivity.APP_PREF_WATCHER_FREQUENCY, "15"));

        if (switchWatcher && switchWatcherPrice) {
            loadPriceFromHTTPServer();
            start(context, watcherFrequency);
        }
    }

    private static void start(Context context, int interval) {
        long time = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(interval);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        //alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + minute, period, pendingIntent);

        if (Build.VERSION.SDK_INT >= 23) {
            // Wakes up the device in Doze Mode
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent);
        } else if (Build.VERSION.SDK_INT >= 19) {
            // Wakes up the device in Idle Mode
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent);
        } else {
            // Old APIs
            alarmManager.set(AlarmManager.RTC_WAKEUP, time, pendingIntent);
        }
        Log.d(TAG, "start(" + interval + ")");
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        Log.d(TAG, "stop()");
    }

    private void loadPriceFromHTTPServer() {
        HttpServerApi api = HttpServerApi.Factory.create();
        Call<PriceList> priceListCall = api.getMarketPrices();
        priceListCall.enqueue(new Callback<PriceList>() {
            @Override
            public void onResponse(Call<PriceList> call, Response<PriceList> response) {
                if (response.isSuccessful()) {
                    PriceList pl = response.body();
                    List obList = pl.getList();
                    if (obList.size() > 0) {
                        priceList.clear();
                        priceList.addAll(obList);
                        checkPrice();
                    }
                } else {
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "server request error:" +
                            " #" + response.code() + "\n" + response.message());
                    showSampleNotification(context, "onResponse: " + "#" + response.code() + "\n" + response.message());
                }
            }

            @Override
            public void onFailure(Call<PriceList> call, Throwable t) {
                if (BuildConfig.DEBUG) Log.d(TAG, "server request error:" + t.getMessage());
                showSampleNotification(context, "onFailure: " + t.getMessage());
            }
        });
    }

    private void checkPrice() {
        String alertMessage;
        float priceCurrent, priceHigher, priceLower;
        String priceFormat = "%.5f";

        final DatabaseAdapter adapter = new DatabaseAdapter(context);
        adapter.open();
        List<AlertPrice> listAlertPrice = adapter.getListAlertPrice(AlertPrice.COL_ACTIVE + "=?", new String[]{"1"});
        adapter.close();

        if (listAlertPrice != null) {
            for (AlertPrice alertPrice : listAlertPrice) {
                alertMessage = "";
                priceCurrent = 0;

                currencyPair = alertPrice.getCurrencyPair();
                String[] currencies = currencyPair.split("/");
                currencyTrade = currencies[0];
                currencyBase = currencies[1];
                if (currencyBase.equals("BTC"))
                    priceFormat = "%.10f";

                // определение иконки и названия торгуемой валюты
                int iSymbol = Arrays.asList(MainActivity.CURRENCY_SYMB).indexOf(currencyTrade.toLowerCase());
                if (iSymbol < 0) {
                    icon = R.drawable.unknown;
                    title = currencyTrade;
                } else {
                    icon = MainActivity.CURRENCY_ICON[iSymbol];
                    //title = MainActivity.CURRENCY_NAME[iSymbol];
                    title = currencyPair;
                }

                // определение текущей цены для конкретного рынка
                for (int i = 0; i < priceList.size(); i++) {
                    PriceList.Price a = (PriceList.Price) priceList.get(i);

                    if (a.getCurrencyPair().equalsIgnoreCase(currencyPair)) {
                        priceCurrent = Float.parseFloat(a.getPrice());
                        break;
                    }
                }

                if (priceCurrent > 0) {
                    priceHigher = alertPrice.getHigher();
                    priceLower = alertPrice.getLower();

                    if (Float.compare(priceCurrent, priceHigher) >= 0)
                        alertMessage = "достигнут верхний предел цены";
                    if (Float.compare(priceCurrent, priceLower) <= 0)
                        alertMessage = "достигнут нижний предел цены";

                    if (alertMessage.length() > 0) {
                        createNotification(context, (int) alertPrice.getId(),
                                title, alertMessage,
                                String.format(priceFormat, priceCurrent) + " " + currencyBase
                        );
                        alertPrice.setActive(false); // деактивировать запись в таблице
                        alertPrice.setAlert(true); // установить признак срабатывания
                        adapter.open();
                        adapter.update(alertPrice);
                        adapter.close();
                    }

                    if (BuildConfig.DEBUG)
                        Log.d(TAG, currencyPair + ": " +
                                priceHigher + " - [" + priceCurrent + "] - " + priceLower +
                                "; message = [" + alertMessage + "]"
                        );
                }
            }
        }
    }

    private void createNotification(Context context, int id, String title, String message, String info) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean switchAlert = sharedPreferences.getBoolean(SettingsActivity.APP_PREF_NOTIFICATIONS_ALERT, true);
        Boolean switchSound = sharedPreferences.getBoolean(SettingsActivity.APP_PREF_NOTIFICATIONS_ALERT_SOUND, true);
        String ringtone = sharedPreferences.getString(SettingsActivity.APP_PREF_NOTIFICATIONS_ALERT_RINGTONE, "content://settings/system/notification_sound");
        Boolean switchVibrate = sharedPreferences.getBoolean(SettingsActivity.APP_PREF_NOTIFICATIONS_ALERT_VIBRATE, true);
        Boolean switchLed = sharedPreferences.getBoolean(SettingsActivity.APP_PREF_NOTIFICATIONS_ALERT_LED, true);

        if (switchAlert) {
            // vibrate feature requires <uses-permission android:name="android.permission.VIBRATE" /> permission ???
            // создание интента для активити, на которую планируется переход из уведомления
            Intent resultIntent = new Intent(context, DetailActivity.class);
            resultIntent.setAction(currencyPair);
            resultIntent.putExtra("icon", icon);
            resultIntent.putExtra("title", title);
            resultIntent.putExtra("currencyTrade", currencyTrade);
            resultIntent.putExtra("currencyBase", currencyBase);
            resultIntent.putExtra("currencyPair", currencyTrade.toLowerCase() + "_" + currencyBase.toLowerCase()); // конвертирую BTC/UAH в btc_uah);

            // добавил формирование стека вызова активити
            TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context);
            taskStackBuilder.addParentStack(DetailActivity.class); // todo DetailActivity?
            taskStackBuilder.addNextIntent(resultIntent);

/*
            PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent ,
                    PendingIntent.FLAG_UPDATE_CURRENT);
*/
            //заменил вызовом стека
            PendingIntent resultPendingIntent =
                    taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

/*
            BitmapFactory.Options options = new BitmapFactory.Options();
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), icon, options);
*/

            // Create Notification
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(context)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setBadgeIconType(icon)
                            //.setLargeIcon(bitmap)
                            .setContentTitle(title)
                            .setContentText(message)
                            .setContentInfo(info)
                            .setContentIntent(resultPendingIntent)
                            .setPriority(Notification.PRIORITY_HIGH)
                            //.setUsesChronometer(true)
                            .setAutoCancel(true);

            if (switchSound) {
                Uri uri = Uri.parse(ringtone);
                try {
                    //builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
                    builder.setSound(uri);
                } catch (SecurityException e) {
                    Log.d("NOTIFICATIONS", "unable to use custom notification sound " + uri.toString());
                }
            }
            if (switchVibrate) builder.setVibrate(new long[]{0, 100, 200, 200, 100, 100});
            //if (switchLed) builder.setLights(context.getResources().getColor(R.color.led_notification), 500, 500);
            if (switchLed)
                builder.setLights(context.getResources().getColor(R.color.led_notification), 500, 500);

            Notification notification = builder.build();

            // Show Notification
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(id, notification);
        }
    }

    private void showSampleNotification(Context context, String message) {
        if (!BuildConfig.DEBUG) return;

        Intent resultIntent = new Intent(context, MainActivity.class);
        resultIntent.setAction(currencyPair);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        String info = String.valueOf(Interceptors.isNetworkAvailable());
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setBadgeIconType(icon)
                        .setContentTitle("SampleNotification")
                        .setContentText(message)
                        .setContentInfo(info)
                        .setContentIntent(resultPendingIntent)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setAutoCancel(true);

        Notification notification = builder.build();

        // Show Notification
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify((int) System.currentTimeMillis(), notification);
    }

    public static void launchAlarmReceiver(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Boolean switchWatcher = sharedPreferences.getBoolean(SettingsActivity.APP_PREF_SWITCH_WATCHER, true);
        Boolean switchWatcherPrice = sharedPreferences.getBoolean(SettingsActivity.APP_PREF_SWITCH_WATCHER_PRICE, true);
        Boolean switchWatcherOrder = sharedPreferences.getBoolean(SettingsActivity.APP_PREF_SWITCH_WATCHER_ORDER, true);
        int watcherFrequency = Integer.parseInt(sharedPreferences.getString(SettingsActivity.APP_PREF_WATCHER_FREQUENCY, "15"));

        if (switchWatcher && (switchWatcherPrice || switchWatcherOrder)) {
            AlarmReceiver.start(context, watcherFrequency);
        }
    }
}