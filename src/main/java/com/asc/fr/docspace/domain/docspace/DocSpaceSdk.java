package com.asc.fr.docspace.domain.docspace;

import com.asc.fr.docspace.domain.common.URL;

public final class DocSpaceSdk {
  private DocSpaceSdk() {}

  public static String scriptUrl(URL docSpaceUrl, String sdkVersion) {
    return docSpaceUrl.getValue() + "/static/scripts/sdk/" + sdkVersion + "/api.js";
  }
}
