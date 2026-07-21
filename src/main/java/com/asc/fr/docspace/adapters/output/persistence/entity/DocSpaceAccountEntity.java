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
@Table(name = "plugin_docspace_account")
public class DocSpaceAccountEntity extends BaseEntity {
  @Column(name = "email", length = 512)
  private String email = "";

  @Column(name = "docspaceUserId")
  private String docspaceUserId = "";

  @Column(name = "passwordHash", length = 1024)
  private String passwordHash = "";
}
