package basilisk.stockexchangeterminal.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import basilisk.stockexchangeterminal.BuildConfig;
import basilisk.stockexchangeterminal.R;
import basilisk.stockexchangeterminal.SingletonSession;
import basilisk.stockexchangeterminal.database.AlertPrice;
import basilisk.stockexchangeterminal.database.DatabaseAdapter;
import basilisk.stockexchangeterminal.entity.PriceList;
import basilisk.stockexchangeterminal.entity.Ticker;
import basilisk.stockexchangeterminal.api.HttpServerApi;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements
        AdapterView.OnItemClickListener,
        SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "MainActivity";
    private ArrayList priceList;

    public static final String[] CURRENCY_SYMB = {
            "ada", "bch", "btc", "btg", "dash",
            "doge", "etc", "eth", "fno", "iota",
            "iti", "krb", "ltc", "neo", "nvc",
            "ppc", "sib", "tlr", "usdt", "xem",
            "xlm", "xmr", "xrp", "zec"
    };

    public static final String[] CURRENCY_NAME = {
            "Cardano", "Bitcoin Cash", "Bitcoin", "Bitcoin Gold", "Dash",
            "Dogecoin", "Ethereum Classic", "Ethereum", "Fonero", "IOTA",
            "iTicoin", "Karbo", "Litecoin", "NEO", "Novacoin",
            "Peercoin", "SIBCoin", "Taler", "Tether", "NEM",
            "Stellar", "Monero", "Ripple", "Zcash"
    };

    public static final int[] CURRENCY_ICON = {
            R.drawable.ada, R.drawable.bch, R.drawable.btc, R.drawable.btg, R.drawable.dash,
            R.drawable.doge, R.drawable.etc, R.drawable.eth, R.drawable.fno, R.drawable.iota,
            R.drawable.iti, R.drawable.krb, R.drawable.ltc, R.drawable.neo, R.drawable.nvc,
            R.drawable.ppc, R.drawable.sib, R.drawable.tlr, R.drawable.usdt, R.drawable.xem,
            R.drawable.xlm, R.drawable.xmr, R.drawable.xrp, R.drawable.zec
    };

    private SwipeRefreshLayout swipeRefreshLayout;
    private Menu menu;

    AlertDialog alertDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        swipeRefreshLayout = findViewById(R.id.refresh);
        swipeRefreshLayout.setOnRefreshListener(this);

        // авторизация
        //authOnHttpServer();

        if (savedInstanceState == null) {
            priceList = new ArrayList();
            loadDataFromHTTPServer();
            //loadTickerList();
        } else {
            priceList = savedInstanceState.getStringArrayList("PRICE_LIST");
            prepareList();

/*
            Bundle b = savedInstanceState.getParcelable("alertDialog");
            if (b != null)  {
                alertDialog = new AlertDialog.Builder(getApplicationContext()).create();
                alertDialog.onRestoreInstanceState(b);
                alertDialog.show();
            }
*/
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (SingletonSession.Instance().getPasswordValue().isEmpty())
            startActivity(new Intent(this, PinActivity.class));
        // авторизация
        authOnHttpServer();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putSerializable("PRICE_LIST", priceList);

        if (alertDialog != null) {
            //savedInstanceState.putParcelable("alertDialog", alertDialog.onSaveInstanceState());
            alertDialog.dismiss();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (SingletonSession.Instance().getAuthStatus())
            this.menu.findItem(R.id.main_auth).setIcon(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_auth_true));
        else
            this.menu.findItem(R.id.main_auth).setIcon(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_auth_false));

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            this.menu.findItem(R.id.main_whitelist).setVisible(true);
        else
            this.menu.findItem(R.id.main_whitelist).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.main_auth) {
            authOnHttpServer();
            return true;
        }

/*
        if (id == R.id.main_donations) {
            Intent intent = new Intent(this, DonationActivity.class);
            startActivity(intent);
            return true;
        }
*/

        if (id == R.id.main_whitelist) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent();
                String packageName = getPackageName();
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (pm.isIgnoringBatteryOptimizations(packageName)) {
                    intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                }
                else {
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                }
                startActivity(intent);
            }
        }

        if (id == R.id.main_setting) {
            //Intent intent = new Intent(this, SettingsOldActivity.class);
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        if (id == R.id.main_about) {
            String message =
                    getString(R.string.about_subtitle) + "\n\n" +
                            "v." + BuildConfig.VERSION_NAME + "\n" +
                            "(c) 2018 by Basilisk" + "\n\n" +
                            getString(R.string.about_agreement);
            String[] aboutMessageArray = {
                    getString(R.string.about_subtitle),
                    "v." + BuildConfig.VERSION_NAME + "\n" + "(c) 2018 by Basilisk",
                    getString(R.string.about_agreement)};

            AlertDialog.Builder builder;
            builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.app_name)
                    .setItems(aboutMessageArray, null)
                    //.setMessage(message)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            builder.create();
            builder.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        HashMap itemData = (HashMap) adapterView.getItemAtPosition(position);
        String currencyTrade = (String) itemData.get("currencyTrade");
        String currencyBase = (String) itemData.get("currencyBase");
        String currencyPair = (String) itemData.get("currencyPair");
        currencyPair = currencyPair.replace("/", "_").toLowerCase(); // конвертирую BTC/UAH в btc_uah
        if (!currencyTrade.equals("UAH")) {
            // формируем параметры для передачи в дочернее активити
            Intent intent = new Intent(this, DetailActivity.class);

            intent.putExtra("icon", (int) itemData.get("icon"));
            intent.putExtra("title", (String) itemData.get("title"));
            intent.putExtra("currencyTrade", currencyTrade);
            intent.putExtra("currencyBase", currencyBase);
            intent.putExtra("currencyPair", currencyPair);

            // выполняем переход в дочернее активити, с набором параметров
            startActivity(intent);
        }
    }

    private void authOnHttpServer() {
        if (SingletonSession.Instance().getAuthStatus()) return;
        if (!SingletonSession.Instance().isCorrectKeys()) return;

        Call<Map<String, Object>> call = HttpServerApi.Factory.auth();
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    SingletonSession.Instance().setAuthStatus((Boolean) response.body().get("status"));

                    if (SingletonSession.Instance().getAuthStatus())
                        menu.getItem(0).setIcon(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_auth_true));

                    //loadBalance();
                    //loadTicker();
                } else {
                    String message = getString(R.string.server_request_error) + " #" + response.code() + "\n";
                    if (response.code() == 500) {
                        message += getString(R.string.auth_check_keys);
                        menu.getItem(0).setIcon(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_auth_false));
                    } else {
                        message += response.message();
                        menu.getItem(0).setIcon(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_auth_false));
                    }
                    Toast.makeText(MainActivity.this,
                            getString(R.string.auth_error) + "\n" +
                                    message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                menu.getItem(0).setIcon(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_auth_false));
                Toast.makeText(MainActivity.this,
                        getString(R.string.auth_error) + "\n" +
                                getString(R.string.server_request_error) + "\n" + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadDataFromHTTPServer() {
        swipeRefreshLayout.setRefreshing(true);

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
                        prepareList();
                    }
                } else {
                    Toast.makeText(MainActivity.this, getString(R.string.server_request_error) +
                            " #" + response.code() + "\n" + response.message(), Toast.LENGTH_LONG).show();
                }
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onFailure(Call<PriceList> call, Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(MainActivity.this,
                        getString(R.string.server_request_error) + "\n" + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void prepareList() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean switchSortPrice = sharedPreferences.getBoolean(SettingsActivity.APP_PREF_SWITCH_SORT_PRICE, true);
        Boolean switchMarketUah = sharedPreferences.getBoolean(SettingsActivity.APP_PREF_SWITCH_MARKET_UAH, false);
        Boolean switchPriceUah = sharedPreferences.getBoolean(SettingsActivity.APP_PREF_SWITCH_PRICE_USD, true);
        float rateUsdUah = Float.parseFloat(sharedPreferences.getString(SettingsActivity.APP_PREF_RATE_USDUAH, "26.3"));

        String priceUsd;
        String marketWeight;
        HashMap<String, Object> hashMap;
        ArrayList<HashMap<String, Object>> arrayList = new ArrayList<>();

        for (int i = 0; i < priceList.size(); i++) {
            PriceList.Price a = (PriceList.Price) priceList.get(i);

            if (a.getCurrencyBase().equalsIgnoreCase("UAH"))
                marketWeight = "1";
            else {
                if (switchMarketUah) continue;
                marketWeight = "2";
            }

            priceUsd = "";
            hashMap = new HashMap<>();

            int iSymbol = Arrays.asList(CURRENCY_SYMB).indexOf(a.getCurrencyTrade().toLowerCase());
            if (iSymbol < 0) {
                hashMap.put("icon", R.drawable.unknown);
                hashMap.put("title", a.getCurrencyTrade());
            } else {
                hashMap.put("icon", CURRENCY_ICON[iSymbol]);
                hashMap.put("title", CURRENCY_NAME[iSymbol]);
            }
            hashMap.put("currencyTrade", a.getCurrencyTrade());
            hashMap.put("currencyBase", a.getCurrencyBase());
            hashMap.put("currencyPair", a.getCurrencyPair());
            hashMap.put("price", a.getPrice());
            hashMap.put("marketWeight", marketWeight);
            if (switchPriceUah && a.getCurrencyBase().equalsIgnoreCase("UAH") && rateUsdUah > 0f) {
                try {
                    priceUsd = "($" + String.format("%.5f", Float.parseFloat(a.getPrice()) / rateUsdUah).replace(",", ".") + ")";
                } catch (NumberFormatException e) {
                    // java.lang.NumberFormatException: Invalid float: "No deals"
                }
            }
            hashMap.put("priceUsd", priceUsd);

            arrayList.add(hashMap);
        }

        // сортировка листинга рынков
        if (switchSortPrice) {
            Collections.sort(arrayList, new Comparator<Map<String, Object>>() {
                final static String COMPARE_KEY0 = "marketWeight";
                final static String COMPARE_KEY1 = "currencyBase";
                final static String COMPARE_KEY2 = "currencyTrade";

                @Override
                public int compare(Map<String, Object> lhs, Map<String, Object> rhs) {
                    String v1 = (String) lhs.get(COMPARE_KEY0);
                    String v2 = (String) rhs.get(COMPARE_KEY0);
                    int result = v1.compareTo(v2); // ascending
                    if (result != 0) return result;

                    v1 = (String) lhs.get(COMPARE_KEY1);
                    v2 = (String) rhs.get(COMPARE_KEY1);
                    result = v1.compareTo(v2); // ascending
                    if (result != 0) return result;

                    v1 = (String) lhs.get(COMPARE_KEY2);
                    v2 = (String) rhs.get(COMPARE_KEY2);
                    result = v1.compareTo(v2); // ascending
                    return result;
                }
            });
        }

        // реализация адаптера списка
        ListAdapter adapter = new SimpleAdapter(this, arrayList, R.layout.item_price,
                new String[]{"icon", "currencyPair", "price", "priceUsd"},
                new int[]{R.id.image_icon, R.id.currency_pair, R.id.price, R.id.priceUsd}) {
            Boolean switchWatcherPrice = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean(SettingsActivity.APP_PREF_SWITCH_WATCHER_PRICE, true);
            Boolean switchWatcher = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean(SettingsActivity.APP_PREF_SWITCH_WATCHER, true);

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                final ImageView imageAlert = v.findViewById(R.id.image_alert);
                TextView currencyPair = v.findViewById(R.id.currency_pair);

                if (switchWatcher && switchWatcherPrice) {
                    DatabaseAdapter adapterDB = new DatabaseAdapter(MainActivity.this);
                    adapterDB.open();
                    AlertPrice alertPrice = adapterDB.getAlertPrice(currencyPair.getText().toString());
                    adapterDB.close();

                    int alertIco = R.drawable.ic_alert_off;
                    if (alertPrice != null) {
                        if (alertPrice.isAlert()) alertIco = R.drawable.ic_alert_ring;
                        else if (alertPrice.isActive()) alertIco = R.drawable.ic_alert_on;
                    }
                    imageAlert.setImageResource(alertIco);

                    imageAlert.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            View parentRow = (View) view.getParent();
                            if (parentRow != null) {
                                TextView textPrice = parentRow.findViewById(R.id.price);
                                TextView textCurrencyPair = parentRow.findViewById(R.id.currency_pair);
                                ImageView imageView = parentRow.findViewById(R.id.image_icon);
                                Drawable icon = imageView.getDrawable();

                                showAlertDialog(
                                        textCurrencyPair.getText().toString(),
                                        Float.parseFloat(textPrice.getText().toString()),
                                        icon,
                                        imageAlert
                                );
                            }
                        }
                    });
                } else
                    imageAlert.setVisibility(View.GONE);
                return v;
            }
        };

        ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
    }

    private void showAlertDialog(String currencyPair, final float price, Drawable icon, final ImageView imageAlert) {
        LayoutInflater layoutAlert = LayoutInflater.from(MainActivity.this);
        //View viewAlert = layoutAlert.inflate(R.layout.dialog_alert, null);
        View viewAlert = layoutAlert.inflate(R.layout.dialog_alert_icon, null);

        final TextView textPair = viewAlert.findViewById(R.id.text_pair);
        textPair.setText(currencyPair);
        ImageView imageView = viewAlert.findViewById(R.id.image_icon);
        imageView.setImageDrawable(icon);
        final Switch switchActive = viewAlert.findViewById(R.id.switch_active);
        final TextView textDelta = viewAlert.findViewById(R.id.text_delta);
        SeekBar seekBar = viewAlert.findViewById(R.id.seekbar_delta);

        long temp_id = 0;
        boolean checkActive;
        String higher, current, lower;
        int deltaStart = 2;

        final DatabaseAdapter adapterDB = new DatabaseAdapter(MainActivity.this);
        adapterDB.open();
        AlertPrice alertPrice = adapterDB.getAlertPrice(currencyPair);
        if (alertPrice != null) {
            // update
            temp_id = alertPrice.getId();
            higher = (Float.toString(alertPrice.getHigher()) + "0000000000").substring(0, 10);
            current = (Float.toString(price) + "0000000000").substring(0, 10);
            lower = (Float.toString(alertPrice.getLower()) + "0000000000").substring(0, 10);
            checkActive = alertPrice.isActive();
            textDelta.setText("%");
        } else {
            //insert
            higher = (Float.toString(price * (1f + deltaStart / 100f)) + "0000000000").substring(0, 10);
            current = (Float.toString(price) + "0000000000").substring(0, 10);
            lower = (Float.toString(price * (1f - deltaStart / 100f)) + "0000000000").substring(0, 10);
            checkActive = true;
            textDelta.setText(deltaStart + "%");
            seekBar.setProgress(deltaStart);
        }
        final long id = temp_id;

        final TextView textHigher = viewAlert.findViewById(R.id.text_higher);
        TextView textCurrent = viewAlert.findViewById(R.id.text_current);
        final TextView textLower = viewAlert.findViewById(R.id.text_lower);
//        final TextInputLayout textLayoutHigher= viewAlert.findViewById(R.id.text_layout_height);
//        final TextInputLayout textLayoutLower= viewAlert.findViewById(R.id.text_layout_lower);

        textHigher.setText(higher);
        textCurrent.setText(current);
        textCurrent.setInputType(InputType.TYPE_NULL);
        textLower.setText(lower);
        switchActive.setChecked(checkActive);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                String higher, lower;
                higher = (Float.toString(price * (1f + i / 100f)) + "0000000000").substring(0, 10);
                lower = (Float.toString(price * (1f - i / 100f)) + "0000000000").substring(0, 10);

                textHigher.setText(higher);
                textLower.setText(lower);
                textDelta.setText(i + "%");
                switchActive.setChecked(true);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder
                .setView(viewAlert)
                .setTitle((id == 0) ? getString(R.string.add_alert) : getString(R.string.edit_alert))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        AlertPrice alertPrice = new AlertPrice(
                                id,
                                switchActive.isChecked(),
                                false,
                                textPair.getText().toString(),
                                Float.parseFloat(textLower.getText().toString()),
                                Float.parseFloat(textHigher.getText().toString())
                        );
                        if (id == 0) adapterDB.insert(alertPrice);
                        else adapterDB.update(alertPrice);
                        //adapter.close();

                        if (switchActive.isChecked())
                            imageAlert.setImageResource(R.drawable.ic_alert_on);
                        else
                            imageAlert.setImageResource(R.drawable.ic_alert_off);
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        adapterDB.close();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        adapterDB.close();
                    }
                });

        // использую экземпляр alertDialog, чтобы иметь возможность закрыть диалог при переворачивании экрана
        // т.к. иначе будет утечка E/WindowManager: android.view.WindowLeaked
        // todo разобраться с переворотом окна диалога
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void loadTickerList() {
        Call<Map<String, Object>> call = HttpServerApi.Factory.tickerList();
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Map<String, Object> list = response.body();
                    for (Map.Entry<String, Object> entry : list.entrySet()) {
                        if (!entry.getKey().equals("status")) {
                            LinkedTreeMap o = (LinkedTreeMap) entry.getValue();
                            if (o != null) {
                                Gson gson = new GsonBuilder().create();
                                Ticker ticker = gson.fromJson(o.toString(), new TypeToken<Ticker>() {
                                }.getType());
                                Log.d("loadTickers", ticker.toString());
                            }
                        }
                    }
                }
                else {
                    Toast.makeText(MainActivity.this,
                            getString(R.string.server_request_error) + "\n" + response.code() + " " + response.message(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(MainActivity.this,
                        getString(R.string.server_request_error) + "\n" + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onRefresh() {
        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                loadDataFromHTTPServer();
            }
        });
    }

}
