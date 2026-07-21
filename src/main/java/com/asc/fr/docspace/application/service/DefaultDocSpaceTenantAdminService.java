package com.asc.fr.docspace.application.service;

import com.asc.fr.docspace.application.port.input.DocSpaceTenantAdminService;
import com.asc.fr.docspace.domain.DocSpaceTenantService;
import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.asc.fr.docspace.domain.docspace.DocSpaceTenantConfiguration;
import com.google.inject.Inject;
import java.io.IOException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @__(@Inject))
public final class DefaultDocSpaceTenantAdminService implements DocSpaceTenantAdminService {
  private final DocSpaceTenantService tenantService;

  @Override
  public void save(URL docSpaceUrl, DocSpaceAccountCredentials admin) throws IOException {
    tenantService.save(new DocSpaceTenantConfiguration(docSpaceUrl.getValue(), admin));
  }

  @Override
  public void reset() throws IOException {
    tenantService.clear();
  }
}
