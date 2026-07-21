package com.asc.fr.docspace.adapters.input.web.export.handler;

import com.asc.fr.docspace.PluginManifest;
import com.asc.fr.docspace.adapters.input.web.JsonHttpHandler;
import com.asc.fr.docspace.adapters.input.web.RequestUser;
import com.asc.fr.docspace.adapters.input.web.Requests;
import com.asc.fr.docspace.adapters.input.web.export.transfer.ExportTriggerRequest;
import com.asc.fr.docspace.adapters.input.web.export.transfer.UploadedFileResponse;
import com.asc.fr.docspace.application.exception.BadRequestStatusException;
import com.asc.fr.docspace.application.port.input.DocSpaceExporterService;
import com.asc.fr.docspace.application.port.input.transfer.ExportFileCommand;
import com.asc.fr.docspace.domain.fr.FineSession;
import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.third.springframework.web.bind.annotation.RequestMethod;
import com.google.inject.Inject;
import javax.servlet.http.HttpServletRequest;

/**
 * Delivers a FineBI-generated Excel export to DocSpace. The frontend first POSTs widget/filter
 * state to FineBI's export endpoint with a client-generated operationId, then calls this endpoint;
 * the export service downloads the produced file and uploads it.
 */
public class ExportTriggerHttpHandler extends JsonHttpHandler {
  private final DocSpaceExporterService export;

  @Inject
  public ExportTriggerHttpHandler(DocSpaceExporterService export) {
    super(RequestMethod.POST, PluginManifest.get().endpoints.exportTrigger);
    this.export = export;
  }

  @Override
  @ExecuteFunctionRecord
  protected Object handleJson(HttpServletRequest request) throws Exception {
    ExportTriggerRequest body = Requests.json(request, ExportTriggerRequest.class);
    String operationId = orEmpty(body.getOperationId());
    String reportName = orEmpty(body.getReportName());

    RequestUser user = RequestUser.from(request);
    ExportFileCommand command =
        ExportFileCommand.builder()
            .userName(user.name())
            .operationId(operationId)
            .reportName(reportName.isEmpty() ? operationId : reportName)
            .build();

    if (!command.valid()) throw new BadRequestStatusException("Operation id is required");

    FineSession session =
        new FineSession(Requests.decisionBase(request), Requests.cookieHeader(request));
    return UploadedFileResponse.of(export.export(command, session));
  }
}
