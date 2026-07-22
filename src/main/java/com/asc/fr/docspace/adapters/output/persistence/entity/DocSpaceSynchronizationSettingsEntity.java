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
@Table(name = "plugin_docspace_synchronization_settings")
public class DocSpaceSynchronizationSettingsEntity extends BaseEntity {
  public static final String SINGLETON_ID = "default";

  @Column(name = "callbackUrl", length = 2048)
  private String callbackUrl = "";

  @Column(name = "webhookSecret", length = 1024)
  private String webhookSecret = "";
}
