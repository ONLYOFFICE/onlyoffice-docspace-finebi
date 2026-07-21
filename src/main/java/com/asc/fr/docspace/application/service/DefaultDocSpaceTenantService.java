package com.asc.fr.docspace.application.service;

import com.asc.fr.docspace.application.port.input.DocSpaceTenantService;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.google.inject.Inject;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @__(@Inject))
public final class DefaultDocSpaceTenantService implements DocSpaceTenantService {
  private final com.asc.fr.docspace.domain.DocSpaceTenantService tenantService;

  @Override
  public boolean isConfigured() {
    return !docSpaceUrl().isEmpty() && adminCredentials().isComplete();
  }

  @Override
  public String docSpaceUrl() {
    return tenantService.load().getUrl().getValue();
  }

  @Override
  public DocSpaceAccountCredentials adminCredentials() {
    return tenantService.load().getAdmin();
  }
}
