package net.relserver.core.peer;

import com.google.gson.reflect.TypeToken;
import net.relserver.core.Settings;
import net.relserver.core.http.APIClient;
import net.relserver.core.http.ConfigApi;
import net.relserver.core.util.Logger;
import net.relserver.core.util.Utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class HubLoader {
    private static final TypeToken<List<String>> HUBS_TYPE = new TypeToken<>() {
    };
    public static List<String> loadFromRemoteRepository() {
        try {
            String response = APIClient.fetchConfig(ConfigApi::hubs);
            if (response == null) {
                return new ArrayList<>();
            }
            List<String> hubs = Utils.fromJson(response, HUBS_TYPE);
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
            return Utils.fromJson(reader, HUBS_TYPE);
        } catch (Exception e) {
            Logger.log("Cannot read hubs.json file: %s", e.getMessage());
        }
        return new ArrayList<>();
    }

    public static List<String> getHubIps(Settings settings) {
        String hubIp = settings.getString(Settings.hubIp);
        List<String> remoteIps;
        List<String> resourceIps;
        if (hubIp != null && !hubIp.isEmpty()) {
            ArrayList<String> ip = new ArrayList<>();
            ip.add(hubIp);
            return ip;
        } else if (!(remoteIps = loadFromRemoteRepository()).isEmpty()) {
            return remoteIps;
        } else if (!(resourceIps = loadFromResourcesFolder()).isEmpty()) {
            return resourceIps;
        } else {
            ArrayList<String> localhostHub = new ArrayList<>();
            localhostHub.add("127.0.0.1");
            return localhostHub;
        }
    }
}
