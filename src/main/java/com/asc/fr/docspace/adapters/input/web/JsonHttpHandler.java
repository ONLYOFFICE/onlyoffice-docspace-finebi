package com.asc.fr.docspace.adapters.input.web;

import com.asc.fr.docspace.application.exception.BadRequestStatusException;
import com.asc.fr.docspace.application.exception.ForbiddenStatusException;
import com.asc.fr.docspace.application.exception.ImportRejectedException;
import com.asc.fr.docspace.application.exception.PluginStatusException;
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
public abstract class JsonHttpHandler extends PluginHttpHandler {
  /** Non-public endpoint (requires a FineBI session). */
  protected JsonHttpHandler(RequestMethod method, String path) {
    this(method, path, false);
  }

  protected JsonHttpHandler(RequestMethod method, String path, boolean open) {
    super(method, path, open);
  }

  /** Trimmed optional body field, never null. */
  protected static String orEmpty(String value) {
    return value == null ? "" : value.trim();
  }

  /**
   * @return the resolved user; throws 403 when not a FineBI administrator.
   */
  protected static RequestUser requireAdmin(HttpServletRequest request, String message) {
    RequestUser user = RequestUser.from(request);
    if (!user.isAdmin()) throw new ForbiddenStatusException(message);

    return user;
  }

  /**
   * @return the trimmed body field; throws 400 when the field is absent or blank.
   */
  protected static String require(String value, String name) {
    String trimmed = orEmpty(value);
    if (trimmed.isEmpty()) throw new BadRequestStatusException(name + " is required");

    return trimmed;
  }

  /**
   * @return the response payload — an {@link OkResponse} subclass, any Jackson-serializable object,
   *     or a String of pre-rendered JSON.
   */
  protected abstract Object handleJson(HttpServletRequest request) throws Exception;

  @Override
  public final void handle(HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    try {
      HttpJson.write(response, HttpServletResponse.SC_OK, handleJson(request));
    } catch (PluginStatusException e) {
      HttpJson.write(response, e.status(), new ErrorResponse(e.getMessage()));
    } catch (ImportRejectedException e) {
      HttpJson.write(
          response,
          HttpServletResponse.SC_BAD_REQUEST,
          new ErrorResponse(e.getMessage(), e.getCode(), e.getParams()));
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
}
