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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import basilisk.stockexchangeterminal.R;
import basilisk.stockexchangeterminal.Utils;
import basilisk.stockexchangeterminal.activity.NewOrderActivity;
import basilisk.stockexchangeterminal.entity.OfferBuy;
import basilisk.stockexchangeterminal.api.HttpServerApi;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OfferBuyFragment extends Fragment implements AdapterView.OnItemLongClickListener, SwipeRefreshLayout.OnRefreshListener {
    private Context context;
    private View view;
    private ArrayList dataList;
    private SwipeRefreshLayout swipeRefreshLayout;
    private String currencyPair;
    private int iconResource;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_offer_buy, container, false);
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
        Call<OfferBuy> call = api.getOffersBuy(currencyPair);

        call.enqueue(new Callback<OfferBuy>() {
            @Override
            public void onResponse(Call<OfferBuy> call, Response<OfferBuy> response) {
                Activity activity = getActivity();
                if (activity != null && isAdded()) {
                    swipeRefreshLayout.setRefreshing(false);
                    if (response.isSuccessful()) {
                        OfferBuy offerBuy = response.body();
                        if (offerBuy != null && offerBuy.getList() != null) {
                            List obList = offerBuy.getList();
                            if (obList.size() > 0) {
                                dataList.clear();
                                dataList.addAll(obList);
                                prepareList();
                            }
                        }
                    } else {
                        Toast.makeText(context, getString(R.string.server_request_error) +
                                " #" + response.code() + "\n" + response.message(), Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<OfferBuy> call, Throwable t) {
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
            OfferBuy.Offer bo = (OfferBuy.Offer) dataList.get(i);
            hashMap = new HashMap<>();
            hashMap.put("price", Utils.getFormattedValue(bo.getPrice()));
            hashMap.put("volume", Utils.getFormattedValue(bo.getCurrency_trade()));
            hashMap.put("amount", Utils.getFormattedValue(bo.getCurrency_base()));
            arrayList.add(hashMap);
        }

        ListAdapter adapter = new SimpleAdapter(context, arrayList, R.layout.item_offer_buy,
                new String[]{"price", "volume", "amount"},
                new int[]{R.id.price, R.id.volume, R.id.amount});

        ListView listView = (ListView) view.findViewById(R.id.list_view);
        listView.setAdapter(adapter);
        listView.setOnItemLongClickListener(this);
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
