package com.asc.fr.docspace.domain.docspace;

import com.asc.fr.docspace.domain.common.URL;
import lombok.Getter;

@Getter
public final class DocSpaceTenantConfiguration {
  private static final DocSpaceTenantConfiguration NO_TENANT = new DocSpaceTenantConfiguration();

  private final URL url;
  private final DocSpaceAccountCredentials admin;

  private DocSpaceTenantConfiguration() {
    this.url = URL.EMPTY;
    this.admin = DocSpaceAccountCredentials.empty();
  }

  public DocSpaceTenantConfiguration(String url, DocSpaceAccountCredentials admin) {
    this.url = new URL(url);
    this.admin = admin == null ? DocSpaceAccountCredentials.empty() : admin;
  }

  public static DocSpaceTenantConfiguration empty() {
    return NO_TENANT;
  }
}
