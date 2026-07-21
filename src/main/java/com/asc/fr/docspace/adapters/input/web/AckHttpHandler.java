package com.asc.fr.docspace.adapters.input.web;

import com.fr.third.springframework.web.bind.annotation.RequestMethod;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AckHttpHandler extends PluginHttpHandler {
  protected AckHttpHandler(RequestMethod method, String path, boolean open) {
    super(method, path, open);
  }

  @Override
  public final void handle(HttpServletRequest request, HttpServletResponse response) {
    response.setStatus(HttpServletResponse.SC_OK);
  }
}
