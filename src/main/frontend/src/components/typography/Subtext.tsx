import type { ComponentChildren } from "preact";

import "./subtext.css";

interface SubtextProps {
  children: ComponentChildren;
}

export function Subtext({ children }: SubtextProps) {
  return <p className="onlyoffice-subtext">{children}</p>;
}
