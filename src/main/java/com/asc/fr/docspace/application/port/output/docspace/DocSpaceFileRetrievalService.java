package com.asc.fr.docspace.application.port.output.docspace;

import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface DocSpaceFileRetrievalService {
  Map<String, CompletableFuture<Boolean>> fileExistenceProbes(
      URL docSpaceUrl, Collection<String> fileIds, DocSpaceAccountCredentials credentials)
      throws IOException;
}
