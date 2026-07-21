package com.asc.fr.docspace.adapters.output.client.http;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;

/**
 * Builds shared, production-tuned {@link OkHttpClient} instances for the outbound REST adapters.
 * This is infrastructure — it belongs in the adapter layer, not the application layer — and the
 * composition root exposes the instances as Guice singletons.
 *
 * <p>Clients are expensive: one long-lived instance per timeout profile keeps TCP/TLS connections
 * and the pool warm. Defaults favour plugin REST calls: bounded dispatcher, connection pooling, no
 * automatic redirects (callers must detect auth/login redirects), retry on transient connection
 * failures.
 */
public final class HttpClients {
  private static final int DEFAULT_CONNECT_MS = 15_000;
  private static final int DEFAULT_READ_MS = 60_000;

  /** FineBI / DocSpace uploads and downloads often need a longer read window. */
  private static final int LONG_READ_MS = 120_000;

  private static final int MAX_REQUESTS = 64;
  private static final long KEEP_ALIVE_MINUTES = 5;
  private static final int MAX_IDLE_CONNECTIONS = 10;
  private static final int MAX_REQUESTS_PER_HOST = 16;

  private static final OkHttpClient FINE_BI = create(DEFAULT_CONNECT_MS, LONG_READ_MS);
  private static final OkHttpClient DOC_SPACE = create(DEFAULT_CONNECT_MS, LONG_READ_MS);

  private HttpClients() {}

  /** Shared FineBI client (15s connect, 120s read/write). */
  public static OkHttpClient fine() {
    return FINE_BI;
  }

  /** Shared DocSpace client (15s connect, 120s read/write). */
  public static OkHttpClient docSpace() {
    return DOC_SPACE;
  }

  private static OkHttpClient create(int connectMs, int readWriteMs) {
    Dispatcher dispatcher = new Dispatcher();
    dispatcher.setMaxRequests(MAX_REQUESTS);
    dispatcher.setMaxRequestsPerHost(MAX_REQUESTS_PER_HOST);

    return new OkHttpClient.Builder()
        .dispatcher(dispatcher)
        .connectionPool(
            new ConnectionPool(MAX_IDLE_CONNECTIONS, KEEP_ALIVE_MINUTES, TimeUnit.MINUTES))
        .protocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1))
        .connectTimeout(connectMs, TimeUnit.MILLISECONDS)
        .readTimeout(readWriteMs, TimeUnit.MILLISECONDS)
        .writeTimeout(readWriteMs, TimeUnit.MILLISECONDS)
        .callTimeout(connectMs + readWriteMs, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(false)
        .followSslRedirects(false)
        .build();
  }
}
