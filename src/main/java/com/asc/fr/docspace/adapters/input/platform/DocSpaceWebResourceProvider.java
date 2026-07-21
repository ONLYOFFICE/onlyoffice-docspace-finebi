package com.asc.fr.docspace.adapters.input.platform;

import com.fr.decision.fun.impl.AbstractWebResourceProvider;
import com.fr.decision.web.MainComponent;
import com.fr.plugin.transform.FunctionRecorder;
import com.fr.web.struct.Atom;

/** Injects client so that DocSpace appears in the platform left menu. */
@FunctionRecorder
public class DocSpaceWebResourceProvider extends AbstractWebResourceProvider {
  @Override
  public Atom attach() {
    return MainComponent.KEY;
  }

  @Override
  public Atom client() {
    return DocSpaceNavigationComponent.KEY;
  }
}
