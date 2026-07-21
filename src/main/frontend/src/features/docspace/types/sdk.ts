import { DocSpaceItem } from "./entry";
import { LoginResult, UserInfo } from "./profile";

export type HashSettings = Record<string, unknown>;

export interface DocSpaceFrame {
  getHashSettings(): Promise<HashSettings>;
  createHash(password: string, settings: HashSettings): Promise<string>;
  login(
    email: string,
    passwordHash: string,
  ): Promise<LoginResult | null | undefined>;
  logout(): Promise<unknown>;
  getUserInfo(): Promise<UserInfo | null | undefined>;
}

export interface SystemOptions {
  src: string;
  frameId: string;
  checkCSP: boolean;
  events: {
    onAppReady?: () => void;
    onAppError?: (error: unknown) => void;
  };
}

export interface ManagerOptions {
  frameId: string;
  src: string;
  mode?: string;
  width?: string;
  height?: string;
  showHeader?: boolean;
  checkCSP?: boolean;
  events?: {
    onAppReady?: () => void;
    onContentReady?: () => void;
    onAppError?: (error: unknown) => void;
  };
}

export interface FileSelectorOptions {
  frameId: string;
  src: string;
  width?: string;
  height?: string;
  checkCSP?: boolean;
  acceptButtonLabel?: string;
  events?: {
    onAppReady?: () => void;
    onSelectCallback?: (item: DocSpaceItem | DocSpaceItem[]) => void;
    onCloseCallback?: () => void;
    onAppError?: (error: unknown) => void;
  };
}

export interface DocSpaceSdk {
  initSystem(options: SystemOptions): unknown;
  initManager(options: ManagerOptions): unknown;
  initFileSelector?: (options: FileSelectorOptions) => unknown;
  frames: Record<string, DocSpaceFrame | undefined>;
}
