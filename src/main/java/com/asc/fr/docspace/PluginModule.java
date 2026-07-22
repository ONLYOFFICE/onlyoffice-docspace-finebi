package com.asc.fr.docspace;

import com.asc.fr.docspace.adapters.input.web.debug.DebugDashboardHttpHandler;
import com.asc.fr.docspace.adapters.input.web.export.handler.ExportConfigHttpHandler;
import com.asc.fr.docspace.adapters.input.web.export.handler.ExportTriggerHttpHandler;
import com.asc.fr.docspace.adapters.input.web.export.handler.ExportUploadHttpHandler;
import com.asc.fr.docspace.adapters.input.web.imports.handler.FoldersHttpHandler;
import com.asc.fr.docspace.adapters.input.web.imports.handler.ImportHttpHandler;
import com.asc.fr.docspace.adapters.input.web.imports.handler.SynchronizationEventBroadcaster;
import com.asc.fr.docspace.adapters.input.web.imports.handler.SynchronizationEventsHttpHandler;
import com.asc.fr.docspace.adapters.input.web.imports.handler.WebhookHttpHandler;
import com.asc.fr.docspace.adapters.input.web.imports.handler.WebhookRegisterHttpHandler;
import com.asc.fr.docspace.adapters.input.web.tenant.handler.AdminSettingsHttpHandler;
import com.asc.fr.docspace.adapters.input.web.tenant.handler.DocSpacePageHttpHandler;
import com.asc.fr.docspace.adapters.input.web.tenant.handler.LoginHttpHandler;
import com.asc.fr.docspace.adapters.input.web.tenant.handler.LogoutHttpHandler;
import com.asc.fr.docspace.adapters.input.web.tenant.handler.ResetHttpHandler;
import com.asc.fr.docspace.adapters.input.web.tenant.handler.SessionConfigHttpHandler;
import com.asc.fr.docspace.adapters.input.web.tenant.handler.SetupHttpHandler;
import com.asc.fr.docspace.adapters.output.client.docspace.DocSpaceAuthenticationClient;
import com.asc.fr.docspace.adapters.output.client.docspace.DocSpaceCspClient;
import com.asc.fr.docspace.adapters.output.client.docspace.DocSpaceFileClient;
import com.asc.fr.docspace.adapters.output.client.docspace.DocSpaceRest;
import com.asc.fr.docspace.adapters.output.client.docspace.DocSpaceWebhookClient;
import com.asc.fr.docspace.adapters.output.client.fr.FineDataClient;
import com.asc.fr.docspace.adapters.output.client.fr.FineExportClient;
import com.asc.fr.docspace.adapters.output.client.fr.FineRest;
import com.asc.fr.docspace.adapters.output.client.http.HttpClients;
import com.asc.fr.docspace.adapters.output.client.http.RedirectingDownloader;
import com.asc.fr.docspace.adapters.output.client.http.RetrofitFactory;
import com.asc.fr.docspace.adapters.output.persistence.FineUnitOfWork;
import com.asc.fr.docspace.adapters.output.persistence.service.FineDocSpaceSynchronizationService;
import com.asc.fr.docspace.adapters.output.persistence.service.FineDocSpaceTenantService;
import com.asc.fr.docspace.adapters.output.persistence.service.FineDocSpaceUserAccountService;
import com.asc.fr.docspace.adapters.output.service.*;
import com.asc.fr.docspace.adapters.resource.DefaultResourceLoader;
import com.asc.fr.docspace.adapters.resource.ResourceLoader;
import com.asc.fr.docspace.application.job.JobSchedule;
import com.asc.fr.docspace.application.job.SyncLinkJobSchedule;
import com.asc.fr.docspace.application.job.SyncLinkReconciliationClusterJob;
import com.asc.fr.docspace.application.job.WebhookReconciliationClusterJob;
import com.asc.fr.docspace.application.port.input.DocSpaceExporterService;
import com.asc.fr.docspace.application.port.input.DocSpaceImporterService;
import com.asc.fr.docspace.application.port.input.DocSpaceOriginService;
import com.asc.fr.docspace.application.port.input.DocSpaceTenantAdminService;
import com.asc.fr.docspace.application.port.input.DocSpaceTenantService;
import com.asc.fr.docspace.application.port.input.DocSpaceUserAccountService;
import com.asc.fr.docspace.application.port.input.PageSelectorService;
import com.asc.fr.docspace.application.port.input.ScheduledClusterJob;
import com.asc.fr.docspace.application.port.input.ScheduledJob;
import com.asc.fr.docspace.application.port.input.SynchronizationService;
import com.asc.fr.docspace.application.port.output.CachingService;
import com.asc.fr.docspace.application.port.output.ClusterLockService;
import com.asc.fr.docspace.application.port.output.IUnitOfWork;
import com.asc.fr.docspace.application.port.output.SynchronizationEventPublisher;
import com.asc.fr.docspace.application.port.output.TaskSchedulerService;
import com.asc.fr.docspace.application.port.output.WebhookRegistrar;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceAuthenticator;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceCspService;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceFileDownloadService;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceFileRetrievalService;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceFileUploadService;
import com.asc.fr.docspace.application.port.output.docspace.DocSpacePathService;
import com.asc.fr.docspace.application.port.output.docspace.DocSpaceSecretGenerator;
import com.asc.fr.docspace.application.port.output.fr.FineAttachmentService;
import com.asc.fr.docspace.application.port.output.fr.FineDatasetService;
import com.asc.fr.docspace.application.port.output.fr.FineEncryptionService;
import com.asc.fr.docspace.application.port.output.fr.FineExportService;
import com.asc.fr.docspace.application.port.output.fr.FineFolderService;
import com.asc.fr.docspace.application.port.output.fr.FineSessionFactory;
import com.asc.fr.docspace.application.service.DefaultDocSpaceExporterService;
import com.asc.fr.docspace.application.service.DefaultDocSpaceImporterService;
import com.asc.fr.docspace.application.service.DefaultDocSpaceOriginService;
import com.asc.fr.docspace.application.service.DefaultDocSpaceTenantAdminService;
import com.asc.fr.docspace.application.service.DefaultDocSpaceTenantService;
import com.asc.fr.docspace.application.service.DefaultDocSpaceUserAccountService;
import com.asc.fr.docspace.application.service.DefaultPageSelectorService;
import com.asc.fr.docspace.application.service.DefaultSynchronizationService;
import com.asc.fr.docspace.domain.SynchronizationLinkRegistry;
import com.asc.fr.docspace.domain.SynchronizationSettings;
import com.fr.decision.fun.HttpHandler;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import okhttp3.OkHttpClient;

final class PluginModule extends AbstractModule {
  @Provides
  @Singleton
  IUnitOfWork unitOfWork() {
    return FineUnitOfWork.builder().build();
  }

  @Provides
  @Singleton
  @Named("docSpace")
  OkHttpClient docSpaceHttp() {
    return HttpClients.docSpace();
  }

  @Provides
  @Singleton
  @Named("fineBi")
  OkHttpClient fineBiHttp() {
    return HttpClients.fine();
  }

  @Provides
  @Singleton
  DocSpaceRest docSpaceApi(@Named("docSpace") OkHttpClient http) {
    return new RetrofitFactory(http).create(DocSpaceRest.class);
  }

  @Provides
  @Singleton
  FineRest fineBiApi(@Named("fineBi") OkHttpClient http) {
    return new RetrofitFactory(http).create(FineRest.class);
  }

  @Provides
  @Singleton
  DocSpaceAuthenticator authenticator(DocSpaceRest api) {
    return new DocSpaceAuthenticationClient(api);
  }

  @Provides
  @Singleton
  DocSpaceFileClient docSpaceFiles(
      DocSpaceRest api,
      DocSpaceAuthenticator authenticator,
      DocSpacePathService paths,
      @Named("docSpace") OkHttpClient http) {
    return new DocSpaceFileClient(api, authenticator, paths, new RedirectingDownloader(http));
  }

  @Provides
  @Singleton
  DocSpaceWebhookClient webhooks(DocSpaceRest api, DocSpaceAuthenticator authenticator) {
    return new DocSpaceWebhookClient(api, authenticator);
  }

  @Provides
  @Singleton
  DocSpaceCspClient csp(DocSpaceRest api) {
    return new DocSpaceCspClient(api);
  }

  @Provides
  @Singleton
  RedirectingDownloader fineBiDownloader(@Named("fineBi") OkHttpClient http) {
    return new RedirectingDownloader(http);
  }

  @Provides
  @Singleton
  FineDataClient fineData(FineRest api) {
    return new FineDataClient(api);
  }

  @Provides
  @Singleton
  FineExportClient fineExport(RedirectingDownloader downloader) {
    return new FineExportClient(downloader);
  }

  @Provides
  @Singleton
  DefaultDocSpaceTenantService tenantService(
      com.asc.fr.docspace.domain.DocSpaceTenantService tenants) {
    return new DefaultDocSpaceTenantService(tenants);
  }

  private void bindInfrastructure() {
    bind(CachingService.class).to(FineCachingService.class).in(Singleton.class);
    bind(FineEncryptionService.class).to(FineStorageEncryptorsService.class).in(Singleton.class);
    bind(TaskSchedulerService.class).to(FineTaskSchedulerService.class).in(Singleton.class);
    bind(ClusterLockService.class).to(FineClusterLockService.class).in(Singleton.class);
    bind(FineSessionFactory.class).to(FinePlatformSessionFactory.class).in(Singleton.class);
    bind(DocSpaceSecretGenerator.class)
        .to(DocSpaceWebhookSecretGenerator.class)
        .in(Singleton.class);
    bind(DocSpacePathService.class).to(DefaultDocSpacePathService.class).in(Singleton.class);
    bind(FineExportService.class).to(FineExportClient.class).in(Singleton.class);
    bind(PageSelectorService.class).to(DefaultPageSelectorService.class).in(Singleton.class);
    bind(ResourceLoader.class).to(DefaultResourceLoader.class).in(Singleton.class);
  }

  private void bindDomainServices() {
    bind(com.asc.fr.docspace.domain.DocSpaceTenantService.class)
        .to(FineDocSpaceTenantService.class)
        .in(Singleton.class);
    bind(com.asc.fr.docspace.domain.DocSpaceUserAccountService.class)
        .to(FineDocSpaceUserAccountService.class)
        .in(Singleton.class);
    bind(FineDocSpaceSynchronizationService.class).in(Singleton.class);
    bind(SynchronizationLinkRegistry.class).to(FineDocSpaceSynchronizationService.class);
    bind(SynchronizationSettings.class).to(FineDocSpaceSynchronizationService.class);
  }

  private void bindApplicationServices() {
    bind(SynchronizationEventBroadcaster.class).in(Singleton.class);
    bind(SynchronizationEventPublisher.class).to(SynchronizationEventBroadcaster.class);

    bind(DocSpaceTenantService.class).to(DefaultDocSpaceTenantService.class).in(Singleton.class);
    bind(DocSpaceTenantAdminService.class)
        .to(DefaultDocSpaceTenantAdminService.class)
        .in(Singleton.class);
    bind(DocSpaceOriginService.class).to(DefaultDocSpaceOriginService.class).in(Singleton.class);
    bind(DocSpaceUserAccountService.class)
        .to(DefaultDocSpaceUserAccountService.class)
        .in(Singleton.class);
    bind(DocSpaceImporterService.class)
        .to(DefaultDocSpaceImporterService.class)
        .in(Singleton.class);
    bind(DocSpaceExporterService.class)
        .to(DefaultDocSpaceExporterService.class)
        .in(Singleton.class);
    bind(SynchronizationService.class).to(DefaultSynchronizationService.class).in(Singleton.class);

    bind(WebhookRegistrar.class).to(DocSpaceWebhookClient.class).in(Singleton.class);
    bind(DocSpaceCspService.class).to(DocSpaceCspClient.class).in(Singleton.class);

    bind(DocSpaceFileUploadService.class).to(DocSpaceFileClient.class);
    bind(DocSpaceFileDownloadService.class).to(DocSpaceFileClient.class);
    bind(DocSpaceFileRetrievalService.class).to(DocSpaceFileClient.class);

    bind(FineAttachmentService.class).to(FineDataClient.class);
    bind(FineFolderService.class).to(FineDataClient.class);
    bind(FineDatasetService.class).to(FineDataClient.class);
  }

  @Provides
  @Singleton
  JobSchedule webhookReconciliationSchedule() {
    PluginManifest.Schedule s = PluginManifest.get().schedulers.webhookReconciliation;
    return new JobSchedule(s.initialDelayMillis, s.periodMillis);
  }

  @Provides
  @Singleton
  SyncLinkJobSchedule syncLinkReconciliationSchedule() {
    PluginManifest.SyncLinkSchedule s = PluginManifest.get().schedulers.syncLinkReconciliation;
    return new SyncLinkJobSchedule(s.initialDelayMillis, s.periodMillis, s.staleAfterMillis);
  }

  private void bindScheduledJobs() {
    Multibinder.newSetBinder(binder(), ScheduledJob.class);

    Multibinder<ScheduledClusterJob> clusterJobs =
        Multibinder.newSetBinder(binder(), ScheduledClusterJob.class);
    clusterJobs.addBinding().to(WebhookReconciliationClusterJob.class).in(Singleton.class);
    clusterJobs.addBinding().to(SyncLinkReconciliationClusterJob.class).in(Singleton.class);
  }

  private void bindHttpHandlers() {
    Multibinder<HttpHandler> web = Multibinder.newSetBinder(binder(), HttpHandler.class);
    // Tenant pages and auth
    web.addBinding().to(DocSpacePageHttpHandler.class);
    web.addBinding().to(AdminSettingsHttpHandler.class);
    web.addBinding().to(DebugDashboardHttpHandler.class);
    web.addBinding().to(SessionConfigHttpHandler.class);
    web.addBinding().to(SetupHttpHandler.class);
    web.addBinding().to(LoginHttpHandler.class);
    web.addBinding().to(LogoutHttpHandler.class);
    web.addBinding().to(ResetHttpHandler.class);

    // Import and webhook-driven sync
    web.addBinding().to(ImportHttpHandler.class);
    web.addBinding().to(FoldersHttpHandler.class);
    web.addBinding().to(WebhookHttpHandler.class);
    web.addBinding().to(WebhookHttpHandler.HeadHandler.class);
    web.addBinding().to(WebhookHttpHandler.GetHandler.class);
    web.addBinding().to(WebhookRegisterHttpHandler.class);
    web.addBinding().to(SynchronizationEventsHttpHandler.class);

    // Export
    web.addBinding().to(ExportConfigHttpHandler.class);
    web.addBinding().to(ExportUploadHttpHandler.class);
    web.addBinding().to(ExportTriggerHttpHandler.class);
  }

  @Override
  protected void configure() {
    bindInfrastructure();
    bindDomainServices();
    bindApplicationServices();
    bindScheduledJobs();
    bindHttpHandlers();

    bind(FineScheduledJobRunner.class).asEagerSingleton();
  }
}
