import type { ComponentChildren } from "preact";

import "./hint.css";

interface HintProps {
  children: ComponentChildren;
}

export function Hint({ children }: HintProps) {
  return <p className="onlyoffice-hint">{children}</p>;
}
