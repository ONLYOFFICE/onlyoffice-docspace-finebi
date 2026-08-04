package com.asc.fr.docspace.application.service;

import com.asc.fr.docspace.application.exception.TenantLimitExceededException;
import com.asc.fr.docspace.application.port.input.DocSpaceTenantAdminService;
import com.asc.fr.docspace.domain.DocSpaceSavedTenantService;
import com.asc.fr.docspace.domain.DocSpaceTenantService;
import com.asc.fr.docspace.domain.SynchronizationLinkRegistry;
import com.asc.fr.docspace.domain.SynchronizationSettings;
import com.asc.fr.docspace.domain.common.URL;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.asc.fr.docspace.domain.docspace.DocSpaceSavedTenantConnection;
import com.asc.fr.docspace.domain.docspace.DocSpaceTenantConfiguration;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.Optional;
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
          "Remove a saved DocSpace connection before registering a different tenant. Maximum number of saved tenants reached.",
          "client.error.tenant.limit.save");

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
    DocSpaceTenantConfiguration outgoing = tenantService.load();
    String currentUrl = outgoing.getUrl().getValue();

    if (!currentUrl.isEmpty() && !savedTenantService.hasCapacityFor(currentUrl))
      throw new TenantLimitExceededException(
          "Remove a saved DocSpace connection before changing tenant. Maximum number of saved tenants reached.",
          "client.error.tenant.limit.change");

    savedTenantService.upsert(outgoing, synchronizationSettings.loadSecret());
    tenantService.clear();
  }

  @Override
  public void selectTenant(URL docSpaceUrl) throws IOException {
    String targetUrl = docSpaceUrl.getValue();

    DocSpaceTenantConfiguration outgoing = tenantService.load();
    String currentUrl = outgoing.getUrl().getValue();
    if (!currentUrl.isEmpty() && currentUrl.equals(targetUrl)) return;

    Optional<DocSpaceSavedTenantConnection> saved =
        savedTenantService.listConnections().stream()
            .filter(
                connection -> connection.getConfiguration().getUrl().getValue().equals(targetUrl))
            .findFirst();

    if (!saved.isPresent())
      throw new IOException("No saved DocSpace connection found for " + targetUrl);

    if (!currentUrl.isEmpty() && outgoing.getAdmin().isComplete())
      savedTenantService.upsert(outgoing, synchronizationSettings.loadSecret());

    DocSpaceSavedTenantConnection selected = saved.get();
    tenantService.save(selected.getConfiguration());
    synchronizationSettings.storeSecret(selected.getWebhookSecret());
  }

  @Override
  public void removeTenant(URL docSpaceUrl) throws IOException {
    String url = docSpaceUrl.getValue();
    if (url.isEmpty()) return;

    synchronizationService.removeByTenant(url);
    savedTenantService.remove(url);

    String activeUrl = tenantService.load().getUrl().getValue();
    if (!url.equals(activeUrl)) return;

    // The active tenant was the one just removed — fall back to another saved connection
    // instead of leaving the plugin unconfigured whenever one is still available.
    Optional<DocSpaceSavedTenantConnection> next =
        savedTenantService.listConnections().stream().findFirst();
    if (next.isPresent()) {
      DocSpaceSavedTenantConnection connection = next.get();
      tenantService.save(connection.getConfiguration());
      synchronizationSettings.storeSecret(connection.getWebhookSecret());
    } else {
      tenantService.clear();
      synchronizationSettings.clearSecret();
    }
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
