import type { DocSpaceItem } from "@features/docspace/types";

export interface ToastState {
  message: string;
  type?: "success" | "error";
}

export type Stage =
  | { name: "connecting" }
  | { name: "connect-error"; message: string }
  | { name: "file" }
  | { name: "folder"; file: DocSpaceItem }
  | { name: "finishing" };
