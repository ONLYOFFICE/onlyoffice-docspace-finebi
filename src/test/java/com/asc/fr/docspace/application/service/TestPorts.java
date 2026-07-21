package com.asc.fr.docspace.application.service;

import com.asc.fr.docspace.application.exception.DatasetAbsentException;
import com.asc.fr.docspace.application.port.output.SynchronizationEventPublisher;
import com.asc.fr.docspace.application.port.output.TaskSchedulerService;
import com.asc.fr.docspace.application.port.output.WebhookRegistrar;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceCspService;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceFileDownloadService;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceFileUploadService;
import com.asc.fr.docspace.application.port.output.docspace.transfer.DocSpaceDownloadFileCommand;
import com.asc.fr.docspace.application.port.output.docspace.transfer.DocSpaceDownloadFileFromUrlCommand;
import com.asc.fr.docspace.application.port.output.docspace.transfer.DocSpaceUploadFileCommand;
import com.asc.fr.docspace.application.port.output.fr.FineAttachmentService;
import com.asc.fr.docspace.application.port.output.fr.FineDatasetService;
import com.asc.fr.docspace.application.port.output.fr.FineFolderService;
import com.asc.fr.docspace.application.port.output.fr.FineSessionFactory;
import com.asc.fr.docspace.application.port.output.fr.transfer.FineCreateDatasetCommand;
import com.asc.fr.docspace.application.port.output.fr.transfer.FineRefreshDatasetCommand;
import com.asc.fr.docspace.application.port.output.fr.transfer.FineReplaceDatasetCommand;
import com.asc.fr.docspace.application.port.output.fr.transfer.FineUploadAttachmentCommand;
import com.asc.fr.docspace.domain.DocSpaceTenantService;
import com.asc.fr.docspace.domain.DocSpaceUserAccountService;
import com.asc.fr.docspace.domain.SynchronizationService;
import com.asc.fr.docspace.domain.common.FileSynchronizationRecord;
import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.asc.fr.docspace.domain.docspace.DocSpaceRawFile;
import com.asc.fr.docspace.domain.docspace.DocSpaceTenantConfiguration;
import com.asc.fr.docspace.domain.docspace.DocSpaceUploadedFile;
import com.asc.fr.docspace.domain.fr.FineAttachment;
import com.asc.fr.docspace.domain.fr.FineFolder;
import com.asc.fr.docspace.domain.fr.FineSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TestPorts {
  private TestPorts() {}

  static final class InMemoryDocSpaceTenantService implements DocSpaceTenantService {
    DocSpaceTenantConfiguration config = DocSpaceTenantConfiguration.empty();

    @Override
    public DocSpaceTenantConfiguration load() {
      return config;
    }

    @Override
    public void save(DocSpaceTenantConfiguration config) {
      this.config = config;
    }

    @Override
    public void clear() {
      this.config = DocSpaceTenantConfiguration.empty();
    }
  }

  static final class InMemoryDocSpaceUserAccountService implements DocSpaceUserAccountService {
    final Map<String, DocSpaceAccountCredentials> credentials = new HashMap<>();

    @Override
    public DocSpaceAccountCredentials credentials(String username) {
      return credentials.getOrDefault(username, DocSpaceAccountCredentials.empty());
    }

    @Override
    public void saveCredentials(String username, DocSpaceAccountCredentials value) {
      credentials.put(username, value);
    }

    @Override
    public void clear(String username) {
      credentials.remove(username);
    }

    @Override
    public void clearAll() {
      credentials.clear();
    }
  }

  static final class EmptyDocSpaceCspService implements DocSpaceCspService {
    @Override
    public List<String> allowedDomains(URL docSpaceUrl) {
      return Collections.emptyList();
    }
  }

  static final class InMemorySynchronizationService implements SynchronizationService {
    final Map<String, FileSynchronizationRecord> entries = new LinkedHashMap<>();
    String callbackUrl = "";
    String secret = "TestSecret123";

    @Override
    public void put(String fileId, FileSynchronizationRecord entry) {
      entries.put(fileId, entry);
    }

    @Override
    public FileSynchronizationRecord find(String fileId) {
      return entries.get(fileId);
    }

    @Override
    public void remove(String fileId) {
      entries.remove(fileId);
    }

    @Override
    public Map<String, FileSynchronizationRecord> entries() {
      return entries;
    }

    @Override
    public void storeCallbackUrl(String url) {
      this.callbackUrl = url;
    }

    @Override
    public String loadCallbackUrl() {
      return callbackUrl;
    }

    @Override
    public String ensureSecret() {
      return secret;
    }

    @Override
    public String loadSecret() {
      return secret;
    }
  }

  static final class StubFileStorage
      implements DocSpaceFileUploadService, DocSpaceFileDownloadService {
    DocSpaceRawFile next = new DocSpaceRawFile("data.xlsx", new byte[] {1, 2, 3});
    IOException failWith;
    String lastDownloadedFileId;

    @Override
    public DocSpaceUploadedFile upload(
        DocSpaceUploadFileCommand command, DocSpaceAccountCredentials credentials) {
      return new DocSpaceUploadedFile("f1", command.getFileName(), command.getFolderId());
    }

    @Override
    public DocSpaceRawFile download(
        DocSpaceDownloadFileCommand command, DocSpaceAccountCredentials credentials)
        throws IOException {
      if (failWith != null) throw failWith;
      lastDownloadedFileId = command.getFileId();
      return next;
    }

    @Override
    public DocSpaceRawFile downloadFromUrl(
        DocSpaceDownloadFileFromUrlCommand command, DocSpaceAccountCredentials credentials)
        throws IOException {
      if (failWith != null) throw failWith;
      String hint = command.getFileNameHint();
      String filename = hint == null || hint.trim().isEmpty() ? next.getFileName() : hint;
      return new DocSpaceRawFile(filename, next.getRawContent());
    }
  }

  static final class RecordingFineService
      implements FineAttachmentService, FineFolderService, FineDatasetService {
    final List<FineUploadAttachmentCommand> uploadAttachmentCalls = new ArrayList<>();
    final List<String> ensureFolderCalls = new ArrayList<>();
    final List<FineCreateDatasetCommand> createDatasetCalls = new ArrayList<>();
    final List<FineReplaceDatasetCommand> replaceDatasetCalls = new ArrayList<>();
    final List<FineRefreshDatasetCommand> refreshDatasetCalls = new ArrayList<>();
    String createdUuid = "uuid-created";
    boolean failReplace;
    boolean datasetAbsent;

    boolean noCalls() {
      return uploadAttachmentCalls.isEmpty()
          && ensureFolderCalls.isEmpty()
          && createDatasetCalls.isEmpty()
          && replaceDatasetCalls.isEmpty()
          && refreshDatasetCalls.isEmpty();
    }

    @Override
    public FineAttachment uploadAttachment(FineUploadAttachmentCommand command, FineSession s) {
      uploadAttachmentCalls.add(command);
      return new FineAttachment("attach-1");
    }

    @Override
    public List<FineFolder> listFolders(FineSession s) {
      return Collections.singletonList(new FineFolder("folder-1", "DocSpace"));
    }

    @Override
    public String ensureFolder(String name, FineSession s) {
      ensureFolderCalls.add(name);
      return "folder-1";
    }

    @Override
    public String createDataset(FineCreateDatasetCommand command, FineSession s) {
      createDatasetCalls.add(command);
      return createdUuid;
    }

    @Override
    public void replaceDataset(
        FineReplaceDatasetCommand command, FineSession s, FineAttachment attachment)
        throws IOException {
      replaceDatasetCalls.add(command);
      if (datasetAbsent) throw new DatasetAbsentException(command.getTableId());
      if (failReplace) throw new IOException("replace failed");
    }

    @Override
    public void refreshDataset(FineRefreshDatasetCommand command, FineSession s) {
      refreshDatasetCalls.add(command);
    }
  }

  static final class ImmediateTaskSchedulerService implements TaskSchedulerService {
    @Override
    public void run(Runnable task) {
      task.run();
    }

    @Override
    public Cancellable schedule(long delayMs, Runnable task) {
      task.run();
      return () -> {};
    }
  }

  static final class RejectingTaskSchedulerService implements TaskSchedulerService {
    int scheduled;

    @Override
    public void run(Runnable task) {
      scheduled++;
    }

    @Override
    public Cancellable schedule(long delayMs, Runnable task) {
      scheduled++;
      return () -> {};
    }
  }

  static final class DeferredTaskSchedulerService implements TaskSchedulerService {
    final List<Runnable> queued = new ArrayList<>();

    @Override
    public void run(Runnable task) {
      queued.add(task);
    }

    @Override
    public Cancellable schedule(long delayMs, Runnable task) {
      queued.add(task);
      return () -> queued.remove(task);
    }

    void flush() {
      List<Runnable> toRun = new ArrayList<>(queued);
      queued.clear();
      toRun.forEach(Runnable::run);
    }
  }

  static final class RecordingWebhookRegistrar implements WebhookRegistrar {
    final List<String> registered = new ArrayList<>();

    @Override
    public void register(
        URL docSpaceUrl, URL callbackUrl, String secret, DocSpaceAccountCredentials credentials) {
      registered.add(callbackUrl.getValue());
    }

    @Override
    public void ensureRegistered(
        URL docSpaceUrl, URL callbackUrl, String secret, DocSpaceAccountCredentials credentials) {
      registered.add(callbackUrl.getValue());
    }
  }

  static final class RecordingPublisher implements SynchronizationEventPublisher {
    final List<String> updated = new ArrayList<>();

    @Override
    public void datasetUpdated(String tableName) {
      updated.add(tableName);
    }

    @Override
    public void tenantReset() {
      updated.add("__tenant_reset__");
    }
  }

  static final class StubSessionFactory implements FineSessionFactory {
    @Override
    public FineSession generateSession(String baseUrl) {
      return new FineSession(baseUrl, "fine_auth_token=fresh");
    }
  }
}
