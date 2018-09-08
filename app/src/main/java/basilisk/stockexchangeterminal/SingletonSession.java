package basilisk.stockexchangeterminal;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import basilisk.stockexchangeterminal.activity.SettingsActivity;
import basilisk.stockexchangeterminal.api.NbuService;
import basilisk.stockexchangeterminal.entity.RateNbu;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SingletonSession {
    public static final String APP_PREF_PUBLIC_KEY = "public_key";
    public static final String APP_PREF_PRIVATE_KEY = "private_key";
    public static final String APP_PREF_PASSWORD_HASH = "password_hash";
    public static final String APP_PREF_PASSWORD_SALT = "password_salt";

    private final String TAG = "SingletonSession";
    private static SingletonSession instance;

    private String publicKey;
    private String privateKey;
    private String passwordValue;
    private String passwordHash;
    private String passwordSalt;
    private Long nonce;
    private Long outOrderId;
    private Boolean authStatus;

    private String balanceBase;
    private String balanceTrade;

    private SingletonSession() {
        Context context = App.getAppContext();
        // удалить старые настройки, созданные до версии #13
        try {
            //context.getSharedPreferences("StockExchangeTerminal", Context.MODE_PRIVATE).edit().clear().commit();
            File file = new File(context.getCacheDir().getParent() + "/shared_prefs/StockExchangeTerminal.xml");
            if (file.exists()) file.delete();
            file = new File(context.getCacheDir().getParent() + "/shared_prefs/StockExchangeTerminal.bak");
            if (file.exists()) file.delete();
            file = new File(context.getCacheDir().getParent() + "/shared_prefs/StockExchangeTerminal.xml.bak");
            if (file.exists()) file.delete();        }
        catch (Exception e) {
            Log.d(TAG, "Error deleting the OLD settings file");
        }

        try {
            Enigma.generateSecret(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.authStatus = false;

        this.passwordValue = "";
        this.passwordHash = "";
        this.passwordSalt = "";
        this.publicKey = "";
        this.privateKey = "";

        this.nonce = System.currentTimeMillis()+1;
        this.outOrderId =  System.currentTimeMillis()-1;

        balanceBase = "";
        balanceTrade = "";

        // запрос курса НБУ для рынка USD/UAH
        Call<RateNbu[]> call = NbuService.Factory.getExchangeRate("USD");
        call.enqueue(new Callback<RateNbu[]>() {
            @Override
            public void onResponse(Call<RateNbu[]> call, Response<RateNbu[]> response) {
                if (response.isSuccessful()) {
                    RateNbu[] rates = response.body();
                    if (rates != null && rates.length > 0) {
                        if (BuildConfig.DEBUG) Log.d(TAG, rates[0].toString());
                        PreferenceManager.getDefaultSharedPreferences(App.getAppContext()).edit().putString(SettingsActivity.APP_PREF_RATE_USDUAH, String.valueOf(rates[0].getRate())).apply();
                    }
                } else {
                }
            }

            @Override
            public void onFailure(Call<RateNbu[]> call, Throwable t) {
            }
        });

        readSharedPreferences();
    }

    public void readSharedPreferences() {

        String encryptedString;
        byte[] encryptedBytes;
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(App.getAppContext());

        this.passwordHash = sharedPref.getString(APP_PREF_PASSWORD_HASH, "");
        this.passwordSalt = sharedPref.getString(APP_PREF_PASSWORD_SALT, "");

        if (!(passwordValue.isEmpty())) {
            // чтение публичного ключа к API
            encryptedString = sharedPref.getString(APP_PREF_PUBLIC_KEY, "");
            if (!encryptedString.isEmpty()) {
                try {
                    encryptedBytes = Enigma.easDecrypt(App.getAppContext(), Base64.decode(encryptedString, Base64.NO_WRAP), passwordValue);
                    this.publicKey = new String(encryptedBytes);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // чтение приватного ключа к API
            encryptedString = sharedPref.getString(APP_PREF_PRIVATE_KEY, "");
            if (!encryptedString.isEmpty()) {
                try {
                    encryptedBytes = Enigma.easDecrypt(App.getAppContext(), Base64.decode(encryptedString, Base64.NO_WRAP), getPasswordValue());
                    this.privateKey = new String(encryptedBytes);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static SingletonSession Instance() {
        if (instance == null) {
            instance = new SingletonSession();
        }
        return instance;
    }


    // возвращает хеш сумму переданной строки с использованием алгоритма хеширования "SHA-256"
    public String getHashSHA256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(text.getBytes("UTF-8"));
            byte[] digest = md.digest();
            String hex = String.format("%064x", new BigInteger(1, digest));
            return hex;
        } catch (NoSuchAlgorithmException ex) {
            Log.e(TAG, "getHashSHA256()", ex);
        } catch (UnsupportedEncodingException ex) {
            Log.e(TAG, "getHashSHA256()", ex);
        }
        return "";
    }

    public String getApiSign(String text) {
        return getHashSHA256(text + getPrivateKey());
    }

    public Long getNonce() {
        return nonce++;
    }

    public void setNonce(Long nonce) {
        this.nonce = nonce;
    }

    public Long getOutOrderId() {
        return outOrderId--;
    }

    public void setOutOrderId(Long out_order_id) {
        this.outOrderId = out_order_id;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public Boolean getAuthStatus() {
        return authStatus;
    }

    public void setAuthStatus(Boolean authStatus) {
        this.authStatus = authStatus;
    }

    public Boolean isCorrectKeys() {
        return (!passwordHash.isEmpty() && publicKey.length() == 64 && privateKey.length() == 64 && !publicKey.equals(privateKey));
    }

    public String getPasswordValue() {
        return passwordValue;
    }

    public void setPasswordValue(String passwordValue) {
        this.passwordValue = passwordValue;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public void setPasswordSalt(String passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    public Boolean isCorrectPin() {
        return !(passwordValue.isEmpty());
    }

    public String getBalanceBase() {
        return balanceBase;
    }

    public void setBalanceBase(String balanceBase) {
        this.balanceBase = balanceBase;
    }

    public String getBalanceTrade() {
        return balanceTrade;
    }

    public void setBalanceTrade(String balanceTrade) {
        this.balanceTrade = balanceTrade;
    }
}
