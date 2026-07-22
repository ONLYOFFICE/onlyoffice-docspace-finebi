package com.asc.fr.docspace.adapters.input.web;

import com.asc.fr.docspace.adapters.resource.ResourceLoader;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Renders the shared placeholder-style error page.
 */
@Singleton
public final class ErrorPageRenderer {
  private final TemplateEngine templateEngine;
  private final String logoSvg;
  private final String owlSvg;

  @Inject
  ErrorPageRenderer(ResourceLoader resources) {
    try {
      this.logoSvg = resources.text("images/horizontal-logo.svg");
      this.owlSvg = resources.text("images/owl.svg");
    } catch (IOException e) {
      throw new IllegalStateException("Missing placeholder SVG assets", e);
    }

    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    this.templateEngine = new TemplateEngine();
    this.templateEngine.setTemplateResolver(resolver);
  }

  public String render(String message) {
    Context ctx = new Context();
    ctx.setVariable("message", message != null ? message : "");
    ctx.setVariable("logoSvg", logoSvg);
    ctx.setVariable("owlSvg", owlSvg);
    return templateEngine.process("error", ctx);
  }
}
