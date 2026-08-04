package com.asc.fr.docspace;

import static org.assertj.core.api.Assertions.assertThat;

import com.fr.decision.fun.HttpHandler;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class DocSpacePluginApplicationContextTest {
  @Test
  void givenApplicationContext_whenBootingGuiceGraph_thenExposesAllHttpHandlers() {
    HttpHandler[] handlers = DocSpacePluginApplicationContext.get().httpHandlers();
    assertThat(handlers)
        .as(
            () ->
                "Registered handlers changed: "
                    + Arrays.stream(handlers)
                        .map(h -> h.getClass().getSimpleName())
                        .collect(Collectors.joining(", ")))
        .hasSize(21);
  }
}
