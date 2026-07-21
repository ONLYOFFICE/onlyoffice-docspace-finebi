import type { ComponentChildren } from "preact";

import "./page.css";

interface PageContainerProps {
  children: ComponentChildren;
  className?: string;
}

export function PageContainer({ children, className }: PageContainerProps) {
  const cls = ["onlyoffice-page-container", className].filter(Boolean).join(" ");
  return <div className={cls}>{children}</div>;
}
