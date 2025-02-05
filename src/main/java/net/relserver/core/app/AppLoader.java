package net.relserver.core.app;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.relserver.core.util.Logger;
import net.relserver.core.http.APIClient;
import net.relserver.core.http.ConfigApi;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class AppLoader {
    private static final Type APP_LIST_TYPE = new TypeToken<List<App>>() {
    }.getType();

    public static List<App> loadFromRemoteRepository() {
        try {
            String response = APIClient.fetchConfig(ConfigApi::applications);
            if (response == null) {
                return new ArrayList<>();
            }
            List<App> apps = new Gson().fromJson(response, APP_LIST_TYPE);
            return apps == null ? new ArrayList<>() : apps;
        } catch (Exception e) {
            Logger.log("Cannot read applications.json file from remote: %s", e.getMessage());
        }
        return new ArrayList<>();
    }

    public static List<App> loadFromResourcesFolder() {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        try (InputStream is = classloader.getResourceAsStream("applications.json");
             InputStreamReader streamReader = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(streamReader)
        ) {
            return new Gson().fromJson(reader, APP_LIST_TYPE);
        } catch (Exception e) {
            Logger.log("Cannot read applications.json file: %s", e.getMessage());
        }
        return new ArrayList<>();
    }
}
