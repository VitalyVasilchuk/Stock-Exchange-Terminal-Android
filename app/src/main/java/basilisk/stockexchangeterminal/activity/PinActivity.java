package basilisk.stockexchangeterminal.activity;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.security.SecureRandom;

import basilisk.stockexchangeterminal.Enigma;
import basilisk.stockexchangeterminal.R;
import basilisk.stockexchangeterminal.SingletonSession;

public class PinActivity extends AppCompatActivity {
    private StringBuilder numberPin;
    private int[] pinAsterisk = {
            R.id.image_asterisk_1, R.id.image_asterisk_2, R.id.image_asterisk_3,
            R.id.image_asterisk_4, R.id.image_asterisk_5, R.id.image_asterisk_6,
            R.id.image_asterisk_7, R.id.image_asterisk_8};

    private String mode;
    private final String MODE_CREATION = "creation";
    private final String MODE_CHANGE = "change";
    private final String MODE_VERIFICATION = "verification";

    private String firstValue;

    TextView textTitle;
    TextView textReset;
    ImageButton buttonCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);
        init(savedInstanceState);
    }

    private void init(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            numberPin = new StringBuilder();
            firstValue = "";
            mode = (SingletonSession.Instance().getPasswordHash().isEmpty()) ? MODE_CREATION : MODE_VERIFICATION;
        }
        else {
            numberPin = new StringBuilder(savedInstanceState.getString("numberPin"));
            firstValue = savedInstanceState.getString("firstValue");
            mode = savedInstanceState.getString("mode");
        }

        textTitle = findViewById(R.id.text_title);
        textReset = findViewById(R.id.text_reset);
        buttonCheck = findViewById(R.id.button_check);

        switch (mode) {
            case MODE_CREATION:
                textTitle.setText(R.string.pin_set);
                textReset.setVisibility(View.GONE);
                break;

            case MODE_VERIFICATION:
                textTitle.setText(R.string.pin_enter);
                textReset.setVisibility(View.VISIBLE);
                break;
        }
        updatePinDisplay();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString("numberPin", numberPin.toString());
        savedInstanceState.putString("firstValue", firstValue);
        savedInstanceState.putString("mode", mode);
    }

    @Override
    public void onBackPressed() {
        // Disable going back to the MainActivity
        moveTaskToBack(true);
    }

    // обработка нажатия на цифровую кнопку
    public void onNumClick(View view) {
        if (numberPin.length() < pinAsterisk.length) {
            Button button = (Button) view;
            numberPin.append(button.getText());
            updatePinDisplay();
        }
    }

    public void onBackspaceClick(View view) {
        if (numberPin.length() > 0) {
            numberPin.deleteCharAt(numberPin.length() - 1);
            updatePinDisplay();
        }
    }

    public void onClearClick(View view) {
        numberPin.delete(0, numberPin.length());
        updatePinDisplay();
    }

    public void updatePinDisplay() {
        for (int i = 0; i < pinAsterisk.length; i++) {
            if (numberPin.length() > i) {
                findViewById(pinAsterisk[i]).setVisibility(View.VISIBLE);
            } else {
                findViewById(pinAsterisk[i]).setVisibility(View.GONE);
            }
        }

        //buttonCheck.setEnabled((numberPin.length() < 4) ? false : true);

        if (numberPin.length() == pinAsterisk.length) {
            //checkPin(null);
        }
    }

    public void resetPin(View view) {
        String confirmationText = getString(R.string.pin_reset_confirm);
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.pin_reset_title)
                .setMessage(confirmationText)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(PinActivity.this).edit();
                        editor.remove("password_hash");
                        editor.remove("password_salt");
                        editor.remove("public_key");
                        editor.remove("private_key");
                        editor.commit();
                        SingletonSession.Instance().readSharedPreferences();
                        init(null);
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

    public void checkPin(View view) {
        if (numberPin.length() < 4) {
            Toast.makeText(this, getString(R.string.pin_enter) + "\n" +
                    getString(R.string.pin_4_8), Toast.LENGTH_SHORT).show();
            return;
        }

        switch (mode) {
            case MODE_VERIFICATION:
                String hash = Enigma.hashPassword(numberPin.toString(), Base64.decode(SingletonSession.Instance().getPasswordSalt(), Base64.NO_WRAP));
                if (hash.equals(SingletonSession.Instance().getPasswordHash())) {
                    Toast.makeText(this, R.string.pin_correct, Toast.LENGTH_SHORT).show();
                    SingletonSession.Instance().setPasswordValue(numberPin.toString());
                    SingletonSession.Instance().readSharedPreferences();
                    finish();
                } else {
                    Toast.makeText(this, R.string.pin_wrong, Toast.LENGTH_SHORT).show();
                    onClearClick(null);
                }
                break;

            case MODE_CREATION:
                if (firstValue.isEmpty()) {
                    firstValue = numberPin.toString();
                    onClearClick(null);
                    textTitle.setText(R.string.pin_confirm);
                } else {
                    if (firstValue.equals(numberPin.toString())) {
                        SecureRandom random = new SecureRandom();
                        byte salt[] = new byte[256];
                        random.nextBytes(salt);

                        hash = Enigma.hashPassword(firstValue, salt);
                        if (hash != null) {
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
                            editor.putString("password_hash", hash);
                            editor.putString("password_salt", Base64.encodeToString(salt, Base64.NO_WRAP));
                            editor.commit();
                            SingletonSession.Instance().readSharedPreferences();
                            finish();
                        }
                    } else {
                        firstValue = "";
                        onClearClick(null);
                        textTitle.setText(R.string.pin_enter);
                        Toast.makeText(this, R.string.pin_different, Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }
}
