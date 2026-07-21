package com.asc.fr.docspace.adapters.input.web;

import com.fr.decision.fun.impl.BaseHttpHandler;
import com.fr.third.springframework.web.bind.annotation.RequestMethod;

/**
 * Base for all plugin endpoints: method, path, and visibility are declared once in the constructor
 * instead of three overrides per handler.
 */
public abstract class PluginHttpHandler extends BaseHttpHandler {
  private final RequestMethod method;
  private final String path;
  private final boolean open;

  protected PluginHttpHandler(RequestMethod method, String path, boolean open) {
    this.method = method;
    this.path = path;
    this.open = open;
  }

  @Override
  public final RequestMethod getMethod() {
    return method;
  }

  @Override
  public final String getPath() {
    return path;
  }

  @Override
  public final boolean isPublic() {
    return open;
  }
}
