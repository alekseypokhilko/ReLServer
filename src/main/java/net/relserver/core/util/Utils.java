package net.relserver.core.util;

import com.google.gson.Gson;

public final class Utils {

    private static final Gson GSON = new Gson();

    private Utils() {
        throw new IllegalStateException();
    }

    public static String toJson(Object o) {
        return GSON.toJson(o);
    }

    public static <T> T fromJson(String str, Class<T> cls) {
        return GSON.fromJson(str, cls);
    }
}
