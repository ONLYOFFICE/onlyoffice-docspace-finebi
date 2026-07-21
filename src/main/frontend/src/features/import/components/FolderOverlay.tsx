import type { ComponentChildren } from "preact";

interface FolderOverlayProps {
  children: ComponentChildren;
  onClose: () => void;
}

export function FolderOverlay({ children, onClose }: FolderOverlayProps) {
  return (
    <div class="onlyoffice-folder-overlay">
      <div class="onlyoffice-folder-backdrop" onClick={onClose} />
      {children}
    </div>
  );
}
