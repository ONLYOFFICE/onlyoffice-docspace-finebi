package com.asc.fr.docspace.adapters.input.platform;

import com.asc.fr.docspace.PluginManifest;
import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.web.struct.Component;
import com.fr.web.struct.Filter;
import com.fr.web.struct.browser.RequestClient;
import com.fr.web.struct.category.ScriptPath;
import com.fr.web.struct.category.StylePath;

/** Loads navigation injection assets on the decision platform shell. */
public class DocSpaceNavigationComponent extends Component {
  public static final DocSpaceNavigationComponent KEY = new DocSpaceNavigationComponent();

  private DocSpaceNavigationComponent() {}

  @Override
  public ScriptPath script(RequestClient client) {
    return ScriptPath.build(PluginManifest.get().assets.navigation.script);
  }

  @Override
  public StylePath style(RequestClient client) {
    return StylePath.build(PluginManifest.get().assets.navigation.style);
  }

  @Override
  @ExecuteFunctionRecord
  public Filter filter() {
    return () -> true;
  }
}
