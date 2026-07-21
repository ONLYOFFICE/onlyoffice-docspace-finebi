import type { DocSpaceItem } from "@features/docspace/types";

export type Stage =
  | { name: "connecting" }
  | { name: "connect-error"; message: string }
  | { name: "file" }
  | { name: "folder"; file: DocSpaceItem }
  | { name: "finishing"; text: string };
