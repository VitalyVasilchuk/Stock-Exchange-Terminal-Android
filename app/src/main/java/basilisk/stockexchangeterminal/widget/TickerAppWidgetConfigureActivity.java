/*
 * Copyright (c) 2018. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package basilisk.stockexchangeterminal.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import basilisk.stockexchangeterminal.BuildConfig;
import basilisk.stockexchangeterminal.R;
import basilisk.stockexchangeterminal.activity.MainActivity;
import basilisk.stockexchangeterminal.api.HttpServerApi;
import basilisk.stockexchangeterminal.entity.Ticker;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * The configuration screen for the {@link TickerAppWidget TickerAppWidget} AppWidget.
 */
public class TickerAppWidgetConfigureActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "TickerAppWidget";
    private static final String PREF_PREFIX_KEY = "appwidget_";
    private static final String TAG = "TickerAppWidget";

    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    Spinner mAppWidgetSpinner;
    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            final Context context = TickerAppWidgetConfigureActivity.this;

            // When the button is clicked, store the string locally
            String widgetText = mAppWidgetSpinner.getSelectedItem().toString();
            saveTitlePref(context, mAppWidgetId, widgetText);

            // It is the responsibility of the configuration activity to update the app widget
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            TickerAppWidget.updateAppWidget(context, appWidgetManager, mAppWidgetId);

            // Make sure we pass back the original appWidgetId
            Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, resultValue);
            finish();

            // TODO убрать, должно корректно работать onReceive()
            Intent intent = new Intent(context, TickerAppWidget.class);
            intent.setAction("UPDATE_ALL_WIDGETS");
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), TimeUnit.MINUTES.toMillis(5), pendingIntent);
        }
    };

    public TickerAppWidgetConfigureActivity() {
        super();
    }

    // Write the prefix to the SharedPreferences object for this widget
    static void saveTitlePref(Context context, int appWidgetId, String text) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + appWidgetId, text);
        prefs.apply();
    }

    // Read the prefix from the SharedPreferences object for this widget.
    // If there is no preference saved, get the default from a resource
    static String loadTitlePref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String titleValue = prefs.getString(PREF_PREFIX_KEY + appWidgetId, "btc");
        return titleValue;
    }

    static void deleteTitlePref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId);
        prefs.apply();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED);

        setContentView(R.layout.ticker_app_widget_configure);

        mAppWidgetSpinner = findViewById(R.id.spinner_currency);
        findViewById(R.id.button_apply).setOnClickListener(mOnClickListener);

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        // подготовка списка вылют
        final Spinner spinner = (Spinner) findViewById(R.id.spinner_currency);
        final List<String> currencyList = new ArrayList<>(Arrays.asList(MainActivity.CURRENCY_SYMB));
        final ArrayList tickerList = new ArrayList();

        final ArrayAdapter<String> adp = new ArrayAdapter<String> (this,android.R.layout.simple_spinner_dropdown_item, tickerList);
        spinner.setAdapter(adp);

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
                                tickerList.add(ticker.getCurrencyTrade() + "/" + ticker.getCurrencyBase());
                            }
                        }
                    }

                    // сортировка списка рынков
                    Collections.sort(tickerList, new Comparator<String>() {
                        @Override
                        public int compare(String v1, String v2) {
                            return v1.compareTo(v2); // ascending
                        }
                    });

                    adp.notifyDataSetChanged();
                    String compareValue = loadTitlePref(TickerAppWidgetConfigureActivity.this, mAppWidgetId);
                    if (compareValue != null) {
                        int spinnerPosition = adp.getPosition(compareValue);
                        spinner.setSelection(spinnerPosition);
                    }                }
                else {
                    if (BuildConfig.DEBUG) Log.d(TAG, "loadTickerList.onResponse(): " + response.code() + " " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                if (BuildConfig.DEBUG) Log.d(TAG, "loadTickerList.onFailure(): " + t.getMessage());
            }
        });

    }
}

