package basilisk.stockexchangeterminal.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import basilisk.stockexchangeterminal.BuildConfig;
import basilisk.stockexchangeterminal.R;
import basilisk.stockexchangeterminal.Utils;
import basilisk.stockexchangeterminal.activity.MainActivity;
import basilisk.stockexchangeterminal.entity.Ticker;
import basilisk.stockexchangeterminal.api.HttpServerApi;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link TickerAppWidgetConfigureActivity TickerAppWidgetConfigureActivity}
 */
public class TickerAppWidget extends AppWidgetProvider {

    private static final String TAG = "TickerAppWidget";
    private static final String UPDATE_ONE_WIDGET = "UPDATE_ONE_WIDGET";
    private static final String UPDATE_ALL_WIDGETS = "UPDATE_ALL_WIDGETS";

    private static ArrayList tickerList = new ArrayList();

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        String market = TickerAppWidgetConfigureActivity.loadTitlePref(context, appWidgetId);
        String[] itemMarket = market.split("/");

        if (itemMarket.length == 2) {
            // Construct the RemoteViews object
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.ticker_app_widget);

            // определение иконки для виджета, получение данных о цене
            int iSymbol = Arrays.asList(MainActivity.CURRENCY_SYMB).indexOf(itemMarket[0].toLowerCase());
            int icon = R.drawable.unknown;
            String title = market;
            if (iSymbol >= 0) {
                icon = MainActivity.CURRENCY_ICON[iSymbol];
                title = MainActivity.CURRENCY_NAME[iSymbol];
            }

            views.setImageViewResource(R.id.image_icon, icon);
            views.setTextViewText(R.id.text_title, title);
            views.setTextViewText(R.id.text_date_time, (String) DateFormat.format("dd.MM.yyyy HH:mm", System.currentTimeMillis()));
            views.setViewVisibility(R.id.image_update, View.VISIBLE);
            views.setViewVisibility(R.id.progress_bar, View.INVISIBLE);

            for (int i = 0; i < tickerList.size(); i++) {
                Ticker ticker = (Ticker) tickerList.get(i);
                if (ticker.getCurrencyTrade().equalsIgnoreCase(itemMarket[0]) &&
                        ticker.getCurrencyBase().equalsIgnoreCase(itemMarket[1])) {
                    views.setTextViewText(R.id.text_bid, Utils.getFormattedValue(ticker.getBuy()));
                    views.setTextViewText(R.id.text_ask, Utils.getFormattedValue(ticker.getSell()));
                    break;
                }
            }

            // launch configuration activity on icon click
            Intent intentConfig = new Intent(context, TickerAppWidgetConfigureActivity.class);
            intentConfig.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            PendingIntent pendingIntentConfig = PendingIntent.getActivity(context, appWidgetId, intentConfig, 0);
            views.setOnClickPendingIntent(R.id.image_icon, pendingIntentConfig);

            // активити и ее параметры
            Intent intentMain = new Intent(context, MainActivity.class);
            PendingIntent pendingIntentMain = PendingIntent.getActivity(context, appWidgetId,
                    intentMain, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.text_bid, pendingIntentMain);
            views.setOnClickPendingIntent(R.id.text_ask, pendingIntentMain);

            // update on image click
            Intent clickIntent = new Intent(context, TickerAppWidget.class);
            clickIntent.setAction(UPDATE_ALL_WIDGETS);
            clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            PendingIntent pendingClickIntent = PendingIntent.getBroadcast(context, appWidgetId, clickIntent, 0);
            views.setOnClickPendingIntent(R.id.image_update, pendingClickIntent);

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        for (int appWidgetId : appWidgetIds) {
            TickerAppWidgetConfigureActivity.deleteTitlePref(context, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        super.onEnabled(context);

        Intent intent = new Intent(context, TickerAppWidget.class);
        intent.setAction(UPDATE_ALL_WIDGETS);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), TimeUnit.MINUTES.toMillis(5), pendingIntent);
        }
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        super.onDisabled(context);

        Intent intent = new Intent(context, TickerAppWidget.class);
        intent.setAction(UPDATE_ALL_WIDGETS);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (intent.getAction().equalsIgnoreCase(UPDATE_ALL_WIDGETS)) {
            Bundle extras = intent.getExtras();
            final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

            int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
            if (extras != null) {
                appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
                // подмена иконки обновления индикатором загрузки
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.ticker_app_widget);
                views.setViewVisibility(R.id.image_update, View.INVISIBLE);
                views.setViewVisibility(R.id.progress_bar, View.VISIBLE);
                appWidgetManager.updateAppWidget(appWidgetId, views);
            }

            ComponentName thisAppWidget = new ComponentName(context.getPackageName(), getClass().getName());
            int appWidgetIds[] = appWidgetManager.getAppWidgetIds(thisAppWidget);

            loadTickerList(context, appWidgetManager, appWidgetIds, appWidgetId);
        }

        if (intent.getAction().equalsIgnoreCase(UPDATE_ONE_WIDGET)) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                updateAppWidget(context, appWidgetManager, appWidgetId);
            }
        }
    }

    private void loadTickerList(final Context context, final AppWidgetManager appWidgetManager,
                                final int[] appWidgetIds, final int appWidgetId) {
        Call<Map<String, Object>> call = HttpServerApi.Factory.tickerList();
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Map<String, Object> list = response.body();
                    tickerList.clear();
                    for (Map.Entry<String, Object> entry : list.entrySet()) {
                        if (!entry.getKey().equals("status")) {
                            LinkedTreeMap o = (LinkedTreeMap) entry.getValue();
                            if (o != null) {
                                Gson gson = new GsonBuilder().create();
                                Ticker ticker = gson.fromJson(o.toString(), new TypeToken<Ticker>() {
                                }.getType());
                                tickerList.add(ticker);
                            }
                        }
                    }
                    // Обновить каждый виджет
                    for (int appWidgetID : appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, appWidgetID);
                    }
                } else {
                    if (BuildConfig.DEBUG) Log.d(TAG, "loadTickerList.onResponse(): " + response.code() + " " + response.message());
                    Intent intent = new Intent(context, TickerAppWidget.class);
                    intent.setAction(UPDATE_ONE_WIDGET);
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                    context.sendBroadcast(intent);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                if (BuildConfig.DEBUG) Log.d(TAG, "loadTickerList.onFailure(): " + t.getMessage());
                Intent intent = new Intent(context, TickerAppWidget.class);
                intent.setAction(UPDATE_ONE_WIDGET);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                context.sendBroadcast(intent);
            }
        });
    }
}

