package net.relserver.core.app;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.relserver.core.Constants;
import net.relserver.core.Utils;
import net.relserver.core.http.APIClient;
import net.relserver.core.http.ServerApi;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class AppLoader {
    private static final Type APP_LIST_TYPE = new TypeToken<CopyOnWriteArrayList<App>>() {
    }.getType();

    public static List<App> loadFromRemoteRepository() {
        try {
            var api = APIClient.getClient(Constants.GITHUB_BASE_URL)
                    .create(ServerApi.class);
            String response = api.applications()
                    .execute()
                    .body();
            return new Gson().fromJson(response, APP_LIST_TYPE);
        } catch (Exception e) {
            Utils.log("Cannot read applications.json file from remote: " + e.getMessage());
        }
        return null;
    }

    public static List<App> loadFromResourcesFolder() {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        try (InputStream is = classloader.getResourceAsStream("applications.json");
             InputStreamReader streamReader = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(streamReader)
        ) {
            return new Gson().fromJson(reader, new TypeToken<CopyOnWriteArrayList<App>>() {
            }.getType());
        } catch (Exception e) {
            Utils.log("Cannot read applications.json file: " + e.getMessage());
        }
        return null;
    }
}
