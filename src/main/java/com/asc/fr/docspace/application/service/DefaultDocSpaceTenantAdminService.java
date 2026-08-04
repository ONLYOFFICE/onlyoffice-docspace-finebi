package com.asc.fr.docspace.application.service;

import com.asc.fr.docspace.application.exception.TenantLimitExceededException;
import com.asc.fr.docspace.application.port.input.DocSpaceTenantAdminService;
import com.asc.fr.docspace.domain.DocSpaceSavedTenantService;
import com.asc.fr.docspace.domain.DocSpaceTenantService;
import com.asc.fr.docspace.domain.SynchronizationLinkRegistry;
import com.asc.fr.docspace.domain.SynchronizationSettings;
import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.asc.fr.docspace.domain.docspace.DocSpaceTenantConfiguration;
import com.google.inject.Inject;
import java.io.IOException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(onConstructor_ = @__(@Inject))
public final class DefaultDocSpaceTenantAdminService implements DocSpaceTenantAdminService {
  private final DocSpaceTenantService tenantService;
  private final DocSpaceSavedTenantService savedTenantService;
  private final SynchronizationLinkRegistry synchronizationService;
  private final SynchronizationSettings synchronizationSettings;

  @Override
  public void save(URL docSpaceUrl, DocSpaceAccountCredentials admin) throws IOException {
    DocSpaceTenantConfiguration outgoing = tenantService.load();
    String currentUrl = outgoing.getUrl().getValue();
    String newUrl = docSpaceUrl.getValue();
    boolean sameAsActive = !currentUrl.isEmpty() && currentUrl.equals(newUrl);

    // Refuse to configure a tenant beyond the cap outright.
    if (!sameAsActive && !savedTenantService.hasCapacityFor(newUrl))
      throw new TenantLimitExceededException(
          "Reset available credentials to register a different tenant. Maximum number of saved tenants reached.");

    // Setup can also be used to point at a different DocSpace directly (not just via the
    // dedicated "Change Tenant" action) — preserve the outgoing tenant exactly like
    // changeTenant() does, so switching tenants this way still respects the saved-tenant cap and
    // doesn't silently drop its credentials/secret.
    if (!sameAsActive && !currentUrl.isEmpty() && outgoing.getAdmin().isComplete())
      savedTenantService.upsert(outgoing, synchronizationSettings.loadSecret());

    tenantService.save(new DocSpaceTenantConfiguration(newUrl, admin));
  }

  @Override
  public void changeTenant() throws IOException {
    // Unlike reset(), switching to a different DocSpace connection keeps every sync link — they
    // simply go unused unless the same files/tenant are reconnected later. The outgoing tenant's
    // admin credentials and webhook secret are preserved too (not lost), so only an explicit
    // reset() is the deliberate "wipe everything" action.
    savedTenantService.upsert(tenantService.load(), synchronizationSettings.loadSecret());
    tenantService.clear();
  }

  @Override
  public void reset() throws IOException {
    // The webhook secret is deliberately NOT cleared here: DocSpace's own subscription for our
    // callback URL is never deleted either (see register()'s "already registered, skip create"
    // check — reliably matched by the fixed "FineBI Sync" name), so it keeps signing events with
    // this same secret. Rotating the secret without also being able to reliably delete/update the
    // DocSpace-side subscription would just make future webhooks unverifiable — keeping it stable
    // is what makes that idempotent check meaningful (and avoids ever creating a duplicate).
    synchronizationService.removeAll();
    savedTenantService.clearAll();
    tenantService.clear();
  }
}
