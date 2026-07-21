package com.asc.fr.docspace.adapters.output.client.docspace;

import com.asc.fr.docspace.adapters.output.client.docspace.transfer.DocSpaceEnvelope;
import com.asc.fr.docspace.adapters.output.client.docspace.transfer.response.DocSpaceCspResponse;
import com.asc.fr.docspace.adapters.output.client.http.Calls;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceCspService;
import com.asc.fr.docspace.domain.common.URL;
import com.google.common.base.Strings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class DocSpaceCspClient implements DocSpaceCspService {
  private final DocSpaceRest rest;

  @Override
  public List<String> allowedDomains(URL docSpaceUrl) throws IOException {
    DocSpaceEnvelope<DocSpaceCspResponse> envelope =
        Calls.body(rest.csp(docSpaceUrl + Paths.CSP.path()));
    DocSpaceCspResponse dto = envelope == null ? null : envelope.getResponse();
    if (dto == null || dto.getDomains() == null) return Collections.emptyList();

    List<String> result = new ArrayList<>();
    for (String domain : dto.getDomains()) if (!Strings.isNullOrEmpty(domain)) result.add(domain);

    return result;
  }
}
