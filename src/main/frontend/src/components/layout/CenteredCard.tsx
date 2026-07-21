import type { ComponentChildren } from "preact";

import "./card.css";

interface CenteredCardProps {
  children: ComponentChildren;
}

export function CenteredCard({ children }: CenteredCardProps) {
  return (
    <div className="onlyoffice-centered-card">
      <div className="onlyoffice-centered-card__body">
        {children}
      </div>
    </div>
  );
}
