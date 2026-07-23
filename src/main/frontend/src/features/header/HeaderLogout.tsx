import { createPortal } from "preact/compat";
import { useEffect, useLayoutEffect, useRef, useState } from "preact/hooks";

import { useHeaderSession } from "./useHeaderSession";
import { LogoutButton } from "./components/LogoutButton";

const POPUP_WIDTH = 140;
const CLOSE_DELAY = 120;

interface PopupProps {
  anchor: HTMLElement | null;
  onKeep(): void;
  onLeave(): void;
  onLogout(): void;
}

function LogoutPopup({ anchor, onKeep, onLeave, onLogout }: PopupProps) {
  const [position, setPosition] = useState<{ top: number; left: number } | null>(null);

  useLayoutEffect(() => {
    if (!anchor) return;
    const rectangle = anchor.getBoundingClientRect();
    setPosition({
      top: Math.round(rectangle.bottom + 4),
      left: Math.round(Math.max(8, rectangle.right - POPUP_WIDTH)),
    });
  }, [anchor]);

  return createPortal(
    <div
      role="menu"
      className="bi-popup-view bi-card list-view-shadow onlyoffice-header-logout__popup"
      style={{
        position: "fixed",
        top: position?.top ?? -9999,
        left: position?.left ?? -9999,
        zIndex: 10000,
      }}
      onMouseEnter={onKeep}
      onMouseLeave={onLeave}
    >
      <div
        role="menuitem"
        className="bi-basic-button cursor-pointer bi-down-list-item bi-list-item-active onlyoffice-header-logout__item"
        onClick={(event) => {
          event.preventDefault();
          event.stopPropagation();
          onLogout();
        }}
      >
        Logout
      </div>
    </div>,
    document.body,
  );
}

export function HeaderLogout() {
  const closeTimer = useRef(0);
  const [open, setOpen] = useState(false);
  const anchorRef = useRef<HTMLDivElement>(null);

  const { visible, isLoggingOut, logout } = useHeaderSession();

  const cancelClose = () => {
    window.clearTimeout(closeTimer.current);
    closeTimer.current = 0;
  };
  
  const scheduleClose = () => {
    cancelClose();
    closeTimer.current = window.setTimeout(() => setOpen(false), CLOSE_DELAY);
  };

  useEffect(() => cancelClose, []);

  useEffect(() => {
    if (!visible || isLoggingOut) {
      cancelClose();
      setOpen(false);
    }
  }, [visible, isLoggingOut]);

  if (!visible) return null;

  return (
    <div
      ref={anchorRef}
      role="button"
      className={`bi-basic-button cursor-pointer bi-f-v-c bi-f-h v-middle h-center onlyoffice-header-logout onlyoffice-header-logout--visible${
        isLoggingOut ? " onlyoffice-header-logout--busy" : ""
      }`}
      aria-haspopup="menu"
      title="DocSpace"
      onMouseEnter={() => {
        cancelClose();
        if (!isLoggingOut) setOpen(true);
      }}
      onMouseLeave={scheduleClose}
    >
      <LogoutButton />
      {open && (
        <LogoutPopup
          anchor={anchorRef.current}
          onKeep={cancelClose}
          onLeave={scheduleClose}
          onLogout={() => {
            setOpen(false);
            void logout();
          }}
        />
      )}
    </div>
  );
}
