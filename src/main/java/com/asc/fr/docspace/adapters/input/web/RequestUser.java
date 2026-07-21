package com.asc.fr.docspace.adapters.input.web;

import com.fr.decision.authority.data.User;
import com.fr.decision.webservice.exception.login.LoginInfoNotAvailableException;
import com.fr.decision.webservice.utils.DecisionServiceUtils;
import com.fr.decision.webservice.v10.user.UserService;
import javax.servlet.http.HttpServletRequest;

/** The FineBI user behind a request, resolved lazily from the platform session. */
public final class RequestUser {
  private interface UsernameReader {
    String read();
  }

  private String name;
  private Boolean admin;

  private final HttpServletRequest request;

  private RequestUser(HttpServletRequest request) {
    this.request = request;
  }

  private static String readUsername(UsernameReader reader) {
    try {
      String value = reader.read();
      return value == null ? "" : value.trim();
    } catch (LoginInfoNotAvailableException ignored) {
      return "";
    }
  }

  private boolean resolveAdmin() {
    try {
      User user = UserService.getInstance().getUserByRequestCookie(request);
      if (user == null) user = UserService.getInstance().getUserByRequest(request);

      if (user == null) {
        if (name().isEmpty()) return false;

        user = UserService.getInstance().getUserByUserName(name());
      }

      return user != null && UserService.getInstance().isAdmin(user.getId());
    } catch (Exception ignored) {
      return false;
    }
  }

  public static RequestUser from(HttpServletRequest request) {
    return new RequestUser(request);
  }

  public String name() {
    if (name == null) {
      String fromCookie =
          readUsername(() -> DecisionServiceUtils.getUserNameFromRequestCookie(request));
      name =
          fromCookie.isEmpty()
              ? readUsername(() -> DecisionServiceUtils.getUserNameFromRequest(request))
              : fromCookie;
    }
    return name;
  }

  public boolean isAdmin() {
    if (admin == null) {
      admin = resolveAdmin();
    }
    return admin;
  }
}
