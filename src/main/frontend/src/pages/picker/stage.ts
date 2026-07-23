import type { DocSpaceItem } from "@features/docspace/types";

export type Stage =
  | { name: "picker" }
  | { name: "folder"; file: DocSpaceItem }
  | { name: "finishing" };
