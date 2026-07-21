package com.asc.fr.docspace.application.port.input;

import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import java.io.IOException;

public interface DocSpaceUserAccountService {
  boolean hasLogin(String userName);

  DocSpaceAccountCredentials credentials(String userName);

  void saveLogin(String userName, DocSpaceAccountCredentials credentials) throws IOException;

  void clear(String userName) throws IOException;

  void clearAll() throws IOException;
}
