package com.asc.fr.docspace;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fr.decision.fun.HttpHandler;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class DocSpacePluginApplicationContextTest {
  @Test
  void givenApplicationContext_whenBootingGuiceGraph_thenExposesAllHttpHandlers() {
    HttpHandler[] handlers = DocSpacePluginApplicationContext.get().httpHandlers();
    assertEquals(
        17,
        handlers.length,
        () ->
            "Registered handlers changed: "
                + Arrays.stream(handlers)
                    .map(h -> h.getClass().getSimpleName())
                    .collect(Collectors.joining(", ")));
  }
}
