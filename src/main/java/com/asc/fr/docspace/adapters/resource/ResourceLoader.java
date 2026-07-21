package com.asc.fr.docspace.adapters.resource;

import java.io.IOException;

/** Loads resources bundled with the plugin. */
public interface ResourceLoader {
  String text(String path) throws IOException;
}
