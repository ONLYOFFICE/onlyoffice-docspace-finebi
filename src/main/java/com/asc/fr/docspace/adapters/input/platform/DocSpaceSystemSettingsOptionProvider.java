package com.asc.fr.docspace.adapters.input.platform;

import com.asc.fr.docspace.PluginManifest;
import com.fr.decision.fun.impl.AbstractSystemOptionProvider;
import com.fr.decision.web.MainComponent;
import com.fr.plugin.transform.FunctionRecorder;
import com.fr.web.struct.Atom;

/**
 * Registers "DocSpace" as a first-class System Management module: the platform persists it as an
 * authority object under {@code decision-management-root} (see AuthorityDataCheckerOperator), so it
 * appears in {@code Dec.decisionModules} and participates in FineBI's graded authority — a super
 * admin can grant/revoke the page per sub-admin.
 *
 * <p>The visible menu entry is injected client-side by nav.js (injectManagementItem) using the SAME
 * id: the management shell merges the server module (existence + permissions) with the client item
 * (text + iframe cardType). Neither half works alone — both read the id from manifest.json.
 */
@FunctionRecorder
public class DocSpaceSystemSettingsOptionProvider extends AbstractSystemOptionProvider {
  @Override
  public String id() {
    return PluginManifest.get().module.id;
  }

  @Override
  public String displayName() {
    return PluginManifest.get().module.displayName;
  }

  @Override
  public int sortIndex() {
    return PluginManifest.get().module.sortIndex;
  }

  @Override
  public Atom attach() {
    return MainComponent.KEY;
  }

  /** client is already injected via DocSpaceWebResourceProvider; no extra client assets. */
  @Override
  public Atom[] clients() {
    return new Atom[0];
  }
}
