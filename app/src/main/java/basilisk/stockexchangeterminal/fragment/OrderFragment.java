package basilisk.stockexchangeterminal.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
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
import java.util.Map;

import basilisk.stockexchangeterminal.R;
import basilisk.stockexchangeterminal.SingletonSession;
import basilisk.stockexchangeterminal.entity.order.OrderList;
import basilisk.stockexchangeterminal.httpserverapi.HttpServerApi;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderFragment extends Fragment implements AdapterView.OnItemLongClickListener, SwipeRefreshLayout.OnRefreshListener {
    private Context context;
    private View view;
    private ArrayList dataList;
    private SwipeRefreshLayout swipeRefreshLayout;
    private String currencyPair;
    private String deletedEntryId;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_order, container, false);
        context = view.getContext();

        swipeRefreshLayout = view.findViewById(R.id.refresh);
        swipeRefreshLayout.setOnRefreshListener(this);

        // отключил пока использование меню
        //setHasOptionsMenu(true);

        if (savedInstanceState == null) {
            dataList = new ArrayList();
            currencyPair = (String) getArguments().get("currencyPair");
//            loadDataFromJson();
//            prepareList();
            loadDataFromHTTPServer(currencyPair);
        } else {
            dataList = savedInstanceState.getParcelableArrayList("DATA_LIST");
            currencyPair = savedInstanceState.getString("CURRENCY_PAIR");
            prepareList();
        }
        prepareList();

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putParcelableArrayList("DATA_LIST", dataList);
        savedInstanceState.putString("CURRENCY_PAIR", currencyPair);
    }

/*
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_order, menu);
        super.onCreateOptionsMenu(menu,inflater);
    }
*/

    private void loadDataFromHTTPServer(String currencyPair) {
        swipeRefreshLayout.setRefreshing(true);

        Long outOrderId = SingletonSession.Instance().getOutOrderId();
        Long nonce = SingletonSession.Instance().getNonce();
        HttpServerApi api = HttpServerApi.Factory.create();
        Call<OrderList> callBalance = api.getOrder(
                currencyPair,
                SingletonSession.Instance().getPublicKey(),
                SingletonSession.Instance().getApiSign("out_order_id=" + outOrderId + "&nonce=" + nonce),
                outOrderId,
                nonce);

        callBalance.enqueue(new Callback<OrderList>() {
            @Override
            public void onResponse(Call<OrderList> call, Response<OrderList> response) {
                Activity activity = getActivity();
                if (activity != null && isAdded()) {
                    swipeRefreshLayout.setRefreshing(false);
                    if (response.isSuccessful()) {
                        OrderList ob = response.body();
                        List oList = ob.getList();
                        if (oList != null && oList.size() >= 0) {
                            dataList.clear();
                            dataList.addAll(oList);
                            prepareList();
                        } else {
                            String description = response.body().getDescription();
                            if (description != null)
                                Toast.makeText(context, getString(R.string.api_error) + "\n" +
                                        "description = \"" + description + "\"", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(context, getString(R.string.server_request_error) +
                                " #" + response.code() + "\n" + response.message(), Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<OrderList> call, Throwable t) {
                Activity activity = getActivity();
                if (activity != null && isAdded()) {
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(context,
                            getString(R.string.server_request_error) + "\n" + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void loadDataFromJson() {
        InputStream is = getResources().openRawResource(R.raw.order);
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
        OrderList ob = gson.fromJson(jsonString, OrderList.class);
        List orderList = ob.getList();

        dataList.clear();
        dataList.addAll(orderList);
    }


    private void prepareList() {
        ArrayList<HashMap<String, String>> arrayList = new ArrayList<>();
        HashMap<String, String> hashMap;

        for (int i = 0; i < dataList.size(); i++) {
            OrderList.Order o = (OrderList.Order) dataList.get(i);
            hashMap = new HashMap<>();
            hashMap.put("id", o.getId());
            hashMap.put("type", o.getType());
            hashMap.put("price", o.getPrice().substring(0, 10));
            hashMap.put("volume", o.getAmnt_trade().substring(0, 10));
            hashMap.put("amount", o.getAmnt_base().substring(0, 10));
            arrayList.add(hashMap);
        }

        ListAdapter orderAdapter = new SimpleAdapter(context, arrayList, R.layout.item_order,
                new String[]{"id", "type", "price", "volume", "amount"},
                new int[]{R.id.ID, R.id.type, R.id.price, R.id.volume, R.id.amount})
        {
            @Override
            public View getView (int position, View convertView, final ViewGroup parent) {
                View v = super.getView(position, convertView, parent);

                TextView textType = v.findViewById(R.id.type);
                int colorType = (textType.getText().toString().equals("sell")) ? getResources().getColor(R.color.color_ask) : getResources().getColor(R.color.color_bid);
                textType.setTextColor(colorType);

                ImageView imageOrderCancel = v.findViewById(R.id.ib_delete);
                imageOrderCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        View parentRow = (View) v.getParent();
                        if (parentRow != null) {
                            TextView textId = parentRow.findViewById(R.id.ID);
                            TextView textType = parentRow.findViewById(R.id.type);
                            TextView textPrice = parentRow.findViewById(R.id.price);
                            TextView textVolume = parentRow.findViewById(R.id.volume);
                            TextView textAmount = parentRow.findViewById(R.id.amount);

                            String currencyTrade = currencyPair.substring(0, currencyPair.indexOf("_")).toUpperCase();
                            String currencyBase = currencyPair.substring(currencyPair.indexOf("_") + 1).toUpperCase();

                            String confirmationText = "" + getString(R.string.confirm_delete_order) + "\n" +
                                    "ID " + textId.getText().toString() + "\n" +
                                    textType.getText().toString() + " " +
                                    textVolume.getText().toString() + " " + currencyTrade + "\n" +
                                    getString(R.string.confirmation_order_price) + " " + textPrice.getText().toString() + " " + currencyBase + "\n" +
                                    getString(R.string.confirmation_order_amount) + " " + textAmount.getText().toString() + " " + currencyBase;

                            deletedEntryId = textId.getText().toString();

                            AlertDialog.Builder builder;
                            builder = new AlertDialog.Builder(context);
                            builder.setTitle(R.string.delete_entry)
                                    .setMessage(confirmationText)
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            deleteEntry(deletedEntryId);
                                        }
                                    })
                                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            // do nothing
                                        }
                                    })
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .show();
                        }
                    }
                });
                return v;
            }
        };

        ListView listView = view.findViewById(R.id.list_view);
        listView.setFocusable(true);
        listView.setAdapter(orderAdapter);
        listView.setOnItemLongClickListener(this);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
        HashMap<String, String> data = (HashMap<String, String>) adapterView.getItemAtPosition(position);
        deletedEntryId = data.get("id");

        String currencyTrade = currencyPair.substring(0, currencyPair.indexOf("_")).toUpperCase();
        String currencyBase = currencyPair.substring(currencyPair.indexOf("_") + 1).toUpperCase();

        String confirmationText = "" + getString(R.string.confirm_delete_order) + "\n" +
                "ID " + data.get("id") + "\n" +
                data.get("type") + " " +
                data.get("volume") + " " + currencyTrade + "\n" +
                getString(R.string.confirmation_order_price) + " " + data.get("price") + " " + currencyBase + "\n" +
                getString(R.string.confirmation_order_amount) + " " + data.get("amount") + " " + currencyBase;

        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.delete_entry)
                .setMessage(confirmationText)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        deleteEntry(deletedEntryId);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();

        return true;
    }

    private void deleteEntry(String deletedEntryId) {
        Long outOrderId = SingletonSession.Instance().getOutOrderId();
        Long nonce = SingletonSession.Instance().getNonce();
        HttpServerApi api = HttpServerApi.Factory.create();
        Call<Map<String, Object>> call = api.removeOrder(
                //currencyPair,
                deletedEntryId,
                SingletonSession.Instance().getPublicKey(),
                SingletonSession.Instance().getApiSign("out_order_id=" + outOrderId + "&nonce=" + nonce),
                outOrderId,
                nonce);

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                String message;
                if (response.isSuccessful()) {
                    onRefresh();
                }
                boolean status = (boolean) response.body().get("status");
                if (status) {
                    message = getString(R.string.order_delete_success);
                }
                else {
                    message = getString(R.string.order_delete_error);
                    String description = (String) response.body().get("description");
                    if (description != null) {
                        message += ":\n" + description;
                    }
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(context,
                        getString(R.string.server_request_error) + "\n" + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

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
