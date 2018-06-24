package basilisk.stockexchangeterminal.activity;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import basilisk.stockexchangeterminal.R;

public class KeyInputActivity extends AppCompatActivity {

    private TextView textKey;
    private TextInputLayout textLayoutKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_input);
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationIcon(R.drawable.ic_cancel);
            setSupportActionBar(toolbar);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String keyName = extras.getString(SettingsActivity.EXTRA_KEY_NAME);
            if (keyName != null) setTitle(keyName);
        }

        textKey = findViewById(R.id.text_key);
        textLayoutKey = findViewById(R.id.text_layout_key);
        textKey.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_LEFT = 0;
                final int DRAWABLE_TOP = 1;
                final int DRAWABLE_RIGHT = 2;
                final int DRAWABLE_BOTTOM = 3;

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (textKey.getRight() - textKey.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        // your action here
                        scanQR(null);
                        return true;
                    }
                }
/*
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (textKey.getLeft() - textKey.getCompoundDrawables()[DRAWABLE_LEFT].getBounds().width())) {
                        // your action here
                        pastText(null);
                        return true;
                    }
                }
*/
                return false;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_new_key, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                setResult(Activity.RESULT_CANCELED);
                finish();
                break;

            case R.id.menu_save_key:
                if (textKey.getText().toString().length() != 64) {
                    textLayoutKey.setError(getString(R.string.key_length_error));
                    //Toast.makeText(this, getString(R.string.key_input_message), Toast.LENGTH_LONG).show();
                    break;
                }
                Intent data = new Intent();
                data.putExtra(SettingsActivity.EXTRA_KEY, textKey.getText().toString());
                setResult(RESULT_OK, data);
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void pastText(View view) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        ClipData clipData = clipboard.getPrimaryClip();
        ClipData.Item item = clipData.getItemAt(0);

        textKey.setText(item.getText().toString());
    }

    public void scanQR(View view) {
        IntentIntegrator ii = new IntentIntegrator(this)
                .setOrientationLocked(false)
                .setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
                .setBeepEnabled(false)
                .setCaptureActivity(ScannerActivity.class);
        //.initiateScan();

        Intent intent = ii.createScanIntent();
        startActivityForResult(intent, IntentIntegrator.REQUEST_CODE);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        String scanText;
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            scanText = result.getContents();
            if (scanText != null) {
                textKey.setText(scanText);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
