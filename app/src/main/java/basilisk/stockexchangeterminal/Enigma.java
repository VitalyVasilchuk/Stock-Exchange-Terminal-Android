package basilisk.stockexchangeterminal;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Calendar;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

public class Enigma {
    private static final String TAG = "Enigma";

    private static final String ENCRYPTED_KEY = "aes_key";

    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "Secret";

    private static final String AES_MODE = "AES/CBC/PKCS7Padding";
    private static final String RSA_MODE = "RSA/ECB/PKCS1Padding";


    public static void generateSecret(Context context) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);
        if (!keyStore.containsAlias(KEY_ALIAS)) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // для версии старше 6.0 генерируется и хранится один AES-ключ
                KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
                keyGenerator.init(
                        new KeyGenParameterSpec.Builder(KEY_ALIAS,
                                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                                .setRandomizedEncryptionRequired(false)
                                .setKeySize(256)
                                .build());
                keyGenerator.generateKey();
            } else {
                // для младших версий генерируется и хранится пара RSA ключей
                Calendar start = Calendar.getInstance();
                Calendar end = Calendar.getInstance();
                end.add(Calendar.YEAR, 5);

                KeyPairGenerator kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEY_STORE);

                KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(context)
                        .setAlias(KEY_ALIAS)
                        .setSubject(new X500Principal("CN=" + KEY_ALIAS))
                        .setSerialNumber(BigInteger.TEN)
                        .setStartDate(start.getTime())
                        .setEndDate(end.getTime())
                        .build();
                kpg.initialize(spec);
                kpg.generateKeyPair();
                // затем создается, шифруется RSA, сохраняется один AES-ключ
                aesGenerateKey(context);
            }
        }
    }

    public static byte[] easEncrypt(Context context, byte[] plainTextBytes, String initValue) throws Exception {
        byte[] bytesIV = generateInitVector(initValue, 16);
        Cipher cipher = Cipher.getInstance(AES_MODE);
        cipher.init(Cipher.ENCRYPT_MODE, getSecret(context), new IvParameterSpec(bytesIV));
        byte[] encrypted = cipher.doFinal(plainTextBytes);
        return encrypted;
    }

    public static byte[] easDecrypt(Context context, byte[] plainTextBytes, String InitValue) throws Exception {
        byte[] bytesIV = generateInitVector(InitValue, 16);
        Cipher cipher = Cipher.getInstance(AES_MODE);
        cipher.init(Cipher.DECRYPT_MODE, getSecret(context), new IvParameterSpec(bytesIV));
        byte[] decrypted = cipher.doFinal(plainTextBytes);
        return decrypted;
    }

    private static byte[] rsaEncrypt(byte[] secret) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);
        KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(KEY_ALIAS, null);
        // Encrypt the text
        //Cipher inputCipher = Cipher.getInstance(RSA_MODE, "AndroidOpenSSL");
        Cipher inputCipher = rsaGetCipher();
        inputCipher.init(Cipher.ENCRYPT_MODE, privateKeyEntry.getCertificate().getPublicKey());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, inputCipher);
        cipherOutputStream.write(secret);
        cipherOutputStream.close();

        byte[] vals = outputStream.toByteArray();
        return vals;
    }

    private static byte[] rsaDecrypt(byte[] encrypted) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);
        KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(KEY_ALIAS, null);
        //Cipher output = Cipher.getInstance(RSA_MODE, "AndroidOpenSSL");
        Cipher output = rsaGetCipher();
        output.init(Cipher.DECRYPT_MODE, privateKeyEntry.getPrivateKey());
        CipherInputStream cipherInputStream = new CipherInputStream(
                new ByteArrayInputStream(encrypted), output);
        ArrayList<Byte> values = new ArrayList<>();
        int nextByte;
        while ((nextByte = cipherInputStream.read()) != -1) {
            values.add((byte) nextByte);
        }

        byte[] bytes = new byte[values.size()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = values.get(i).byteValue();
        }
        return bytes;
    }

    private static Cipher rsaGetCipher() {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) { // below android m
                // error in android 6: InvalidKeyException: Need RSA private or public key
                return Cipher.getInstance(RSA_MODE, "AndroidOpenSSL");
            }
            else { // android m and above
                // error in android 5: NoSuchProviderException: Provider not available: AndroidKeyStoreBCWorkaround
                return Cipher.getInstance(RSA_MODE, "AndroidKeyStoreBCWorkaround");
            }
        } catch(Exception exception) {
            throw new RuntimeException("rsaGetCipher: Failed to get an instance of Cipher", exception);
        }
    }

    private static void aesGenerateKey(Context context) throws Exception {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String enryptedKeyB64 = pref.getString(ENCRYPTED_KEY, null);
        if (enryptedKeyB64 == null) {
            SecureRandom random = new SecureRandom();
            // случайная соль для нового ключа
            byte salt[] = new byte[256];
            random.nextBytes(salt);

            // случайный пароль для нового ключа
            byte pass[] = new byte[256];
            random.nextBytes(pass);

            // формирование нового ключа
            PBEKeySpec pbKeySpec = new PBEKeySpec(new String(pass).toCharArray(), salt, 10000, 256);
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            byte[] bytesKey = secretKeyFactory.generateSecret(pbKeySpec).getEncoded();

            // шифрование сформированого AES-ключа алгоритмом RSA
            byte[] encryptedKey = rsaEncrypt(bytesKey);
            // преобразование в MIME64
            enryptedKeyB64 = Base64.encodeToString(encryptedKey, Base64.NO_WRAP);

            // сохранение в настройках
            SharedPreferences.Editor edit = pref.edit();
            edit.putString(ENCRYPTED_KEY, enryptedKeyB64);
            edit.commit();
        }
    }

    private static Key getSecret(Context context) throws Exception {
        Key key = null;
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            key = keyStore.getKey(KEY_ALIAS, null);
        } else {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            String enryptedKeyB64 = pref.getString(ENCRYPTED_KEY, null);

            if (enryptedKeyB64 != null) {
                byte[] encryptedKey = Base64.decode(enryptedKeyB64, Base64.NO_WRAP);
                byte[] decryptedKey = rsaDecrypt(encryptedKey);
                key = new SecretKeySpec(decryptedKey, "AES");
            }
        }

        return key;
    }

    private static byte[] generateInitVector(String initValue, int length) {
        String stringIV = stretch(initValue , 2).substring(0, length);
        byte[] bytesIV = stringIV.getBytes();

        return bytesIV;
    }

    private static String stretch(String numbers, int count) {
        for (int j = 0; j < count; j++) {
            byte[] stringBytes = numbers.getBytes();
            numbers = "";
            for (int i = 0; i < stringBytes.length; i++) {
                numbers += stringBytes[i];
            }
        }
        return numbers;
    }

    public static String hashPassword(final String password, final byte salt[]) {
        try {
            Long startTime = System.currentTimeMillis();

            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 3000, 256);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            SecretKey key = skf.generateSecret(spec);
            byte[] hash = key.getEncoded();

            //Log.d(TAG, "hashPassword() time = " + Long.toString(System.currentTimeMillis() - startTime) + "ms");
            return Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }
}
