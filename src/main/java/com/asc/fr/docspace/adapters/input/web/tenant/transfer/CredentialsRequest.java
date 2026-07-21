package com.asc.fr.docspace.adapters.input.web.tenant.transfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public final class CredentialsRequest {
  private String docspaceUrl;
  private String email;
  private String userId;
  private String hash;
}
