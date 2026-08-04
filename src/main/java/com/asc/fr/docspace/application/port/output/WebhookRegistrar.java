package com.asc.fr.docspace.application.port.output;

import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import java.io.IOException;

public interface WebhookRegistrar {
  void register(
      URL docSpaceUrl, URL callbackUrl, String secret, DocSpaceAccountCredentials credentials)
      throws IOException;

  void ensureSynced(
      URL docSpaceUrl, URL callbackUrl, String secret, DocSpaceAccountCredentials credentials);

  void ensureRegistered(
      URL docSpaceUrl, URL callbackUrl, String secret, DocSpaceAccountCredentials credentials);
}
