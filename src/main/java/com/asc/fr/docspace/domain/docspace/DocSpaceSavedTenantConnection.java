package com.asc.fr.docspace.domain.docspace;

import lombok.Value;

/**
 * A saved tenant's full connection details, including its own webhook secret — used where a saved
 * (non-active) tenant must be treated exactly like the active one: matching an inbound webhook's
 * signature (see WebhookHttpHandler) and keeping its webhook subscription registered (see
 * WebhookReconciliationClusterJob).
 */
@Value
public class DocSpaceSavedTenantConnection {
  DocSpaceTenantConfiguration configuration;
  String webhookSecret;
}
