package com.asc.fr.docspace.application.service;

import com.asc.fr.docspace.application.exception.DatasetAbsentException;
import com.asc.fr.docspace.application.port.output.CachingService;
import com.asc.fr.docspace.application.port.output.SynchronizationEventPublisher;
import com.asc.fr.docspace.application.port.output.TaskSchedulerService;
import com.asc.fr.docspace.application.port.output.WebhookRegistrar;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceCspService;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceFileDownloadService;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceFileRetrievalService;
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
import com.asc.fr.docspace.domain.SynchronizationLinkRegistry;
import com.asc.fr.docspace.domain.SynchronizationSettings;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

  static final class InMemoryCachingService implements CachingService {
    @Override
    public <K, V> Cache<K, V> create(String name, long ttlSeconds, long maximumEntries) {
      Map<K, V> store = new HashMap<>();
      return new Cache<K, V>() {
        @Override
        public V get(K key) {
          return store.get(key);
        }

        @Override
        public void put(K key, V value) {
          store.put(key, value);
        }

        @Override
        public void invalidate(K key) {
          store.remove(key);
        }

        @Override
        public void invalidateAll() {
          store.clear();
        }
      };
    }
  }

  static final class EmptyDocSpaceCspService implements DocSpaceCspService {
    @Override
    public List<String> allowedDomains(URL docSpaceUrl) {
      return Collections.emptyList();
    }
  }

  static final class RecordingDocSpaceFileRetrievalService implements DocSpaceFileRetrievalService {
    final List<String> existsProbes = new ArrayList<>();
    final Set<String> absentFileIds = new HashSet<>();
    IOException failWith;

    @Override
    public boolean fileExists(URL docSpaceUrl, String fileId, DocSpaceAccountCredentials creds)
        throws IOException {
      existsProbes.add(fileId);
      if (failWith != null) throw failWith;
      return !absentFileIds.contains(fileId);
    }
  }

  static final class InMemorySynchronizationService
      implements SynchronizationLinkRegistry, SynchronizationSettings {
    final Map<String, FileSynchronizationRecord> entries = new LinkedHashMap<>();

    String callbackUrl = "";
    String secret = "TestSecret123";
    String decisionBase = "";
    IOException failPutWith;

    @Override
    public void put(FileSynchronizationRecord entry) throws IOException {
      if (failPutWith != null) throw failPutWith;
      if (entry == null || entry.getTableId().isEmpty()) return;
      entries.put(entry.getTableId(), entry);
    }

    @Override
    public List<FileSynchronizationRecord> findByFile(String fileId) {
      List<FileSynchronizationRecord> matches = new ArrayList<>();
      for (FileSynchronizationRecord entry : entries.values())
        if (entry.getFileId().equals(fileId)) matches.add(entry);
      return matches;
    }

    FileSynchronizationRecord find(String fileId) {
      List<FileSynchronizationRecord> matches = findByFile(fileId);
      return matches.isEmpty() ? null : matches.get(0);
    }

    @Override
    public void remove(String tableId) {
      entries.remove(tableId);
    }

    @Override
    public void removeByFile(String fileId) {
      entries.values().removeIf(entry -> entry.getFileId().equals(fileId));
    }

    @Override
    public void removeAll() {
      entries.clear();
    }

    private List<FileSynchronizationRecord> links(String afterTableId, int limit) {
      String after = afterTableId == null ? "" : afterTableId;
      List<FileSynchronizationRecord> page = new ArrayList<>();
      List<String> keys = new ArrayList<>(entries.keySet());
      Collections.sort(keys);
      for (String key : keys) {
        if (key.compareTo(after) <= 0) continue;
        page.add(entries.get(key));
        if (page.size() >= limit) break;
      }
      return page;
    }

    @Override
    public List<FileSynchronizationRecord> staleLinks(
        long reconciledBefore, String afterTableId, int limit) {
      return links(afterTableId, limit);
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
    public void storeDecisionBase(String decisionBase) {
      this.decisionBase = decisionBase;
    }

    @Override
    public String loadDecisionBase() {
      return decisionBase;
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
    final List<String> datasetExistsProbes = new ArrayList<>();
    final Set<String> absentTableIds = new HashSet<>();
    String createdUuid = "uuid-created";
    boolean failReplace;
    boolean datasetAbsent;
    IOException failExistsWith;

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

    @Override
    public boolean datasetExists(String tableId, String folderId, FineSession s)
        throws IOException {
      datasetExistsProbes.add(tableId);
      if (failExistsWith != null) throw failExistsWith;
      return !absentTableIds.contains(tableId);
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

    @Override
    public Cancellable scheduleAtFixedRate(long initialDelayMs, long periodMs, Runnable task) {
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

    @Override
    public Cancellable scheduleAtFixedRate(long initialDelayMs, long periodMs, Runnable task) {
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

    @Override
    public Cancellable scheduleAtFixedRate(long initialDelayMs, long periodMs, Runnable task) {
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
