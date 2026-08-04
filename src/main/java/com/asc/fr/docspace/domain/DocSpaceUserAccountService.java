package com.asc.fr.docspace.domain;

import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import java.io.IOException;

public interface DocSpaceUserAccountService {
  DocSpaceAccountCredentials credentials(String userName);

  String signedInTenantUrl(String userName);

  void saveCredentials(String userName, DocSpaceAccountCredentials credentials, String tenantUrl)
      throws IOException;

  void clear(String userName) throws IOException;

  void clearAll() throws IOException;
}
