import type { ComponentChildren } from "preact";

import "./lead.css";

interface LeadProps {
  children: ComponentChildren;
}

export function Lead({ children }: LeadProps) {
  return <span className="onlyoffice-lead">{children}</span>;
}
