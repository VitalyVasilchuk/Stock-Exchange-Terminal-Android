package basilisk.stockexchangeterminal.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import basilisk.stockexchangeterminal.R;

public class DonationActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donation);
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        createList();
    }

    private void createList() {
        HashMap<String, Object> mapWallet = new HashMap<>();
        ArrayList<HashMap<String, Object>> arrayList = new ArrayList<>();

        mapWallet.put("icon", R.drawable.btc);
        mapWallet.put("wallet_symb", "BTC");
        mapWallet.put("wallet_address", "197LMEcLgue3xKhH7GyzMiaKcSUW1SegsX");
        arrayList.add(mapWallet);

        mapWallet = new HashMap<>();
        mapWallet.put("icon", R.drawable.doge);
        mapWallet.put("wallet_symb", "DOGE");
        mapWallet.put("wallet_address", "DHaqkiiJzrKppN7GSFNDqfvYC27Y8AjcZU");
        arrayList.add(mapWallet);

        mapWallet = new HashMap<>();
        mapWallet.put("icon", R.drawable.eth);
        mapWallet.put("wallet_symb", "ETH");
        mapWallet.put("wallet_address", "0x50d9c646da6f1cfdf9bdfb6e4ed1613d2f75bab7");
        arrayList.add(mapWallet);

        mapWallet = new HashMap<>();
        mapWallet.put("icon", R.drawable.krb);
        mapWallet.put("wallet_symb", "KRB");
        mapWallet.put("wallet_address", "KgCxKFZ7RGHE261zuss9kCK5Xi3NR8gQV6LnKEuvtpMoQY5vBZDBpRU3ZS6CJpKsPxgjqLLvhwQDTXk8g6hRUzhd5SD7C4u");
        arrayList.add(mapWallet);

        mapWallet = new HashMap<>();
        mapWallet.put("icon", R.drawable.ltc);
        mapWallet.put("wallet_symb", "LTC");
        mapWallet.put("wallet_address", "LZMAyFdYf2vG9BiGstdTuwbXje2DpGPFxe");
        arrayList.add(mapWallet);

        ListAdapter adapter = new SimpleAdapter(this,
                arrayList,
                R.layout.item_wallet,
                new String[]{"icon", "wallet_symb", "wallet_address"},
                new int[]{R.id.image_icon, R.id.text_wallet_symb, R.id.text_wallet_address});

        ListView listView = findViewById(R.id.list_view);
        listView.setDividerHeight(0);
        listView.setDivider(null);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.menu_donation, menu);
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
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        HashMap itemData = (HashMap) adapterView.getItemAtPosition(position);
        String walletAddress = (String) itemData.get("wallet_address");
        ClipboardManager clipboardManager = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        ClipData clipData;
        clipData = ClipData.newPlainText("text", walletAddress);
        clipboardManager.setPrimaryClip(clipData);

        Toast.makeText(this, R.string.wallet_address_copied, Toast.LENGTH_SHORT).show();
    }
}
