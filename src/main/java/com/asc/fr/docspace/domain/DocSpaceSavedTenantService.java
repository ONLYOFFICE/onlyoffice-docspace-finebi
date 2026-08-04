package com.asc.fr.docspace.domain;

import com.asc.fr.docspace.application.exception.TenantLimitExceededException;
import com.asc.fr.docspace.domain.docspace.DocSpaceSavedTenantConnection;
import com.asc.fr.docspace.domain.docspace.DocSpaceTenantConfiguration;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Persists every DocSpace tenant an admin has connected to (URL + admin credentials + its own
 * webhook secret), independently of DocSpaceTenantService's single "currently active" row — so
 * changing tenants doesn't lose the outgoing one's credentials. Only reset() (see
 * DefaultDocSpaceTenantAdminService) clears this history; a plain tenant switch keeps it.
 *
 * <p>This also backs multi-tenant sync: a file's sync link remembers which tenant created it (see
 * FileSynchronizationRecord.tenantUrl), and DefaultSynchronizationService resolves that tenant's
 * credentials here whenever it isn't the currently active one. Its stored webhook secret lets an
 * inbound webhook be matched to this tenant (see WebhookHttpHandler) even while a different tenant
 * is currently active, and lets its subscription stay registered (see
 * WebhookReconciliationClusterJob).
 */
public interface DocSpaceSavedTenantService {
  /**
   * @throws TenantLimitExceededException if this would add a distinct tenant beyond the fixed cap
   */
  void upsert(DocSpaceTenantConfiguration config, String webhookSecret) throws IOException;

  /** The saved configuration for a tenant, by its normalized URL. */
  Optional<DocSpaceTenantConfiguration> find(String url);

  /**
   * Whether {@code url} could be saved without exceeding the cap — true if it's already known
   * (reconnecting to a tenant that's already saved never consumes a new slot) or there's still room
   * for a distinct new one.
   */
  boolean hasCapacityFor(String url);

  /** Every saved tenant with its own webhook secret, for signature matching and reconciliation. */
  List<DocSpaceSavedTenantConnection> listConnections();

  /** Whether any tenant history exists — lets the setup page offer a "Reset" only when relevant. */
  boolean hasAny();

  /** Whether a brand-new (unknown) DocSpace URL could still be registered. */
  boolean canAddNew();

  /** Drops one saved tenant by URL. No-op when unknown. */
  void remove(String url) throws IOException;

  void clearAll() throws IOException;
}
