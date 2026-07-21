package com.asc.fr.docspace.adapters.input.web.imports.handler;

import com.asc.fr.docspace.PluginManifest;
import com.asc.fr.docspace.adapters.input.web.JsonHttpHandler;
import com.asc.fr.docspace.adapters.input.web.Requests;
import com.asc.fr.docspace.adapters.input.web.imports.transfer.FoldersResponse;
import com.asc.fr.docspace.application.port.output.fr.FineFolderService;
import com.asc.fr.docspace.domain.fr.FineSession;
import com.fr.third.springframework.web.bind.annotation.RequestMethod;
import com.google.inject.Inject;
import javax.servlet.http.HttpServletRequest;

/**
 * Returns the list of public-data business packages from FineBI so the frontend can let the user
 * pick a destination folder.
 *
 * <p>Response: {"ok":true,"folders":[{"id":"…","name":"…"},…]}
 */
public class FoldersHttpHandler extends JsonHttpHandler {
  private final FineFolderService folders;

  @Inject
  public FoldersHttpHandler(FineFolderService folders) {
    super(RequestMethod.GET, PluginManifest.get().endpoints.folders);
    this.folders = folders;
  }

  @Override
  protected Object handleJson(HttpServletRequest request) throws Exception {
    FineSession session =
        new FineSession(Requests.decisionBase(request), Requests.cookieHeader(request));
    return FoldersResponse.of(folders.listFolders(session));
  }
}
