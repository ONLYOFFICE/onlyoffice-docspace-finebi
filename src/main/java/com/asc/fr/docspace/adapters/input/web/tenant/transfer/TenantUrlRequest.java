package com.asc.fr.docspace.adapters.input.web.tenant.transfer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public final class TenantUrlRequest {
  @JsonProperty("docspaceUrl")
  private String docspaceUrl;
}
