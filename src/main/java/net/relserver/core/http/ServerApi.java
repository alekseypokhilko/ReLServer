package net.relserver.core.http;

import net.relserver.core.Constants;
import retrofit2.Call;
import retrofit2.http.GET;

public interface ServerApi {
    @GET(Constants.APPLICATIONS_CONFIG_URL)
    Call<String> applications();
}
