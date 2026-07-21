package com.asc.fr.docspace.adapters.output.client.http;

import com.asc.fr.docspace.adapters.format.Json;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public final class RetrofitFactory {
  private static final String PLACEHOLDER_BASE_URL = "http://localhost/";

  private final Retrofit retrofit;

  public RetrofitFactory(OkHttpClient client) {
    this.retrofit =
        new Retrofit.Builder()
            .baseUrl(PLACEHOLDER_BASE_URL)
            .client(client)
            .addConverterFactory(JacksonConverterFactory.create(Json.MAPPER))
            .build();
  }

  public <T> T create(Class<T> service) {
    return retrofit.create(service);
  }
}
