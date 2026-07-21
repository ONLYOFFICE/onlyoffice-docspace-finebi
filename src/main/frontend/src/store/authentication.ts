import { create } from "zustand";
import { AuthenticationClient } from "@/features/authentication/api/client";
import type { AuthenticationResult, LoginRequest } from "@features/authentication/types/authentication";

interface AuthenticationState {
  authenticate(request: LoginRequest): Promise<AuthenticationResult>;
}

export const useAuthenticationStore = create<AuthenticationState>()(() => {
  const client = new AuthenticationClient();
  return {
    authenticate: (request) => client.authenticate(request),
  };
});
