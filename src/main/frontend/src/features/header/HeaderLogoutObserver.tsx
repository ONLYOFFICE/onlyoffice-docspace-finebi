import { useEffect } from "preact/hooks";

import { HeaderLogout } from "./HeaderLogout";

import { DomInjector } from "@api/injector";

import { useRendererStore } from "@store/renderer";

import headerCss from "./header.css?inline";

import finebi from "@config/finebi.json";

function injectStyles(doc: Document): void {
  if (doc.getElementById(finebi.header.styleId))
    return;

  const style = doc.createElement("style");
  style.id = finebi.header.styleId;
  style.textContent = headerCss;

  (doc.head ?? doc.documentElement).appendChild(style);
}

export function HeaderLogoutObserver() {
  useEffect(() => {
    const injector = new DomInjector();
    let host: HTMLElement | null = null;

    injector.addRule<HTMLElement>({
      selector: finebi.header.messageCombo,
      marker: finebi.header.hostMarker,
      setup: injectStyles,
      inject: (message) => {
        const slot = message.parentElement;
        if (!slot) return false;
        if (!host) {
          host = document.createElement("div");
          host.setAttribute(finebi.header.itemAttr, "1");
          useRendererStore.getState().mount(<HeaderLogout />, host);
        }

        if (host.parentElement !== slot || host.nextElementSibling !== message) {
          slot.insertBefore(host, message);
        }

        return false;
      },
    });

    injector.observe(document);

    return () => {
      injector.disconnect();
      if (host) {
        useRendererStore.getState().unmount(host);
        host.remove();
        host = null;
      }
    };
  }, []);

  return null;
}
