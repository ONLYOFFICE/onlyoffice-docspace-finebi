package com.asc.fr.docspace.adapters.input.web.tenant.handler;

import com.asc.fr.docspace.PluginManifest;
import com.asc.fr.docspace.adapters.input.web.HttpJson;
import com.asc.fr.docspace.adapters.input.web.JsonHttpHandler;
import com.asc.fr.docspace.adapters.input.web.OkResponse;
import com.asc.fr.docspace.adapters.input.web.RequestUser;
import com.asc.fr.docspace.application.exception.PluginStatusException;
import com.asc.fr.docspace.application.port.input.DocSpaceUserAccountService;
import com.fr.plugin.transform.ExecuteFunctionRecord;
import com.fr.third.springframework.web.bind.annotation.RequestMethod;
import com.google.inject.Inject;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

/** Clears stored DocSpace credentials for the current FineBI user. */
public class LogoutHttpHandler extends JsonHttpHandler {
  private final DocSpaceUserAccountService userAccountService;

  @Inject
  public LogoutHttpHandler(DocSpaceUserAccountService userAccountService) {
    super(RequestMethod.POST, PluginManifest.get().endpoints.logout);
    this.userAccountService = userAccountService;
  }

  @Override
  @ExecuteFunctionRecord
  protected Object handleJson(HttpServletRequest request) throws Exception {
    try {
      userAccountService.clear(RequestUser.from(request).name());
    } catch (IOException e) {
      throw new PluginStatusException(500, "Could not clear credentials: " + HttpJson.rootCause(e));
    }

    return OkResponse.ok();
  }
}
