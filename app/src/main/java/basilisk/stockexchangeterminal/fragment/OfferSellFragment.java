package basilisk.stockexchangeterminal.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import basilisk.stockexchangeterminal.R;
import basilisk.stockexchangeterminal.activity.NewOrderActivity;
import basilisk.stockexchangeterminal.entity.offersell.OfferSell;
import basilisk.stockexchangeterminal.httpserverapi.HttpServerApi;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class OfferSellFragment extends Fragment implements AdapterView.OnItemLongClickListener, SwipeRefreshLayout.OnRefreshListener {
    private Context context;
    private View view;
    private ArrayList dataList = new ArrayList();
    private SwipeRefreshLayout swipeRefreshLayout;
    private String currencyPair;
    private int iconResource;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_offer_sell, container, false);
        context = view.getContext();

        swipeRefreshLayout = view.findViewById(R.id.refresh);
        swipeRefreshLayout.setOnRefreshListener(this);

        if (savedInstanceState == null) {
            dataList = new ArrayList();
            currencyPair = (String) getArguments().get("currencyPair");
            iconResource = (int) getArguments().get("iconResource");
            loadDataFromHTTPServer(currencyPair);
        } else {
            dataList = savedInstanceState.getParcelableArrayList("DATA_LIST");
            currencyPair = savedInstanceState.getString("CURRENCY_PAIR");
            iconResource = savedInstanceState.getInt("ICON_RESOURCE");
            prepareList();
        }

        return view;

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putParcelableArrayList("DATA_LIST", dataList);
        savedInstanceState.putString("CURRENCY_PAIR", currencyPair);
        savedInstanceState.putInt("ICON_RESOURCE", iconResource);
    }

    private void loadDataFromHTTPServer(String currencyPair) {
        swipeRefreshLayout.setRefreshing(true);
        HttpServerApi api = HttpServerApi.Factory.create();
        Call<OfferSell> offersSell = api.getOffersSell(currencyPair);

        offersSell.enqueue(new Callback<OfferSell>() {
            @Override
            public void onResponse(Call<OfferSell> call, Response<OfferSell> response) {
                Activity activity = getActivity();
                if (activity != null && isAdded()) {
                    swipeRefreshLayout.setRefreshing(false);
                    if (response.isSuccessful()) {
                        if (response.body().getList().size() > 0) {
                            dataList.clear();
                            dataList.addAll(response.body().getList());
                            prepareList();
                        }
                    } else {
                        Toast.makeText(context, getString(R.string.server_request_error) +
                                " #" + response.code() + "\n" + response.message(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<OfferSell> call, Throwable t) {
                Activity activity = getActivity();
                if (activity != null && isAdded()) {
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(context,
                            getString(R.string.server_request_error) + "\n" + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void prepareList() {
        ArrayList<HashMap<String, String>> arrayList = new ArrayList<>();
        HashMap<String, String> hashMap;

        for (int i = 0; i < dataList.size(); i++) {
            OfferSell.Offer bo = (OfferSell.Offer) dataList.get(i);
            hashMap = new HashMap<>();
            hashMap.put("price", bo.getPrice());
            hashMap.put("volume", bo.getCurrency_trade());
            hashMap.put("amount", bo.getCurrency_base());
            arrayList.add(hashMap);
        }

        ListAdapter adapter = new SimpleAdapter(context, arrayList, R.layout.item_offer_sell,
                new String[]{"price", "volume", "amount"},
                new int[]{R.id.price, R.id.volume, R.id.amount});

        ListView listView = (ListView) view.findViewById(R.id.list_view);
        listView.setAdapter(adapter);
        listView.setOnItemLongClickListener(this);
    }

    private void loadDataFromJson() {
        InputStream is = getResources().openRawResource(R.raw.offer_sell);
        Writer writer = new StringWriter();
        char[] buffer = new char[1024];
        try {
            Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
            is.close();
        } catch (IOException | JsonSyntaxException | JsonIOException e) {
            System.out.println(e.toString());
        } finally {
        }

        String jsonString = writer.toString();
        Gson gson = new Gson();
        OfferSell ob = gson.fromJson(jsonString, OfferSell.class);
        List offerList = ob.getList();

        dataList.clear();
        dataList.addAll(offerList);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        HashMap<String, String> data = (HashMap<String, String>) adapterView.getItemAtPosition(position);
        Intent intent = new Intent(context, NewOrderActivity.class);
        intent.putExtra("icon", iconResource);
        intent.putExtra("priceBid", data.get("price"));
        intent.putExtra("priceAsk", data.get("price"));
        intent.putExtra("balanceBase", "");
        intent.putExtra("balanceTrade", "");
        intent.putExtra("volume", data.get("volume"));
        intent.putExtra("currencyTrade", currencyPair.substring(0, currencyPair.indexOf("_")).toUpperCase());
        intent.putExtra("currencyBase", currencyPair.substring(currencyPair.indexOf("_") + 1).toUpperCase());
        startActivity(intent);

        return true;
    }

    @Override
    public void onRefresh() {
        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                loadDataFromHTTPServer(currencyPair);
            }
        });
    }
}
