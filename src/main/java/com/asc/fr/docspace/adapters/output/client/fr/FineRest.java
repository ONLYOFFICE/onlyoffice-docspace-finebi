package com.asc.fr.docspace.adapters.output.client.fr;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;
import retrofit2.http.Url;

/**
 * Declarative FineBI decision REST surface.
 *
 * <p>Responses are returned as raw {@link ResponseBody}: FineBI's envelope is quirky
 * (string-or-bool {@code success}, identifiers on the root or under {@code data}, {@code
 * TokenNotExistException} sniffing), so parsing stays in {@code FineEnvelope}/{@code
 * FineResponses}. Session auth is the {@code fine_auth_token} query plus the FineBI cookie.
 * Absolute URLs come via {@link Url} because the decision base is resolved per request.
 */
public interface FineRest {
  @POST
  @Multipart
  @Headers({"Accept: application/json", "X-Requested-With: XMLHttpRequest"})
  Call<ResponseBody> uploadAttachment(
      @Url String url,
      @Query("filename") String filename,
      @Query("fine_auth_token") String token,
      @Header("Cookie") String cookie,
      @Part MultipartBody.Part file);

  @GET
  @Headers({"Accept: application/json", "X-Requested-With: XMLHttpRequest"})
  Call<ResponseBody> get(
      @Url String url, @Query("fine_auth_token") String token, @Header("Cookie") String cookie);

  @POST
  @Headers({"Accept: application/json", "X-Requested-With: XMLHttpRequest"})
  Call<ResponseBody> postJson(
      @Url String url,
      @Query("fine_auth_token") String token,
      @Header("Cookie") String cookie,
      @Body Object body);
}
