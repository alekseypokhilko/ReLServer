package net.relserver.core.http;

import net.relserver.core.Constants;
import retrofit2.Call;
import retrofit2.http.GET;

public interface ConfigApi {
    @GET(Constants.APPLICATIONS_CONFIG_URL)
    Call<String> applications();

    @GET(Constants.HUBS_URL)
    Call<String> hubs();
}
