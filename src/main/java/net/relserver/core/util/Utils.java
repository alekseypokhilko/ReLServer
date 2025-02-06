package net.relserver.core.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.relserver.core.Constants;
import net.relserver.core.api.model.Operation;

import java.io.Reader;

public final class Utils {

    private static final Gson GSON = new Gson();

    private Utils() {
        throw new IllegalStateException();
    }

    public static String toJson(Object o) {
        return GSON.toJson(o);
    }

    public static  <T> String createRequest(Operation operation, T obj) {
        return operation + Constants.SEPARATOR + Utils.toJson(obj);
    }

    public static <T> T fromJson(String str, Class<T> cls) {
        return GSON.fromJson(str, cls);
    }

    public static <T> T fromJson(String str, TypeToken<T> type) {
        return GSON.fromJson(str, type.getType());
    }

    public static <T> T fromJson(Reader reader, TypeToken<T> type) {
        return GSON.fromJson(reader, type.getType());
    }
}
