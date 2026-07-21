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
@Table(name = "plugin_docspace_synchronization")
public class DocSpaceSynchronizationEntryEntity extends BaseEntity {
  @Column(name = "tableId")
  private String tableId = "";

  @Column(name = "folderId")
  private String folderId = "";

  @Column(name = "tableName", length = 512)
  private String tableName = "";
}
