package net.relserver.core.peer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.relserver.core.http.APIClient;
import net.relserver.core.http.ConfigApi;
import net.relserver.core.util.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class HubLoader {
    private static final Type HUBS_TYPE = new TypeToken<List<String>>() {
    }.getType();
    public static List<String> loadFromRemoteRepository() {
        try {
            String response = APIClient.fetchConfig(ConfigApi::hubs);
            if (response == null) {
                return new ArrayList<>();
            }
            List<String> hubs = new Gson().fromJson(response, HUBS_TYPE);
            return hubs == null ? new ArrayList<>() : hubs;
        } catch (Exception e) {
            Logger.log("Cannot load hub hosts file from remote: %s", e.getMessage());
        }
        return new ArrayList<>();
    }

    public static List<String> loadFromResourcesFolder() {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        try (InputStream is = classloader.getResourceAsStream("hubs.json");
             InputStreamReader streamReader = new InputStreamReader(is, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(streamReader)
        ) {
            return new Gson().fromJson(reader, HUBS_TYPE);
        } catch (Exception e) {
            Logger.log("Cannot read hubs.json file: %s", e.getMessage());
        }
        return new ArrayList<>();
    }
}
