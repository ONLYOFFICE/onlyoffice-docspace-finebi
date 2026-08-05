package com.asc.fr.docspace.adapters.output.client.docspace;

import com.asc.fr.docspace.adapters.output.client.docspace.transfer.DocSpaceEnvelope;
import com.asc.fr.docspace.adapters.output.client.docspace.transfer.response.DocSpaceProfileResponse;
import com.asc.fr.docspace.adapters.output.client.http.Calls;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceAuthenticator;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceProfileService;
import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import java.io.IOException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class DocSpaceProfileClient implements DocSpaceProfileService {
  private final DocSpaceRest rest;
  private final DocSpaceAuthenticator authenticator;

  @Override
  public boolean isAdmin(URL docSpaceUrl, DocSpaceAccountCredentials credentials)
      throws IOException {
    String bearer = authenticator.bearer(docSpaceUrl, credentials);

    DocSpaceProfileResponse profile =
        DocSpaceEnvelope.responseOf(
            Calls.body(rest.self(docSpaceUrl.getValue() + Paths.PEOPLE_SELF.path(), bearer)));

    return profile != null && (profile.isAdmin() || profile.isOwner());
  }
}
