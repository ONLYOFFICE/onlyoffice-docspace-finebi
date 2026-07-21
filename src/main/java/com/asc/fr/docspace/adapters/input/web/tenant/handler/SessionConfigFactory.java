package com.asc.fr.docspace.adapters.input.web.tenant.handler;

import com.asc.fr.docspace.PluginManifest;
import com.asc.fr.docspace.adapters.input.web.PluginRoutes;
import com.asc.fr.docspace.adapters.input.web.RequestOrigin;
import com.asc.fr.docspace.adapters.input.web.RequestUser;
import com.asc.fr.docspace.adapters.input.web.tenant.transfer.PageConfigResponse;
import com.asc.fr.docspace.application.port.input.DocSpaceTenantService;
import com.asc.fr.docspace.application.port.input.DocSpaceUserAccountService;
import com.asc.fr.docspace.application.port.input.transfer.Page;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

/**
 * Builds the SPA bootstrap payload (mode + nested config, mirrors PluginCoreServerConfiguration on
 * the frontend) and serializes it to JSON.
 */
@Singleton
public final class SessionConfigFactory {
  private final DocSpaceUserAccountService userAccountService;
  private final DocSpaceTenantService tenantService;
  private final ObjectMapper objectMapper;

  @Inject
  SessionConfigFactory(
      DocSpaceTenantService tenantService, DocSpaceUserAccountService userAccountService) {
    this.tenantService = tenantService;
    this.userAccountService = userAccountService;
    this.objectMapper =
        new ObjectMapper()
            .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
  }

  private static String pageMode(Page page) {
    switch (page) {
      case DOCSPACE:
        return "docspace";
      case SETUP:
        return "setup";
      case ADMIN_LOGIN:
        return "admin";
      case ADMIN_SETTINGS:
        return "settings";
      case USER_LOGIN:
        return "user";
      case LOGOUT:
        return "logout";
      case NOT_CONFIGURED:
        return "notconfigured";
      case ACCESS_DENIED:
        return "unauthorized";
      default:
        return "notconfigured";
    }
  }

  private static String pageMessage(Page page) {
    switch (page) {
      case NOT_CONFIGURED:
        return "A FineBI administrator must configure the DocSpace tenant before you can sign in.";
      case ACCESS_DENIED:
        return "Only FineBI administrators can configure DocSpace.";
      default:
        return "";
    }
  }

  private PageConfigResponse buildConfig(Page page, HttpServletRequest request, RequestUser user) {
    String docSpaceUrl = tenantService.docSpaceUrl();
    String pageUrl = PluginRoutes.pageUrl(request);
    DocSpaceAccountCredentials credentials =
        page == Page.DOCSPACE || page == Page.ADMIN_SETTINGS
            ? userAccountService.credentials(user.name())
            : DocSpaceAccountCredentials.empty();
    boolean adminConsole = page == Page.ADMIN_LOGIN || page == Page.ADMIN_SETTINGS;
    return PageConfigResponse.builder()
        .mode(pageMode(page))
        .locations(
            PageConfigResponse.Locations.builder()
                .hostOrigin(page == Page.SETUP ? RequestOrigin.of(request) : "")
                .pluginUrl(pageUrl)
                .loginUrl(PluginRoutes.consoleUrl(request))
                .logoutUrl(pageUrl + "?logout=1")
                .importUrl(PluginRoutes.importAction(request))
                .foldersUrl(PluginRoutes.foldersAction(request))
                .webhookRegistrationUrl(PluginRoutes.webhookRegisterAction(request))
                .eventStreamUrl(PluginRoutes.syncEventsUrl(request))
                .build())
        .credentials(
            PageConfigResponse.Credentials.builder()
                .email(credentials.getEmail())
                .hash(page == Page.DOCSPACE ? credentials.getHash() : "")
                .build())
        .status(
            PageConfigResponse.Status.builder()
                .loginStored(adminConsole && userAccountService.hasLogin(user.name()))
                .isAdmin(user.isAdmin())
                .build())
        .tenant(
            PageConfigResponse.Tenant.builder()
                .docSpaceUrl(docSpaceUrl)
                .sdkVersion(PluginManifest.get().sdkVersion)
                .build())
        .actions(
            PageConfigResponse.Actions.builder()
                .submit(
                    page == Page.SETUP
                        ? PluginRoutes.setupAction(request)
                        : PluginRoutes.loginAction(request))
                .reset(adminConsole ? PluginRoutes.resetAction(request) : "")
                .logout(PluginRoutes.logoutAction(request))
                .build())
        .response(
            PageConfigResponse.Response.builder().message(pageMessage(page)).error("").build())
        .build();
  }

  public String configJson(Page page, HttpServletRequest request, RequestUser user) {
    try {
      return objectMapper.writeValueAsString(buildConfig(page, request, user));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Config serialization failed", e);
    }
  }
}
