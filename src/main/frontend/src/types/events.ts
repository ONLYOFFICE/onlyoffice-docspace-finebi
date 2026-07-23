export const DocSpaceStateEvents = {
  session: "docspace:session",
  reset: "docspace:reset",
} as const;

export type DocSpaceStateEvent =
  (typeof DocSpaceStateEvents)[keyof typeof DocSpaceStateEvents];
