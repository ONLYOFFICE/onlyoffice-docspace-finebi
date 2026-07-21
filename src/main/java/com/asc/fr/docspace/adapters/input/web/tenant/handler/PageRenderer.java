package com.asc.fr.docspace.adapters.input.web.tenant.handler;

import com.asc.fr.docspace.PluginManifest;
import com.asc.fr.docspace.adapters.input.web.PluginRoutes;
import com.asc.fr.docspace.adapters.input.web.RequestOrigin;
import com.asc.fr.docspace.adapters.input.web.RequestUser;
import com.asc.fr.docspace.adapters.input.web.tenant.transfer.PageConfigResponse;
import com.asc.fr.docspace.application.port.input.DocSpaceTenantService;
import com.asc.fr.docspace.application.port.input.DocSpaceUserAccountService;
import com.asc.fr.docspace.application.port.input.transfer.Page;
import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.asc.fr.docspace.domain.docspace.DocSpaceSdk;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

// TODO: Refactor. SRP
@Singleton
public final class PageRenderer {
  private final String css;
  private final String script;

  private final DocSpaceUserAccountService userAccountService;
  private final DocSpaceTenantService tenantService;
  private final TemplateEngine templateEngine;
  private final ObjectMapper objectMapper;

  @Inject
  PageRenderer(DocSpaceTenantService tenantService, DocSpaceUserAccountService userAccountService) {
    this.tenantService = tenantService;
    this.userAccountService = userAccountService;
    PluginManifest manifest = PluginManifest.get();

    try {
      this.css = loadResource(manifest.assets.navigation.style);
      this.script = loadResource(manifest.assets.navigation.script);
    } catch (IOException e) {
      throw new IllegalStateException("Missing frontend bundle", e);
    }

    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    this.templateEngine = new TemplateEngine();
    this.templateEngine.setTemplateResolver(resolver);
    this.objectMapper =
        new ObjectMapper()
            .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
  }

  // TODO: Extract it into a resource loader class
  private static String loadResource(String path) throws IOException {
    try (InputStream in = PageRenderer.class.getClassLoader().getResourceAsStream(path)) {
      if (in == null) throw new IOException("Missing resource: " + path);
      try (BufferedReader r =
          new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
        return r.lines().collect(Collectors.joining("\n"));
      }
    }
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
    // The settings view shows who is signed in (email only); the embed
    // additionally needs the hash for the SDK session restore.
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

  private String toJson(PageConfigResponse dto) {
    try {
      return objectMapper.writeValueAsString(dto);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Config serialization failed", e);
    }
  }

  public String render(Page page, HttpServletRequest request, RequestUser user) throws IOException {
    Context ctx = new Context();
    String rawUrl = tenantService.docSpaceUrl();
    ctx.setVariable("css", css);
    ctx.setVariable("script", script);
    ctx.setVariable(
        "sdkSrc",
        rawUrl.isEmpty()
            ? null
            : DocSpaceSdk.scriptUrl(new URL(rawUrl), PluginManifest.get().sdkVersion));
    return templateEngine.process("page", ctx);
  }

  /** Page configuration as JSON — the SPA bootstrap payload */
  public String configJson(Page page, HttpServletRequest request, RequestUser user) {
    return toJson(buildConfig(page, request, user));
  }

  public String renderError(String message) {
    Context ctx = new Context();
    ctx.setVariable("message", message != null ? message : "");
    return templateEngine.process("error", ctx);
  }
}
