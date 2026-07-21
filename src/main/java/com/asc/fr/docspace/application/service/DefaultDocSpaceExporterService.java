package com.asc.fr.docspace.application.service;

import com.asc.fr.docspace.application.port.input.DocSpaceExporterService;
import com.asc.fr.docspace.application.port.input.DocSpaceTenantService;
import com.asc.fr.docspace.application.port.input.DocSpaceUserAccountService;
import com.asc.fr.docspace.application.port.input.transfer.ExportFileCommand;
import com.asc.fr.docspace.application.port.input.transfer.UploadFileCommand;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceFileUploadService;
import com.asc.fr.docspace.application.port.output.docspace.transfer.DocSpaceUploadFileCommand;
import com.asc.fr.docspace.application.port.output.fr.FineExportService;
import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.asc.fr.docspace.domain.docspace.DocSpaceUploadedFile;
import com.asc.fr.docspace.domain.fr.FineSession;
import com.google.inject.Inject;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @__(@Inject))
public final class DefaultDocSpaceExporterService implements DocSpaceExporterService {
  private final FineExportService fineExportService;
  private final DocSpaceTenantService tenantService;
  private final DocSpaceFileUploadService fileUploadService;
  private final DocSpaceUserAccountService userAccountService;

  private static String buildFilename(String name) {
    String safe = name.replaceAll("[^\\p{L}\\p{N}_\\-]", "_").replaceAll("_+", "_");
    if (safe.length() > 60) safe = safe.substring(0, 60);
    String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    return "fine-" + safe + "-" + stamp + ".xlsx";
  }

  @Override
  public DocSpaceUploadedFile upload(UploadFileCommand command) throws IOException {
    if (!tenantService.isConfigured())
      throw new IOException(
          "DocSpace is not configured. Ask your administrator to configure it first.");

    DocSpaceAccountCredentials credentials = userAccountService.credentials(command.getUserName());
    if (!credentials.isComplete())
      throw new IOException("Sign in to DocSpace from the platform menu first.");

    return fileUploadService.upload(
        DocSpaceUploadFileCommand.builder()
            .docSpaceUrl(new URL(tenantService.docSpaceUrl()))
            .folderId("@my")
            .fileName(command.getFileName())
            .content(command.getContent())
            .build(),
        credentials);
  }

  @Override
  public DocSpaceUploadedFile export(ExportFileCommand command, FineSession session)
      throws IOException {
    try {
      byte[] rawExcel = fineExportService.downloadExport(command.getOperationId(), session);
      String filename = buildFilename(command.getReportName());
      return upload(
          UploadFileCommand.builder()
              .userName(command.getUserName())
              .fileName(filename)
              .content(rawExcel)
              .build());
    } catch (IOException e) {
      throw new IOException("FineBI file export has failed: " + e.getMessage());
    }
  }
}
