package com.asc.fr.docspace.adapters.input.web.export.handler;

import com.asc.fr.docspace.PluginManifest;
import com.asc.fr.docspace.adapters.input.web.JsonHttpHandler;
import com.asc.fr.docspace.adapters.input.web.RequestUser;
import com.asc.fr.docspace.adapters.input.web.Requests;
import com.asc.fr.docspace.adapters.input.web.export.transfer.UploadedFileResponse;
import com.asc.fr.docspace.application.exception.BadRequestStatusException;
import com.asc.fr.docspace.application.port.input.DocSpaceExporterService;
import com.asc.fr.docspace.application.port.input.transfer.UploadFileCommand;
import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.third.springframework.web.bind.annotation.RequestMethod;
import com.google.inject.Inject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.servlet.http.HttpServletRequest;

/** Receives an XLSX export from the dashboard and uploads it to DocSpace. */
public class ExportUploadHttpHandler extends JsonHttpHandler {
  private static final int MAX_BODY_BYTES = 10 * 1024 * 1024;

  private final DocSpaceExporterService export;

  @Inject
  public ExportUploadHttpHandler(DocSpaceExporterService export) {
    super(RequestMethod.POST, PluginManifest.get().endpoints.exportUpload);
    this.export = export;
  }

  private static String resolveFilename(HttpServletRequest request) {
    String fromParam = Requests.param(request, "filename");
    if (!fromParam.isEmpty()) return fromParam;

    String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    String reportId = Requests.param(request, "reportId");
    if (reportId.isEmpty()) return "fine-export-" + stamp + ".xlsx";

    String widgetId = Requests.param(request, "widgetId");
    String name = "finebi-" + reportId + (widgetId.isEmpty() ? "" : "-" + widgetId);
    return name + "-" + stamp + ".xlsx";
  }

  @Override
  @ExecuteFunctionRecord
  protected Object handleJson(HttpServletRequest request) throws Exception {
    byte[] content = Requests.body(request, MAX_BODY_BYTES);
    RequestUser user = RequestUser.from(request);
    UploadFileCommand command =
        UploadFileCommand.builder()
            .userName(user.name())
            .fileName(resolveFilename(request))
            .content(content)
            .build();

    if (!command.valid()) throw new BadRequestStatusException("Export file body is empty");

    return UploadedFileResponse.of(export.upload(command));
  }
}
