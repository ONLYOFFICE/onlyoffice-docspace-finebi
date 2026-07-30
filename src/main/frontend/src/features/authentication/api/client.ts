import type { DocSpaceFrame, UserInfo } from "@features/docspace/types";
import type { AuthenticationResult, LoginRequest } from "@features/authentication/types/authentication";

import { usePluginStore } from "@store/plugin";

import { FuncUtils } from "@utils/func";

import { translate } from "@i18n";

const LOGIN_MS = 10_000;

class LoginTimeoutError extends Error {}

function loginFailed(): Error {
  return new Error(translate("client.login.failed"));
}

function loginTimedOut(): Error {
  return new LoginTimeoutError(translate("client.login.timeout"));
}

export class AuthenticationClient {
  private async _authenticate(
    frame: DocSpaceFrame,
    email: string,
    password: string,
  ): Promise<AuthenticationResult> {
    let hash: string;
    let login: AuthenticationResult["login"] | null | undefined;

    try {
      const settings = await FuncUtils.withTimeout(
        frame.getHashSettings(),
        LOGIN_MS,
        loginTimedOut,
      );
      hash = await FuncUtils.withTimeout(
        frame.createHash(password, settings),
        LOGIN_MS,
        loginTimedOut,
      );
      login = await FuncUtils.withTimeout(
        frame.login(email, hash),
        LOGIN_MS,
        loginTimedOut,
      );
    } catch (err) {
      if (err instanceof LoginTimeoutError) throw err;
      throw loginFailed();
    }

    if (!login || !login.url) {
      throw loginFailed();
    }

    let me: UserInfo | null | undefined;
    try {
      me = await FuncUtils.withTimeout(
        frame.getUserInfo(),
        LOGIN_MS,
        () => new Error(translate("client.cookies.blocked")),
      );
    } catch {
      throw new Error(translate("client.cookies.blocked"));
    }

    if (!me || !me.id) {
      throw new Error(translate("client.cookies.blocked"));
    }

    return { hash, login, me };
  }

  async authenticate(request: LoginRequest): Promise<AuthenticationResult> {
    const result = await this._authenticate(
      request.frame,
      request.email,
      request.password,
    );
    await usePluginStore.getState().login(request.action, {
      ...(request.extraFields || {}),
      email: request.email,
      userId: result.me.id ? String(result.me.id) : "",
      hash: result.hash,
    });
    return result;
  }
}
