package com.asc.fr.docspace;

import com.fr.decision.fun.HttpHandler;
import com.fr.decision.fun.impl.AbstractHttpHandlerProvider;
import com.fr.plugin.transform.FunctionRecorder;

/** FineBI entry point: registers HTTP handlers from {@link DocSpacePluginApplicationContext}. */
// TODO: Add Manual ReLink in v2?
@FunctionRecorder
public class DocSpacePluginApplication extends AbstractHttpHandlerProvider {
  @Override
  public HttpHandler[] registerHandlers() {
    return DocSpacePluginApplicationContext.get().httpHandlers();
  }
}
