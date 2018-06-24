package basilisk.stockexchangeterminal.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import basilisk.stockexchangeterminal.R;
import basilisk.stockexchangeterminal.SingletonSession;
import basilisk.stockexchangeterminal.entity.account.AccountList;
import basilisk.stockexchangeterminal.entity.ticker.Ticker;
import basilisk.stockexchangeterminal.fragment.ViewPagerAdapter;
import basilisk.stockexchangeterminal.httpserverapi.HttpServerApi;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetailActivity extends AppCompatActivity implements View.OnClickListener{

    private ViewPager viewPager;
    private ViewPagerAdapter adapter;
    private TabLayout tabLayout;
    private CharSequence titles[];
    private int numbOfTabs;

    private String currencyTrade;
    private String currencyBase;
    private String currencyPair;
    private String priceBid;
    private String priceAsk;
    private String balanceBase;
    private String balanceTrade;

    private int iconResource;
    private TextView text_ask;
    private TextView text_bid;
    private TextView text_currency_trade;
    private TextView text_balance_trade;
    private TextView text_balance_base;

    final int REQUEST_CODE_NEW_ORDER = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        CharSequence titles[] = {
                getString(R.string.tab_chart),
                getString(R.string.tab_buy),
                getString(R.string.tab_sell),
                getString(R.string.tab_deal),
                getString(R.string.tab_my_order),
                getString(R.string.tab_my_deal)};

        this.titles = titles;

        // читаем переданные параметры из родительского активити
        Intent intent = getIntent();
        setTitle(intent.getStringExtra("title"));
        currencyPair = intent.getStringExtra("currencyPair");
        currencyTrade = intent.getStringExtra("currencyTrade");
        currencyBase = intent.getStringExtra("currencyBase");
        iconResource = intent.getIntExtra("icon", R.drawable.unknown);

        priceBid = "";
        priceAsk = "";
        balanceBase = "";
        balanceTrade = "";

        ImageView iconCoin = findViewById(R.id.image_icon);
        iconCoin.setImageResource(iconResource);

        text_ask = findViewById(R.id.text_ask);
        text_bid = findViewById(R.id.text_bid);
        text_currency_trade = findViewById(R.id.text_currency_trade);
        text_balance_trade = findViewById(R.id.text_balance_trade);
        text_balance_base = findViewById(R.id.text_balance_base);
        text_currency_trade.setText(currencyTrade);

        // создаем ViewPagerAdapter и подключаем Fragment Manager, устанавливаем заголовки и количество закладок.
        numbOfTabs = SingletonSession.Instance().getAuthStatus() ? titles.length : titles.length-2;
        adapter = new ViewPagerAdapter(getSupportFragmentManager(), titles, numbOfTabs);
        adapter.setCurrencyPair(currencyPair);
        adapter.setIconResource(iconResource);

        // связываем ViewPager View с адаптером
        viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(adapter);

        // получаем TabLayout, связываем его с ViewPager View
        tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager));

        // реализация плавающей кнопки
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DetailActivity.this, NewOrderActivity.class);
                intent.putExtra("icon", iconResource);
                intent.putExtra("priceBid", priceBid);
                intent.putExtra("priceAsk", priceAsk);
                intent.putExtra("balanceBase", balanceBase);
                intent.putExtra("balanceTrade", balanceTrade);
                intent.putExtra("volume", "");
                intent.putExtra("currencyTrade", currencyPair.substring(0, currencyPair.indexOf("_")).toUpperCase());
                intent.putExtra("currencyBase", currencyPair.substring(currencyPair.indexOf("_") + 1).toUpperCase());
                startActivityForResult(intent, REQUEST_CODE_NEW_ORDER);
            }
        });

        CardView cardAccount = findViewById(R.id.card_account);
        cardAccount.setOnClickListener(this);

        loadTicker();
        if (SingletonSession.Instance().getAuthStatus()) loadBalance();
    }

    private void loadTicker() {
        Call<Map<String, Object>> call = HttpServerApi.Factory.ticker(currencyPair);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    LinkedTreeMap o = (LinkedTreeMap) response.body().get(currencyPair);
                    if (o != null) {
                        Gson gson = new GsonBuilder().create();
                        Ticker ticker = gson.fromJson(o.toString(), new TypeToken<Ticker>() {}.getType());
                        priceBid = ticker.getBuy();
                        priceAsk = ticker.getSell();
                        text_bid.setText(priceBid.substring(0, 10));
                        text_ask.setText(priceAsk.substring(0, 10));
                        String sBalance = text_balance_trade.getText().toString();
                        if (sBalance.length() > 0) {
                            float f = Float.parseFloat(priceBid) * Float.parseFloat(sBalance);
                            text_balance_base.setText("~" + String.format("%.2f", f) + " " + currencyBase);
                        }
                    }
                }
                else {
                    Log.d("loadTicker", "Server request error: " + response.code() + " " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                //Log.d("loadTicker", "Failure: " + t);
                Toast.makeText(DetailActivity.this,
                        getString(R.string.server_request_error) + "\n" + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadBalance() {
        Call<AccountList> call = HttpServerApi.Factory.balance();
        call.enqueue(new Callback<AccountList>() {
            @Override
            public void onResponse(Call<AccountList> call, Response<AccountList> response) {
                if (response.isSuccessful()) {
                    List accounts = response.body().getList();
                    if (accounts != null) {
                        for (int i = 0; i <accounts.size() ; i++) {
                            AccountList.Account a = (AccountList.Account) accounts.get(i);
                            // отображаем остаток на счету валюты торга
                            if (a.getCurrency().equals(currencyTrade)) {
                                balanceTrade = a.getBalance();
                                SingletonSession.Instance().setBalanceTrade(balanceTrade);
                                text_balance_trade.setText(balanceTrade);
                                if (priceBid.length() > 0) {
                                    //float f = Float.parseFloat(a.getBalance()) * Float.parseFloat(priceBid);
                                    //text_balance_base.setText("~" + String.format("%.2f", f) + " " + currencyBase);
                                    float f = new BigDecimal( Float.parseFloat(a.getBalance()) * Float.parseFloat(priceBid)).setScale(3, RoundingMode.DOWN).floatValue();
                                    text_balance_base.setText("~" + Float.toString(f) + " " + currencyBase);
                                }
                            }

                            // отображаем остаток на счету базовой валюты
                            if (a.getCurrency().equals(currencyBase)) {
                                balanceBase = a.getBalance();
                                SingletonSession.Instance().setBalanceBase(balanceBase);
                                getSupportActionBar().setSubtitle(getString(R.string.detail_balance) + " " + currencyBase + ": " + balanceBase);
                            }
                        }
                    }
                    else {
                        text_balance_base.setTextColor(getResources().getColor(R.color.balance_disable));
                        text_balance_trade.setTextColor(getResources().getColor(R.color.balance_disable));
                        text_balance_trade.setText("0.0000000000");
                        text_balance_base.setText("0.00 " + currencyBase);

                        String description = (response.body().getDescription()!= null) ?
                                "\n" + getString(R.string.api_error) + " " +
                                "description = \""+ response.body().getDescription() + "\"" : "";

                        Toast.makeText(DetailActivity.this, getString(R.string.failure_balance) + description, Toast.LENGTH_LONG).show();
                    }
                }
                else {
                    Log.d("loadBalance", "Server request error: " + response.code() + " " + response.message());
                }
            }

            @Override
            public void onFailure(Call<AccountList> call, Throwable t) {
                Toast.makeText(DetailActivity.this,
                        getString(R.string.server_request_error) + "\n" + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_NEW_ORDER:
                if (resultCode == RESULT_OK) loadBalance();
                break;
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.card_account) {
            loadTicker();
            if (SingletonSession.Instance().getAuthStatus()) loadBalance();
        }
    }
}
