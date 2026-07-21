import type { DocSpaceSdk } from "@features/docspace/types";

export {};

declare global {
  interface Window {
    Dec?: {
      fineServletURL?: string;
    };
    BI?: {
      $import?: (url: string) => void;
      Msg?: {
        toast?: (message: string) => void;
      };
    };
    DocSpace?: {
      SDK: DocSpaceSdk;
    };
  }
}
