package com.asc.fr.docspace.adapters.output.client.docspace;

import com.asc.fr.docspace.adapters.output.client.docspace.transfer.DocSpaceEnvelope;
import com.asc.fr.docspace.adapters.output.client.docspace.transfer.request.DocSpaceAuthenticationRequest;
import com.asc.fr.docspace.adapters.output.client.docspace.transfer.request.DocSpaceWebhookCreateRequest;
import com.asc.fr.docspace.adapters.output.client.docspace.transfer.request.DocSpaceWebhookUpdateRequest;
import com.asc.fr.docspace.adapters.output.client.docspace.transfer.response.DocSpaceAuthenticationTokenResponse;
import com.asc.fr.docspace.adapters.output.client.docspace.transfer.response.DocSpaceCspResponse;
import com.asc.fr.docspace.adapters.output.client.docspace.transfer.response.DocSpaceProfileResponse;
import com.asc.fr.docspace.adapters.output.client.docspace.transfer.response.DocSpaceWebhookEntryResponse;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Url;

/**
 * Declarative DocSpace REST surface. Every call targets a per-tenant host, so the full URL is
 * supplied via {@link Url} (see {@code Paths}); bearer auth is passed per call. The {@code
 * response}-wrapped bodies deserialize straight into {@link DocSpaceEnvelope}.
 */
public interface DocSpaceRest {
  @POST
  @Headers("Accept: application/json")
  Call<DocSpaceEnvelope<DocSpaceAuthenticationTokenResponse>> authenticate(
      @Url String url, @Body DocSpaceAuthenticationRequest body);

  @GET
  @Headers("Accept: application/json")
  Call<DocSpaceEnvelope<DocSpaceCspResponse>> csp(@Url String url);

  @GET
  @Headers("Accept: application/json")
  Call<DocSpaceEnvelope<DocSpaceProfileResponse>> self(
      @Url String url, @Header("Authorization") String bearer);

  @GET
  @Headers("Accept: application/json")
  Call<DocSpaceEnvelope<List<DocSpaceWebhookEntryResponse>>> listWebhooks(
      @Url String url, @Header("Authorization") String bearer);

  @POST
  @Headers("Accept: application/json")
  Call<ResponseBody> createWebhook(
      @Url String url,
      @Header("Authorization") String bearer,
      @Body DocSpaceWebhookCreateRequest body);

  @PUT
  @Headers("Accept: application/json")
  Call<ResponseBody> updateWebhook(
      @Url String url,
      @Header("Authorization") String bearer,
      @Body DocSpaceWebhookUpdateRequest body);

  @GET
  @Headers("Accept: application/json")
  Call<DocSpaceEnvelope<JsonNode>> fileMeta(
      @Url String url, @Header("Authorization") String bearer);

  @GET
  @Headers("Accept: application/json")
  Call<DocSpaceEnvelope<String>> presignedUri(
      @Url String url, @Header("Authorization") String bearer);

  @POST
  @Multipart
  @Headers("Accept: application/json")
  Call<DocSpaceEnvelope<JsonNode>> upload(
      @Url String url, @Header("Authorization") String bearer, @Part MultipartBody.Part file);
}
