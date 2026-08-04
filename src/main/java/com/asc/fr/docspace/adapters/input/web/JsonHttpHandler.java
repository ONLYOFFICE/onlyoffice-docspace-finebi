package com.asc.fr.docspace.adapters.input.web;

import com.asc.fr.docspace.application.exception.BadRequestStatusException;
import com.asc.fr.docspace.application.exception.ForbiddenStatusException;
import com.asc.fr.docspace.application.exception.ImportRejectedException;
import com.asc.fr.docspace.application.exception.PluginStatusException;
import com.asc.fr.docspace.application.exception.TenantLimitExceededException;
import com.fr.third.springframework.web.bind.annotation.RequestMethod;
import java.io.IOException;
import java.util.Collections;
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
 *   <li>{@link PluginStatusException} - its status, message as {@code error} (plus its {@code
 *       code}/{@code params} when set, for a translated frontend message)
 *   <li>{@link ImportRejectedException} / {@link TenantLimitExceededException} - 400, likewise
 *       carrying a {@code code} when set
 *   <li>{@link IOException} - 400, message as {@code error}
 *   <li>anything else - 500, generic message
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
    return requireAdmin(request, message, null);
  }

  /**
   * @return the resolved user; throws 403 (carrying {@code code}, an i18n key) when not a FineBI
   *     administrator.
   */
  protected static RequestUser requireAdmin(
      HttpServletRequest request, String message, String code) {
    RequestUser user = RequestUser.from(request);
    if (!user.isAdmin()) throw new ForbiddenStatusException(message, code);

    return user;
  }

  /**
   * @return the trimmed body field; throws 400 when the field is absent or blank.
   */
  protected static String require(String value, String name) {
    String trimmed = orEmpty(value);
    if (trimmed.isEmpty())
      throw new BadRequestStatusException(
          name + " is required",
          "client.error.field.required",
          Collections.singletonMap("field", name));

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
      HttpJson.write(response, e.status(), new ErrorResponse(e.getMessage(), e.code(), e.params()));
    } catch (ImportRejectedException e) {
      HttpJson.write(
          response,
          HttpServletResponse.SC_BAD_REQUEST,
          new ErrorResponse(e.getMessage(), e.getCode(), e.getParams()));
    } catch (TenantLimitExceededException e) {
      HttpJson.write(
          response,
          HttpServletResponse.SC_BAD_REQUEST,
          new ErrorResponse(e.getMessage(), e.code(), null));
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
