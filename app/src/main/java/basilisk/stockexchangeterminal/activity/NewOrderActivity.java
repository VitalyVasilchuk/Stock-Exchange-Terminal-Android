package basilisk.stockexchangeterminal.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import basilisk.stockexchangeterminal.R;
import basilisk.stockexchangeterminal.SingletonSession;
import basilisk.stockexchangeterminal.api.HttpServerApi;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewOrderActivity extends AppCompatActivity implements View.OnClickListener {
    private int iconResource;
    private TextView textPair;

    TextInputEditText textAvailable;
    TextInputEditText textPrice;
    TextInputEditText textVolume;
    TextInputEditText textAmount;
    TextInputEditText textBid;
    TextInputEditText textAsk;

    TextInputLayout textLayoutAvailable;
    TextInputLayout textLayoutPrice;
    TextInputLayout textLayoutVolume;
    TextInputLayout textLayoutAmount;

    Button buttonSend;
    Button buttonCancel;

    RadioGroup radioGroup;
    RadioButton radioBuy;
    RadioButton radioSell;

    String currencyBase;
    String currencyTrade;
    String priceBid;
    String priceAsk;
    String balanceBase;
    String balanceTrade;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_order);
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationIcon(R.drawable.ic_cancel);
            setSupportActionBar(toolbar);
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // чтение переданных параметров
        Intent intent = getIntent();
        iconResource = intent.getIntExtra("icon", R.drawable.unknown);
        currencyBase = intent.getStringExtra("currencyBase");
        currencyTrade = intent.getStringExtra("currencyTrade");
        priceBid = intent.getStringExtra("priceBid");
        priceAsk = intent.getStringExtra("priceAsk");
/*
        balanceBase = intent.getStringExtra("balanceBase");
        balanceTrade = intent.getStringExtra("balanceTrade");
*/
        balanceBase = SingletonSession.Instance().getBalanceBase();
        balanceTrade = SingletonSession.Instance().getBalanceTrade();

        // округляю баланс до 3-го знака вниз, т.к. иначе возникают ошибки при округлении
        if (!balanceBase.equals("")) {
            float f = new BigDecimal(Float.parseFloat(balanceBase)).setScale(3, RoundingMode.DOWN).floatValue();
            balanceBase = Float.toString(f);
        }

        // иконка монеты
        ImageView iconCoin = findViewById(R.id.image_icon);
        iconCoin.setImageResource(iconResource);
        textPair = findViewById(R.id.text_pair);
        textPair.setText((currencyTrade + "/" + currencyBase).toUpperCase());

        // поля ввода значений
        textPrice = findViewById(R.id.text_price);
        textVolume = findViewById(R.id.text_volume);
        textAmount = findViewById(R.id.text_amount);

        textAvailable = findViewById(R.id.text_available);
        textBid = findViewById(R.id.text_bid);
        textAsk = findViewById(R.id.text_ask);
        textAvailable.setOnClickListener(this);
        textBid.setOnClickListener(this);
        textAsk.setOnClickListener(this);
        textAvailable.setOnClickListener(this);

        // метки
        textLayoutAvailable = findViewById(R.id.text_layout_available);
        textLayoutPrice = findViewById(R.id.text_layout_height);
        textLayoutVolume = findViewById(R.id.text_layout_current);
        textLayoutAmount = findViewById(R.id.text_layout_amount);

        // кнопки
        buttonSend = findViewById(R.id.button_submit);
        buttonCancel = findViewById(R.id.button_cancel);
        buttonSend.setOnClickListener(this);
        buttonCancel.setOnClickListener(this);

        // радиокнопки
        radioBuy = findViewById(R.id.radio_buy);
        radioSell = findViewById(R.id.radio_sell);

        // реализация слушателя ввода значений в полях цены и количества для пересчета суммы
        TextWatcher inputTW = new TextWatcher() {
            public void afterTextChanged(Editable s) {
                float amount = 0f;
                try {
                    float price = Float.parseFloat(textPrice.getText().toString());
                    float volume = Float.parseFloat(textVolume.getText().toString());
                    amount = price * volume;
                    if (radioBuy.isChecked() || radioSell.isChecked()) buttonSend.setEnabled(true);
                } catch (NumberFormatException ex) {
                    buttonSend.setEnabled(false);
                }
                textAmount.setText(String.valueOf(amount));
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        };
        textPrice.addTextChangedListener(inputTW);
        textVolume.addTextChangedListener(inputTW);

        // установка переданных параметров
        if (balanceBase.equals("")) {
            textAvailable.setTextColor(getResources().getColor(R.color.balance_disable));
            textAvailable.setText(getString(R.string.failure_balance));
        }
        textPrice.setText(priceBid);
        textVolume.setText("" + intent.getStringExtra("volume"));

        textAvailable.setInputType(InputType.TYPE_NULL);
        textBid.setInputType(InputType.TYPE_NULL);
        textAsk.setInputType(InputType.TYPE_NULL);
        textAmount.setInputType(InputType.TYPE_NULL);

        textPrice.setHint(currencyBase);
        textVolume.setHint(currencyTrade);
        textAmount.setHint(currencyBase);
        textBid.setText(priceBid);
        textAsk.setText(priceAsk);

        textLayoutAvailable.setHint(getString(R.string.label_available));
        textLayoutPrice.setHint(getString(R.string.label_price) + " (" + currencyBase + ")");
        textLayoutVolume.setHint(getString(R.string.label_quantity) + " (" + currencyTrade + ")");
        textLayoutAmount.setHint(getString(R.string.label_amount) + " (" + currencyBase + ")");

        // группа Тип операции, определяем поведение при переключении типов
        radioGroup = findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                radioBuy.setError(null);
                radioSell.setError(null);
                switch (i) {
                    case R.id.radio_buy:
                        buttonSend.setText(R.string.button_label_buy);
                        textPrice.setText("" + priceAsk);
                        textLayoutAvailable.setHint(getString(R.string.label_available) + " (" + currencyBase + ")");
                        if (textAmount.getText().length() > 0) buttonSend.setEnabled(true);
                        if (!balanceBase.equals(""))
                            textAvailable.setText(balanceBase);
                        break;
                    case R.id.radio_sell:
                        buttonSend.setText(R.string.button_label_sell);
                        textPrice.setText("" + priceBid);
                        textLayoutAvailable.setHint(getString(R.string.label_available) + " (" + currencyTrade + ")");
                        if (textAmount.getText().length() > 0) buttonSend.setEnabled(true);
                        if (!balanceTrade.equals("")) textAvailable.setText(balanceTrade);
                        break;
                    default:
                        break;
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_new_order, menu);
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

            case R.id.menu_send_order:
                // нужно выбрать тип операции
                if (radioGroup.getCheckedRadioButtonId() == -1) {
                    radioBuy.setError("");
                    radioSell.setError("");
                    break;
                }

                float amount = Float.parseFloat(textAmount.getText().toString());
                if (amount > 0) {
                    if (!SingletonSession.Instance().getAuthStatus()) {
                        AlertDialog.Builder builder;
                        builder = new AlertDialog.Builder(this);
                        builder.setMessage(getString(R.string.error_order_auth))
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                })
                                .show();
                        break;
                    }

                    // формирование описания данных ордера
                    String operation = radioBuy.isChecked() ?
                            getString(R.string.confirmation_order_operation_buy) :
                            getString(R.string.confirmation_order_operation_sell);
                    String price = textPrice.getText().toString();
                    String count = textVolume.getText().toString();
                    String sum = textAmount.getText().toString();
                    String currency1 = currencyBase;
                    String currency = currencyTrade;

                    String confirmationText = "" +
                            operation + " " +
                            count + " " + currency + "\n" +
                            getString(R.string.confirmation_order_price) + " " + price + " " + currency1 + "\n" +
                            getString(R.string.confirmation_order_amount) + " " + sum + " " + currency1;

                    // вывод подтверждающего диалога
                    AlertDialog.Builder builder;
                    builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.confirmation_order_title)
                            .setMessage(confirmationText)
                            .setPositiveButton(operation, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    setResult(RESULT_OK);
                                    sendNewOrder();
                                    finish();
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // do nothing
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                } else {
                    textLayoutAmount.setError(getString(R.string.error_order_amount_zero));
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
/*
            case R.id.button_cancel:
                finish();
                break;
            case R.id.button_submit:
                sendNewOrder();
                finish();
                break;
*/
            case R.id.text_bid:
                textPrice.setText(textBid.getText());
                break;
            case R.id.text_ask:
                textPrice.setText(textAsk.getText());
                break;
            case R.id.text_available:
                String volume = "";
                try {
                    float balance = Float.parseFloat(textAvailable.getText().toString());
                    float price = Float.parseFloat(textPrice.getText().toString());
                    if (radioBuy.isChecked())
                        volume = (price > 0) ? Float.toString(balance / price) : "";
                    if (radioSell.isChecked())
                        //volume = Float.toString(balance);
                        volume = textAvailable.getText().toString();
                } catch (NumberFormatException ex) {
                    // значит там поломаный текст был, ничего не делаем
                }
                textVolume.setText(volume);
                break;
        }
    }

    private void sendNewOrder() {
        String operation = radioBuy.isChecked() ? "buy" : "sell";
        String price = textPrice.getText().toString();
        String count = textVolume.getText().toString();
        String currency1 = currencyBase;
        String currency = currencyTrade;

        Call<Map<String, Object>> call = HttpServerApi.Factory.addOrder(
                operation, price, count, currency1, currency
        );
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    String description = String.valueOf((response.body().get("description")));
                    if (description != null) {
                        Log.d("sendNewOrder", description);
                        Toast.makeText(NewOrderActivity.this, description, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.d("sendNewOrder", response.code() + " " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.d("sendNewOrder", "Failure: " + t);
            }
        });
    }
}
