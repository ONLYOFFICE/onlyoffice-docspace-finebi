package com.asc.fr.docspace.application.service;

import com.asc.fr.docspace.application.port.input.DocSpaceUserAccountService;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.google.inject.Inject;
import java.io.IOException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @__(@Inject))
public final class DefaultDocSpaceUserAccountService implements DocSpaceUserAccountService {
  private final com.asc.fr.docspace.domain.DocSpaceUserAccountService accounts;

  private static void requireUsername(String username) throws IOException {
    if (username == null || username.trim().isEmpty())
      throw new IOException("FineBI user is not available in this request");
  }

  @Override
  public boolean hasLogin(String username) {
    return username != null && !username.isEmpty() && accounts.credentials(username).isComplete();
  }

  @Override
  public DocSpaceAccountCredentials credentials(String username) {
    return accounts.credentials(username);
  }

  @Override
  public void saveLogin(String username, DocSpaceAccountCredentials credentials)
      throws IOException {
    requireUsername(username);
    accounts.saveCredentials(username, credentials);
  }

  @Override
  public void clear(String username) throws IOException {
    if (username == null || username.isEmpty()) {
      return;
    }
    accounts.clear(username);
  }

  @Override
  public void clearAll() throws IOException {
    accounts.clearAll();
  }
}
