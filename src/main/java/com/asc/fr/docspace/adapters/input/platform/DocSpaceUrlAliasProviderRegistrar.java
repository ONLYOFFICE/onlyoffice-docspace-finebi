package com.asc.fr.docspace.adapters.input.platform;

import com.asc.fr.docspace.PluginManifest;
import com.fr.decision.fun.impl.AbstractURLAliasProvider;
import com.fr.decision.webservice.url.alias.URLAlias;
import com.fr.decision.webservice.url.alias.URLAliasFactory;
import java.util.List;

/** Registers URL aliases from manifest.json */
public class DocSpaceUrlAliasProviderRegistrar extends AbstractURLAliasProvider {
  @Override
  public URLAlias[] registerAlias() {
    List<PluginManifest.Alias> all = PluginManifest.get().aliases.all();
    URLAlias[] result = new URLAlias[all.size()];

    for (int i = 0; i < all.size(); i++) {
      PluginManifest.Alias a = all.get(i);
      result[i] = URLAliasFactory.createPluginAlias(a.from, a.to, a.isPublic);
    }

    return result;
  }
}
