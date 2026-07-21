import type {
  UserInfo,
  LoginResult,
  DocSpaceFrame,
} from "@features/docspace/types";

export interface AuthenticationResult {
  hash: string;
  login: LoginResult;
  me: UserInfo;
}

export interface LoginRequest {
  frame: DocSpaceFrame;
  email: string;
  password: string;
  action: string;
  extraFields?: Record<string, string>;
}
