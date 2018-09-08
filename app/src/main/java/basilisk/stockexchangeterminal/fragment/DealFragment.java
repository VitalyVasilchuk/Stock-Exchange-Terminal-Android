package basilisk.stockexchangeterminal.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.Arrays;
import java.util.HashMap;

import basilisk.stockexchangeterminal.R;
import basilisk.stockexchangeterminal.Utils;
import basilisk.stockexchangeterminal.entity.Deal;
import basilisk.stockexchangeterminal.api.HttpServerApi;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DealFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private Context context;
    private ArrayList dataList = new ArrayList();
    private ListView listView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private String currencyPair;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_deal, container, false);
        context = view.getContext();
        listView = view.findViewById(R.id.list_view);

        swipeRefreshLayout = view.findViewById(R.id.refresh);
        swipeRefreshLayout.setOnRefreshListener(this);

        if (savedInstanceState == null) {
            dataList = new ArrayList();
            currencyPair = (String) getArguments().get("currencyPair");
            loadDataFromHTTPServer(currencyPair);
        }
        else {
            currencyPair = savedInstanceState.getString("CURRENCY_PAIR");
            dataList = savedInstanceState.getParcelableArrayList("DATA_LIST");
            prepareList();
        }

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString("CURRENCY_PAIR", currencyPair);
        savedInstanceState.putParcelableArrayList("DATA_LIST", dataList);
    }

    private void loadDataFromJson() {
        InputStream is = getResources().openRawResource(R.raw.deal);
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
        Deal[] da = gson.fromJson(jsonString, Deal[].class);
        dataList.clear();
        dataList.addAll(Arrays.asList(da));
    }

    private void loadDataFromHTTPServer(String currencyPair) {
        swipeRefreshLayout.setRefreshing(true);
        HttpServerApi api = HttpServerApi.Factory.create();
        Call<Deal[]> deals = api.getDeals(currencyPair);

        deals.enqueue(new Callback<Deal[]>() {
            @Override
            public void onResponse(Call<Deal[]> call, Response<Deal[]> response) {
                Activity activity = getActivity();
                if (activity != null && isAdded()) {
                    swipeRefreshLayout.setRefreshing(false);
                    if (response.isSuccessful()) {
                        Deal[] da = response.body();
                        if (da.length > 0) {
                            dataList.clear();
                            dataList.addAll(Arrays.asList(da));
                            prepareList();
                        }
                    } else {
                        Toast.makeText(context, getString(R.string.server_request_error) +
                                " #" + response.code() + "\n" + response.message(), Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Deal[]> call, Throwable t) {
                Activity activity = getActivity();
                if (activity != null && isAdded()) {
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(context,
                            getString(R.string.server_request_error) + "\n" + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void prepareList() {
        ArrayList<HashMap<String, String>> arrayList = new ArrayList<>();
        HashMap<String, String> hashMap;

        for (int i = 0; i < dataList.size(); i++) {
            Deal d = (Deal) dataList.get(i);
            hashMap = new HashMap<>();
            hashMap.put("ID", d.getId().toString());
            hashMap.put("date", d.getPub_date());
            hashMap.put("type", d.getType());
            hashMap.put("member", d.getUser());
            hashMap.put("price", Utils.getFormattedValue(d.getPrice()));
            hashMap.put("volume", Utils.getFormattedValue(d.getAmnt_trade()));
            hashMap.put("amount", Utils.getFormattedValue(d.getAmnt_base()));
            arrayList.add(hashMap);
        }

        ListAdapter adapter = new SimpleAdapter(context, arrayList, R.layout.item_deal,
                new String[]{"ID", "date", "type", "member", "price", "volume", "amount"},
                new int[]{R.id.ID, R.id.date, R.id.type, R.id.member, R.id.price, R.id.volume, R.id.amount});

        listView.setAdapter(adapter);
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
