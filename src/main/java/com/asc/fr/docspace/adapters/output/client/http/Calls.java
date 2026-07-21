package com.asc.fr.docspace.adapters.output.client.http;

import java.io.IOException;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Executes Retrofit {@link Call}s and unwraps the {@link Response}, turning a non-2xx status into
 * an {@link IOException} with the method, URL, status and an abbreviated error body. Centralises
 * the {@code requireOk} logic the two hand-rolled transports used to duplicate.
 */
public final class Calls {
  private Calls() {}

  private static <T> Response<T> execute(Call<T> call) throws IOException {
    Response<T> response = call.execute();
    if (!response.isSuccessful()) {
      String url = call.request().url().toString();
      String method = call.request().method();
      throw new IOException(method + " " + url + " failed with status code " + response.code());
    }

    return response;
  }

  /**
   * @return the deserialized body; throws on transport failure or non-2xx.
   */
  public static <T> T body(Call<T> call) throws IOException {
    Response<T> response = execute(call);
    return response.body();
  }

  /** Executes a call whose body is irrelevant (fire-and-check-status). */
  public static void verify(Call<?> call) throws IOException {
    execute(call);
  }

  /** Reads the response body as a UTF-8 string (empty when absent). */
  public static String string(Call<ResponseBody> call) throws IOException {
    try (ResponseBody responseBody = body(call)) {
      return responseBody == null ? "" : responseBody.string();
    }
  }
}
