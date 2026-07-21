package com.asc.fr.docspace.adapters.output.client.docspace.transfer.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class DocSpaceAuthenticationRequest {
  @JsonProperty("UserName")
  private String userName;

  @JsonProperty("PasswordHash")
  private String passwordHash;
}
