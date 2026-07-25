import { createPortal } from "preact/compat";
import { useEffect, useLayoutEffect, useRef, useState } from "preact/hooks";

import { useHeaderSession } from "@features/header/useHeaderSession";
import { LogoutButton } from "@features/header/components/LogoutButton";

import { useTranslation } from "@i18n";

import "./header.css";

const POPUP_WIDTH = 140;
const POPUP_CLASS = "onlyoffice-header-logout__popup";

interface PopupProps {
  anchor: HTMLElement | null;
  open: boolean;
  onExited(): void;
  onLogout(): void;
}

function LogoutPopup({ anchor, open, onExited, onLogout }: PopupProps) {
  const translate = useTranslation();
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
      className={`bi-popup-view bi-card list-view-shadow ${POPUP_CLASS} ${POPUP_CLASS}--${
        open ? "open" : "closing"
      }`}
      style={{
        position: "fixed",
        top: position?.top ?? -9999,
        left: position?.left ?? -9999,
        zIndex: 10000,
      }}
      onAnimationEnd={() => {
        if (!open) onExited();
      }}
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
        {translate("header.logout")}
      </div>
    </div>,
    document.body,
  );
}

export function HeaderLogout() {
  const [open, setOpen] = useState(false);
  const [rendered, setRendered] = useState(false);
  const anchorRef = useRef<HTMLDivElement>(null);

  const { visible, isLoggingOut, logout } = useHeaderSession();

  useEffect(() => {
    if (open) setRendered(true);
  }, [open]);

  useEffect(() => {
    if (!visible || isLoggingOut) setOpen(false);
  }, [visible, isLoggingOut]);

  useEffect(() => {
    if (!open) return;

    const onPointerDown = (event: MouseEvent) => {
      const target = event.target as HTMLElement | null;
      if (anchorRef.current?.contains(target)) return;
      if (target?.closest(`.${POPUP_CLASS}`)) return;
      setOpen(false);
    };

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setOpen(false);
    };

    document.addEventListener("mousedown", onPointerDown, true);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("mousedown", onPointerDown, true);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [open]);

  if (!visible) return null;

  return (
    <div
      ref={anchorRef}
      role="button"
      className={`bi-basic-button cursor-pointer bi-f-v-c bi-f-h v-middle h-center onlyoffice-header-logout onlyoffice-header-logout--visible${
        isLoggingOut ? " onlyoffice-header-logout--busy" : ""
      }`}
      aria-haspopup="menu"
      aria-expanded={open}
      title="DocSpace"
      onClick={(event) => {
        event.preventDefault();
        event.stopPropagation();
        if (!isLoggingOut) setOpen((value) => !value);
      }}
    >
      <LogoutButton />
      {rendered && (
        <LogoutPopup
          anchor={anchorRef.current}
          open={open}
          onExited={() => setRendered(false)}
          onLogout={() => {
            setOpen(false);
            void logout();
          }}
        />
      )}
    </div>
  );
}
