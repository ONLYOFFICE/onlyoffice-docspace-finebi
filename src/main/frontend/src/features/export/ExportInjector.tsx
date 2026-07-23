import { DomInjector } from "@api/injector";

import { ExportMenuItem } from "./components/ExportMenuItem";

import { useRendererStore } from "@store/renderer";

import finebi from "@config/finebi.json";

import exportCss from "./components/export.css?inline";

export class ExportInjector {
  private readonly injector = new DomInjector();

  constructor() {
    this.injector.addRule<HTMLElement>({
      selector: finebi.selectors.popupView,
      marker: finebi.export.hostMarker,
      filter: (popup) =>
        this.isExportDropdown(popup) && popup.querySelector(`[${finebi.export.itemAttr}]`) === null,
      setup: (doc) => {
        if (doc.getElementById(finebi.export.styleId)) return;
        const style = doc.createElement("style");
        style.id = finebi.export.styleId;
        style.textContent = exportCss;
        (doc.head ?? doc.documentElement).appendChild(style);
      },
      inject: (popup, doc) => {
        const list = this.findMenuList(popup);
        if (!list) return false;
        list.appendChild(this.createItem(doc));
        return false;
      },
    });
  }

  observeAll(root: Document = document): void {
    const seen = new Set<Document>();
    const visit = (doc: Document): void => {
      if (seen.has(doc)) return;
      seen.add(doc);
      this.injector.observe(doc);
      for (const frame of doc.querySelectorAll("iframe")) {
        try {
          const child = frame.contentDocument;
          if (child) visit(child);
        } catch {
        }
      }
    };
    visit(root);
  }

  disconnect(): void {
    this.injector.disconnect();
  }

  private createItem(doc: Document): HTMLElement {
    const host = doc.createElement("div");
    host.setAttribute(finebi.export.itemAttr, "1");
    useRendererStore.getState().mount(<ExportMenuItem />, host);
    return host;
  }

  private isExportDropdown(popup: HTMLElement): boolean {
    const doc = popup.ownerDocument ?? document;
    return doc.querySelector(finebi.selectors.exportComboOpen) !== null;
  }

  private findMenuList(popup: HTMLElement): HTMLElement | null {
    return (
      popup.querySelector<HTMLElement>(".bi-down-list-group")
      ?? popup.querySelector<HTMLElement>(".bi-button-tree")
      ?? popup.querySelector<HTMLElement>(finebi.selectors.downListPopup)
      ?? null
    );
  }
}
