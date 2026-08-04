package com.asc.fr.docspace.adapters.output.persistence.entity;

import com.fr.stable.db.entity.BaseEntity;
import com.fr.third.javax.persistence.Column;
import com.fr.third.javax.persistence.Entity;
import com.fr.third.javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * One row per DocSpace tenant an admin has connected to. Changing tenants (see
 * DefaultDocSpaceTenantAdminService.changeTenant) upserts the outgoing tenant here instead of
 * discarding it — only an explicit reset() clears this table. Keyed by a short hash of the
 * normalized DocSpace URL (see FineDocSpaceSavedTenantService.keyFor): BaseEntity.id has no
 * explicit @Column length, so (unlike DocSpaceTenantEntity, which keeps id short and stores its url
 * in a dedicated long column) it must not hold the URL itself.
 */
@Getter
@Setter
@Entity
@Table(name = "plugin_docspace_saved_tenant")
public class DocSpaceSavedTenantEntity extends BaseEntity {
  @Column(name = "url", length = 1024)
  private String url = "";

  @Column(name = "adminEmail", length = 512)
  private String adminEmail = "";

  @Column(name = "adminUserId")
  private String adminUserId = "";

  @Column(name = "adminHash", length = 1024)
  private String adminHash = "";

  @Column(name = "lastUsedAt")
  private Long lastUsedAt = 0L;

  // This tenant's own DocSpace webhook secret, so an inbound webhook can be matched to it (see
  // WebhookHttpHandler) and its subscription kept alive (see WebhookReconciliationClusterJob)
  // even while a different tenant is currently active.
  @Column(name = "webhookSecret", length = 1024)
  private String webhookSecret = "";
}
