import { usePluginStore } from "@store/plugin";
import type { AuthenticationResult, LoginRequest } from "../types/authentication";
import type { DocSpaceFrame, UserInfo } from "@features/docspace/types";
import { translate } from "@i18n";

export class AuthenticationClient {
  private async _authenticate(
    frame: DocSpaceFrame,
    email: string,
    password: string,
  ): Promise<AuthenticationResult> {
    const settings = await frame.getHashSettings();
    const hash = await frame.createHash(password, settings);
    const login = await frame.login(email, hash);
    if (!login || !login.url) {
      throw new Error(translate("client.login.failed"));
    }

    let me: UserInfo | null | undefined;
    try {
      me = await frame.getUserInfo();
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
