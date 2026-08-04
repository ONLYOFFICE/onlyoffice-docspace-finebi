package com.asc.fr.docspace.adapters.input.web.tenant.transfer;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Bootstrap configuration serialized into the page template for the frontend bundle (mirrors
 * PluginCoreServerConfiguration on the frontend).
 */
@Value
@Builder
public class PageConfigResponse {
  String mode;
  Locations locations;
  Credentials credentials;
  Status status;
  Tenant tenant;
  List<KnownTenant> knownTenants;
  Actions actions;
  Response response;

  @Value
  @Builder
  public static class Locations {
    String hostOrigin;
    String pluginUrl;
    String loginUrl;
    String logoutUrl;
    String importUrl;
    String foldersUrl;
    String webhookRegistrationUrl;
    String eventStreamUrl;
  }

  @Value
  @Builder
  public static class Credentials {
    String email;
    String hash;
  }

  @Value
  @Builder
  public static class Status {
    boolean loginStored;
    boolean isAdmin;
    boolean hasSavedTenants;
    boolean canAddTenant;
    String signedInTenantUrl;
  }

  @Value
  @Builder
  public static class Tenant {
    String docSpaceUrl;
    String sdkVersion;
  }

  @Value
  @Builder
  public static class KnownTenant {
    String url;
    String email;
    boolean active;
  }

  @Value
  @Builder
  public static class Actions {
    String submit;
    String changeTenant;
    String selectTenant;
    String removeTenant;
    String reset;
    String logout;
  }

  @Value
  @Builder
  public static class Response {
    String message;
    String error;
  }
}
