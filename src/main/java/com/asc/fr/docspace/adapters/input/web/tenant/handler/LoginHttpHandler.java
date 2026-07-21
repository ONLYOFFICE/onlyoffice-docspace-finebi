package com.asc.fr.docspace.adapters.input.web.tenant.handler;

import com.asc.fr.docspace.PluginManifest;
import com.asc.fr.docspace.adapters.input.web.JsonHttpHandler;
import com.asc.fr.docspace.adapters.input.web.OkResponse;
import com.asc.fr.docspace.adapters.input.web.RequestUser;
import com.asc.fr.docspace.adapters.input.web.Requests;
import com.asc.fr.docspace.adapters.input.web.tenant.transfer.CredentialsRequest;
import com.asc.fr.docspace.application.exception.BadRequestStatusException;
import com.asc.fr.docspace.application.exception.PluginStatusException;
import com.asc.fr.docspace.application.port.input.DocSpaceTenantService;
import com.asc.fr.docspace.application.port.input.DocSpaceUserAccountService;
import com.asc.fr.docspace.domain.docspace.DocSpaceAccountCredentials;
import com.asc.fr.docspace.domain.exception.InvalidCredentialsException;
import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.third.springframework.web.bind.annotation.RequestMethod;
import com.google.inject.Inject;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

/** Persists per-user DocSpace login credentials (password hash from SDK). */
public class LoginHttpHandler extends JsonHttpHandler {
  private final DocSpaceTenantService tenantService;
  private final DocSpaceUserAccountService userAccountService;

  @Inject
  public LoginHttpHandler(
      DocSpaceTenantService tenantService, DocSpaceUserAccountService userAccountService) {
    super(RequestMethod.POST, PluginManifest.get().endpoints.login);
    this.tenantService = tenantService;
    this.userAccountService = userAccountService;
  }

  @Override
  @ExecuteFunctionRecord
  protected Object handleJson(HttpServletRequest request) throws Exception {
    if (!tenantService.isConfigured())
      throw new PluginStatusException(400, "DocSpace is not configured. Ask your administrator.");

    CredentialsRequest body = Requests.json(request, CredentialsRequest.class);
    DocSpaceAccountCredentials credentials;
    try {
      credentials =
          new DocSpaceAccountCredentials(body.getEmail(), body.getUserId(), body.getHash());
    } catch (InvalidCredentialsException e) {
      throw new BadRequestStatusException(e.getMessage());
    }

    RequestUser user = RequestUser.from(request);
    try {
      userAccountService.saveLogin(user.name(), credentials);
    } catch (IOException e) {
      throw new PluginStatusException(500, "Could not save login: " + e.getMessage());
    }

    return OkResponse.ok();
  }
}
