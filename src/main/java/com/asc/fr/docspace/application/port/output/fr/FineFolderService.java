package com.asc.fr.docspace.application.port.output.fr;

import com.asc.fr.docspace.domain.fr.FineFolder;
import com.asc.fr.docspace.domain.fr.FineSession;
import java.io.IOException;
import java.util.List;

public interface FineFolderService {
  List<FineFolder> listFolders(FineSession session) throws IOException;

  String ensureFolder(String name, FineSession session) throws IOException;
}
