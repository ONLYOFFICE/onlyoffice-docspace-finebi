package com.asc.fr.docspace.adapters.output.client.http;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Executes Retrofit {@link Call}s and unwraps the {@link Response}, turning a non-2xx status into
 * an {@link IOException} with the method, URL, status and an abbreviated error body. Centralises
 * the {@code requireOk} logic the two hand-rolled transports used to duplicate.
 */
public final class Calls {
  private Calls() {}

  private static IOException statusFailure(Call<?> call, int code) {
    return new IOException(
        call.request().method() + " " + call.request().url() + " failed with status code " + code);
  }

  /**
   * Enqueues {@code call} and completes with its {@link Response}. Only a transport failure (no
   * HTTP response at all) completes the future exceptionally; a non-2xx status is left to the
   * caller to interpret, mirroring {@code call.execute()} returning an unsuccessful {@link
   * Response}.
   */
  public static <T> CompletableFuture<Response<T>> responseAsync(Call<T> call) {
    CompletableFuture<Response<T>> future = new CompletableFuture<>();
    call.enqueue(
        new Callback<T>() {
          @Override
          public void onResponse(Call<T> c, Response<T> response) {
            future.complete(response);
          }

          @Override
          public void onFailure(Call<T> c, Throwable t) {
            future.completeExceptionally(t);
          }
        });
    return future;
  }

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

  /** Non-blocking counterpart of {@link #string(Call)}. */
  public static CompletableFuture<String> stringAsync(Call<ResponseBody> call) {
    return responseAsync(call)
        .thenApply(
            response -> {
              if (!response.isSuccessful())
                throw new CompletionException(statusFailure(call, response.code()));
              try (ResponseBody responseBody = response.body()) {
                return responseBody == null ? "" : responseBody.string();
              } catch (IOException e) {
                throw new CompletionException(e);
              }
            });
  }

  /** Reads the response body as a UTF-8 string (empty when absent). */
  public static String string(Call<ResponseBody> call) throws IOException {
    try (ResponseBody responseBody = body(call)) {
      return responseBody == null ? "" : responseBody.string();
    }
  }
}
