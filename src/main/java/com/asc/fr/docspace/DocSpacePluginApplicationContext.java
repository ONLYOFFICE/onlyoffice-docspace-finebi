package com.asc.fr.docspace;

import com.fr.decision.fun.HttpHandler;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import java.util.Set;

/** Plugin composition root: boots Guice and exposes the shared {@link Injector}. */
public final class DocSpacePluginApplicationContext {
  private static final Key<Set<HttpHandler>> HTTP_HANDLERS =
      Key.get(new TypeLiteral<Set<HttpHandler>>() {});

  private static final DocSpacePluginApplicationContext INSTANCE =
      new DocSpacePluginApplicationContext();

  private final Injector injector;

  private DocSpacePluginApplicationContext() {
    injector = Guice.createInjector(Stage.PRODUCTION, new PluginModule());
    injector.getInstance(DocSpacePluginWebhookStartupRunner.class);
  }

  public static DocSpacePluginApplicationContext get() {
    return INSTANCE;
  }

  public HttpHandler[] httpHandlers() {
    return injector.getInstance(HTTP_HANDLERS).toArray(new HttpHandler[0]);
  }
}
