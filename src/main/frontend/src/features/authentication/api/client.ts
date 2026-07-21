import { usePluginStore } from "@store/plugin";
import type { AuthenticationResult, LoginRequest } from "../types/authentication";
import type { DocSpaceFrame, UserInfo } from "@features/docspace/types";

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
      throw new Error("DocSpace login failed. Check email and password.");
    }

    let me: UserInfo | null | undefined;
    try {
      me = await frame.getUserInfo();
    } catch {
      throw new Error("DocSpace cookies blocked. Could not get user info.");
    }

    if (!me || !me.id) {
      throw new Error("DocSpace cookies blocked. Could not get user info.");
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
