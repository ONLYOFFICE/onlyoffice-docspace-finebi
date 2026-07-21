import type { ComponentChildren } from "preact";

import "./dialog.css";

interface DialogProps {
  title: string;
  onClose: () => void;
  children: ComponentChildren;
}

export function Dialog({ title, onClose, children }: DialogProps) {
  return (
    <div className="onlyoffice-dialog-overlay">
      <div className="onlyoffice-dialog-backdrop" onClick={onClose} />
      <div className="onlyoffice-dialog">
        <div className="onlyoffice-dialog__title">
          <span>{title}</span>
          <button className="onlyoffice-dialog__cancel" onClick={onClose}>Cancel</button>
        </div>
        {children}
      </div>
    </div>
  );
}
