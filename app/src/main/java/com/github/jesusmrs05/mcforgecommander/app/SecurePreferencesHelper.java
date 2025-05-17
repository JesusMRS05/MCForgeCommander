package com.github.jesusmrs05.mcforgecommander.app;

import android.content.Context;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

public class SecurePreferencesHelper {

    private static final String PREFS_NAME = "secure_prefs";
    private static final String KEY_CONNECTIONS = "connections";
    private static final Gson gson = new Gson();

    public static void saveConnections(Context context, List<ConnectionInfo> connections) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            EncryptedSharedPreferences sharedPreferences = (EncryptedSharedPreferences) EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            String json = gson.toJson(connections);
            sharedPreferences.edit()
                    .putString(KEY_CONNECTIONS, json)
                    .apply();

        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    public static List<ConnectionInfo> loadConnections(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            EncryptedSharedPreferences sharedPreferences = (EncryptedSharedPreferences) EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            String json = sharedPreferences.getString(KEY_CONNECTIONS, "");
            if (json.isEmpty()) return new ArrayList<>();

            Type type = new TypeToken<List<ConnectionInfo>>() {}.getType();
            return gson.fromJson(json, type);

        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
