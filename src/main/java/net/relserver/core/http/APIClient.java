package net.relserver.core.http;

import net.relserver.core.Constants;
import net.relserver.core.util.Logger;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.util.function.Function;

public class APIClient {
    public static <T> T fetchConfig(Function<ConfigApi, Call<T>> apiCall) {
        OkHttpClient client = new OkHttpClient.Builder().build();
        try {
            ConfigApi api = new Retrofit.Builder()
                    .baseUrl(Constants.GITHUB_BASE_URL)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .client(client)
                    .build()
                    .create(ConfigApi.class);
            return apiCall.apply(api)
                    .execute()
                    .body();
        } catch (Exception e) {
            Logger.log("Exception while fetching config: %s", e.getMessage());
            return null;
        } finally {
            //HACK https://github.com/square/retrofit/issues/3144
            //OkHttp, the HTTP client which sits behind Retrofit by default, uses non-daemon threads.
            //This will prevent the JVM from exiting until they time out.
            //The general pattern for avoiding this scenario is:
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
        }
    }
}
