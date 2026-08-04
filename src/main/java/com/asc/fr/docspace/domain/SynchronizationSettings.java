package com.asc.fr.docspace.domain;

import java.io.IOException;

public interface SynchronizationSettings {
  void storeCallbackUrl(String url) throws IOException;

  String loadCallbackUrl();

  void storeDecisionBase(String decisionBase) throws IOException;

  String loadDecisionBase();

  String ensureSecret() throws IOException;

  String loadSecret();

  void storeSecret(String secret) throws IOException;

  void clearSecret() throws IOException;
}
