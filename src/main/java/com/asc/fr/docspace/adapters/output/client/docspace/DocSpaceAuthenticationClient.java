package com.asc.fr.docspace.adapters.output.client.docspace;

import com.asc.fr.docspace.adapters.output.client.docspace.transfer.DocSpaceEnvelope;
import com.asc.fr.docspace.adapters.output.client.docspace.transfer.request.DocSpaceAuthenticationRequest;
import com.asc.fr.docspace.adapters.output.client.docspace.transfer.response.DocSpaceAuthenticationTokenResponse;
import com.asc.fr.docspace.adapters.output.client.http.Calls;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceAuthenticator;
import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.google.common.base.Strings;
import java.io.IOException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class DocSpaceAuthenticationClient implements DocSpaceAuthenticator {
  private final DocSpaceRest rest;

  @Override
  public String authenticate(URL baseUrl, DocSpaceAccountCredentials credentials)
      throws IOException {
    DocSpaceAuthenticationRequest body =
        new DocSpaceAuthenticationRequest(credentials.getEmail(), credentials.getHash());
    DocSpaceAuthenticationTokenResponse response =
        DocSpaceEnvelope.responseOf(
            Calls.body(rest.authenticate(baseUrl.getValue() + Paths.AUTHENTICATION.path(), body)));

    String token = response == null ? "" : Strings.nullToEmpty(response.getToken());
    if (token.isEmpty()) throw new IOException("DocSpace authentication returned no token");

    return token;
  }

  @Override
  public String bearer(URL baseUrl, DocSpaceAccountCredentials credentials) throws IOException {
    return "Bearer " + authenticate(baseUrl, credentials);
  }
}
