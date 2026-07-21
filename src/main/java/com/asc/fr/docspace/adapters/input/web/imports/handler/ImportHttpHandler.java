package com.asc.fr.docspace.adapters.input.web.imports.handler;

import com.asc.fr.docspace.PluginManifest;
import com.asc.fr.docspace.adapters.input.web.JsonHttpHandler;
import com.asc.fr.docspace.adapters.input.web.RequestUser;
import com.asc.fr.docspace.adapters.input.web.Requests;
import com.asc.fr.docspace.adapters.input.web.imports.transfer.ImportRequest;
import com.asc.fr.docspace.adapters.input.web.imports.transfer.ImportResponse;
import com.asc.fr.docspace.application.exception.UnauthorizedStatusException;
import com.asc.fr.docspace.application.port.input.DocSpaceImporterService;
import com.asc.fr.docspace.application.port.input.DocSpaceUserAccountService;
import com.asc.fr.docspace.application.port.input.transfer.ImportFileCommand;
import com.asc.fr.docspace.domain.fr.FineSession;
import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.third.springframework.web.bind.annotation.RequestMethod;
import com.google.inject.Inject;
import javax.servlet.http.HttpServletRequest;

/**
 * Imports a DocSpace spreadsheet as a FineBI dataset. All orchestration lives in {@link
 * DocSpaceImporterService}; this handler only translates HTTP to the use case.
 */
public class ImportHttpHandler extends JsonHttpHandler {
  private final DocSpaceUserAccountService users;
  private final DocSpaceImporterService docSpaceImporterService;

  @Inject
  public ImportHttpHandler(
      DocSpaceUserAccountService users, DocSpaceImporterService docSpaceImporterService) {
    super(RequestMethod.POST, PluginManifest.get().endpoints.importFile);
    this.users = users;
    this.docSpaceImporterService = docSpaceImporterService;
  }

  @Override
  @ExecuteFunctionRecord
  protected Object handleJson(HttpServletRequest request) throws Exception {
    ImportRequest body = Requests.json(request, ImportRequest.class);
    String fileId = require(body.getFileId(), "fileId");
    RequestUser user = RequestUser.from(request);
    if (!users.hasLogin(user.name()) || !users.credentials(user.name()).isComplete())
      throw new UnauthorizedStatusException();

    ImportFileCommand command =
        ImportFileCommand.builder()
            .userName(user.name())
            .fileId(fileId)
            .fileName(orEmpty(body.getFilename()))
            .viewUrl(orEmpty(body.getViewUrl()))
            .folderId(orEmpty(body.getFolderId()))
            .callbackUrl(ImportRoutes.webhookCallbackUrl(request))
            .build();

    FineSession session =
        new FineSession(Requests.decisionBase(request), Requests.cookieHeader(request));

    return new ImportResponse(docSpaceImporterService.importFile(command, session));
  }
}
