package com.asc.fr.docspace.adapters.resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/** {@link ResourceLoader} backed by the plugin classpath. */
public final class DefaultResourceLoader implements ResourceLoader {
  @Override
  public String text(String path) throws IOException {
    try (InputStream in = DefaultResourceLoader.class.getClassLoader().getResourceAsStream(path)) {
      if (in == null) throw new IOException("Missing resource: " + path);
      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
        return reader.lines().collect(Collectors.joining("\n"));
      }
    }
  }
}
