package com.asc.fr.docspace.adapters.input.web.debug;

import com.asc.fr.docspace.adapters.input.web.ErrorPageRenderer;
import com.asc.fr.docspace.adapters.input.web.PluginRoutes;
import com.asc.fr.docspace.domain.SynchronizationLinkRegistry;
import com.asc.fr.docspace.domain.common.FileSynchronizationRecord;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Singleton
public final class DebugDashboardRenderer {
  static final int PAGE_SIZE = 20;
  private static final DateTimeFormatter TIMESTAMP =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

  private final SynchronizationLinkRegistry linkRegistry;
  private final TemplateEngine templateEngine;
  private final ErrorPageRenderer errorPage;

  @Inject
  DebugDashboardRenderer(SynchronizationLinkRegistry linkRegistry, ErrorPageRenderer errorPage) {
    this.linkRegistry = linkRegistry;
    this.errorPage = errorPage;

    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    this.templateEngine = new TemplateEngine();
    this.templateEngine.setTemplateResolver(resolver);
  }

  private static String formatTimestamp(long epochMillis) {
    if (epochMillis <= 0) return "never";
    return TIMESTAMP.format(Instant.ofEpochMilli(epochMillis));
  }

  private static String orDash(String value) {
    return value == null || value.isEmpty() ? "—" : value;
  }

  private static DebugLinkView toView(FileSynchronizationRecord record) {
    String sheetId = record.getSheetId() > 0 ? Integer.toString(record.getSheetId()) : "—";
    return new DebugLinkView(
        record.getTableId(),
        sheetId,
        orDash(record.getContentHash()),
        record.getFileId(),
        formatTimestamp(record.getLastReconciledAt()));
  }

  public String renderError(String message) {
    return errorPage.render(message);
  }

  public String render(HttpServletRequest request, int page) {
    int safePage = Math.max(1, page);
    int offset = (safePage - 1) * PAGE_SIZE;

    List<FileSynchronizationRecord> rows = linkRegistry.listLinks(offset, PAGE_SIZE + 1);

    boolean hasNext = rows.size() > PAGE_SIZE;
    List<FileSynchronizationRecord> pageRows = hasNext ? rows.subList(0, PAGE_SIZE) : rows;

    List<DebugLinkView> views = new ArrayList<>(pageRows.size());
    for (FileSynchronizationRecord record : pageRows) views.add(toView(record));

    Context ctx = new Context();
    ctx.setVariable("links", views);
    ctx.setVariable("page", safePage);
    ctx.setVariable("hasPrev", safePage > 1);
    ctx.setVariable("hasNext", hasNext);
    ctx.setVariable("prevPage", safePage - 1);
    ctx.setVariable("nextPage", safePage + 1);
    ctx.setVariable("baseUrl", PluginRoutes.debugUrl(request));
    return templateEngine.process("debug", ctx);
  }
}
