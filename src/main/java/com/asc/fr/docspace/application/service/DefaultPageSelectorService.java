package com.asc.fr.docspace.application.service;

import com.asc.fr.docspace.application.port.input.PageSelectorService;
import com.asc.fr.docspace.application.port.input.transfer.Page;
import com.asc.fr.docspace.application.port.input.transfer.PageSelectionCommand;

public final class DefaultPageSelectorService implements PageSelectorService {
  @Override
  public Page select(PageSelectionCommand request) {
    if (request.isLauncher()) return Page.LAUNCHER;

    if (request.isLogout()) return Page.LOGOUT;

    if (request.isSetup()) {
      if (!request.isAdmin()) return Page.ACCESS_DENIED;
      if (!request.isConfigured()) return Page.SETUP;

      return request.isHasLogin() ? Page.ADMIN_SETTINGS : Page.ADMIN_LOGIN;
    }

    if (!request.isConfigured()) return Page.NOT_CONFIGURED;

    if (!request.isHasLogin()) return Page.USER_LOGIN;

    return Page.DOCSPACE;
  }
}
