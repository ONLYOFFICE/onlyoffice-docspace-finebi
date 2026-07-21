package com.asc.fr.docspace.adapters.input.web.tenant.handler;

import com.asc.fr.docspace.PluginManifest;
import com.asc.fr.docspace.adapters.resource.ResourceLoader;
import com.asc.fr.docspace.application.port.input.DocSpaceTenantService;
import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.docspace.DocSpaceSdk;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Renders the two HTML shells the plugin serves: the SPA page (frontend bundle inlined, DocSpace
 * SDK script tag when a tenant is configured) and the plain error page. Everything the SPA needs
 * beyond the shell comes from the /session endpoint — see {@link SessionConfigFactory}.
 */
@Singleton
public final class PageRenderer {
  private final String css;
  private final String script;

  private final DocSpaceTenantService tenantService;
  private final TemplateEngine templateEngine;

  @Inject
  PageRenderer(DocSpaceTenantService tenantService, ResourceLoader resources) {
    this.tenantService = tenantService;
    PluginManifest manifest = PluginManifest.get();

    try {
      this.css = resources.text(manifest.assets.navigation.style);
      this.script = resources.text(manifest.assets.navigation.script);
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
  }

  public String render() {
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

  public String renderError(String message) {
    Context ctx = new Context();
    ctx.setVariable("message", message != null ? message : "");
    return templateEngine.process("error", ctx);
  }
}
