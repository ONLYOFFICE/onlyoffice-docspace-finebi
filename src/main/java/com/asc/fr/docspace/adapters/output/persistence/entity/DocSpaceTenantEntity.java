package com.asc.fr.docspace.adapters.output.persistence.entity;

import com.fr.stable.db.entity.BaseEntity;
import com.fr.third.javax.persistence.Column;
import com.fr.third.javax.persistence.Entity;
import com.fr.third.javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "plugin_docspace_tenant")
public class DocSpaceTenantEntity extends BaseEntity {
  public static final String SINGLETON_ID = "default";

  @Column(name = "url", length = 1024)
  private String url = "";

  @Column(name = "adminEmail", length = 512)
  private String adminEmail = "";

  @Column(name = "adminUserId")
  private String adminUserId = "";

  @Column(name = "adminHash", length = 1024)
  private String adminHash = "";
}
