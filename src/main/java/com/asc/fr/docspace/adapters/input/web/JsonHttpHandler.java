package com.asc.fr.docspace.adapters.input.web;

import com.asc.fr.docspace.application.exception.BadRequestStatusException;
import com.asc.fr.docspace.application.exception.ForbiddenStatusException;
import com.asc.fr.docspace.application.exception.PluginStatusException;
import com.fr.decision.fun.impl.BaseHttpHandler;
import com.fr.third.springframework.web.bind.annotation.RequestMethod;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Base for endpoints that answer JSON, subclasses declare method/path/visibility once in the
 * constructor and return a payload object from {@link #handleJson}, which is serialized with
 * Jackson ({@code 200 application/json}).
 *
 * <p>Failures map to status codes in one place so no handler hand-rolls error responses or can leak
 * a stack trace:
 *
 * <ul>
 *   <li>{@link PluginStatusException} → its status, message as {@code error}
 *   <li>{@link IOException} → 400, message as {@code error}
 *   <li>anything else → 500, generic message
 * </ul>
 */
public abstract class JsonHttpHandler extends BaseHttpHandler {
  private final RequestMethod method;
  private final String path;
  private final boolean open;

  /** Non-public endpoint (requires a FineBI session). */
  protected JsonHttpHandler(RequestMethod method, String path) {
    this(method, path, false);
  }

  protected JsonHttpHandler(RequestMethod method, String path, boolean open) {
    this.method = method;
    this.path = path;
    this.open = open;
  }

  @Override
  public final RequestMethod getMethod() {
    return method;
  }

  @Override
  public final String getPath() {
    return path;
  }

  @Override
  public final boolean isPublic() {
    return open;
  }

  @Override
  public final void handle(HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    try {
      HttpJson.write(response, HttpServletResponse.SC_OK, handleJson(request));
    } catch (PluginStatusException e) {
      HttpJson.write(response, e.status(), new ErrorResponse(e.getMessage()));
    } catch (IOException e) {
      HttpJson.write(
          response, HttpServletResponse.SC_BAD_REQUEST, new ErrorResponse(e.getMessage()));
    } catch (Exception e) {
      HttpJson.write(
          response,
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          new ErrorResponse("Internal plugin error: " + HttpJson.rootCause(e)));
    }
  }

  /**
   * @return the response payload — an {@link OkResponse} subclass, any Jackson-serializable object,
   *     or a String of pre-rendered JSON.
   */
  protected abstract Object handleJson(HttpServletRequest request) throws Exception;

  /**
   * @return the resolved user; throws 403 when not a FineBI administrator.
   */
  protected static RequestUser requireAdmin(HttpServletRequest request, String message) {
    RequestUser user = RequestUser.from(request);
    if (!user.isAdmin()) throw new ForbiddenStatusException(message);

    return user;
  }

  /**
   * @return the trimmed request parameter; throws 400 when blank.
   */
  protected static String requireParam(HttpServletRequest request, String name) {
    String value = Requests.param(request, name);
    if (value.isEmpty()) throw new BadRequestStatusException(name + " is required");

    return value;
  }
}
