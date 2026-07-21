package com.asc.fr.docspace.adapters.input.web.export.handler;

import com.asc.fr.docspace.PluginManifest;
import com.asc.fr.docspace.adapters.input.web.JsonHttpHandler;
import com.asc.fr.docspace.adapters.input.web.RequestUser;
import com.asc.fr.docspace.adapters.input.web.export.transfer.UploadedFileResponse;
import com.asc.fr.docspace.application.exception.BadRequestStatusException;
import com.asc.fr.docspace.application.port.input.DocSpaceExporterService;
import com.asc.fr.docspace.application.port.input.transfer.UploadFileCommand;
import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.third.springframework.web.bind.annotation.RequestMethod;
import com.google.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.servlet.http.HttpServletRequest;

/** Receives an XLSX export from the dashboard and uploads it to DocSpace. */
public class ExportUploadHttpHandler extends JsonHttpHandler {
  private static final int BUFFER_SIZE = 8192;

  private final DocSpaceExporterService export;

  @Inject
  public ExportUploadHttpHandler(DocSpaceExporterService export) {
    super(RequestMethod.POST, PluginManifest.get().endpoints.exportUpload);
    this.export = export;
  }

  private static byte[] readBody(HttpServletRequest request) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try (InputStream in = request.getInputStream()) {
      byte[] chunk = new byte[BUFFER_SIZE];
      int read;
      while ((read = in.read(chunk)) >= 0) if (read > 0) buffer.write(chunk, 0, read);
    }

    return buffer.toByteArray();
  }

  private static String resolveFilename(HttpServletRequest request) {
    String fromParam = request.getParameter("filename");
    if (fromParam != null && !fromParam.trim().isEmpty()) return fromParam.trim();

    String reportId = request.getParameter("reportId");
    String widgetId = request.getParameter("widgetId");
    String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    if (reportId != null && !reportId.trim().isEmpty()) {
      String name = "finebi-" + reportId.trim();
      if (widgetId != null && !widgetId.trim().isEmpty()) name = name + "-" + widgetId.trim();

      return name + "-" + stamp + ".xlsx";
    }

    return "fine-export-" + stamp + ".xlsx";
  }

  @Override
  @ExecuteFunctionRecord
  protected Object handleJson(HttpServletRequest request) throws Exception {
    byte[] content = readBody(request);
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
