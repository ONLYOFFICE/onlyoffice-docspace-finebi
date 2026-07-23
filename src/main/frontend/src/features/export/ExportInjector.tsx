import { DomInjector } from "@api/injector";
import finebi from "@config/finebi.json";
import { useRegistryStore } from "@store/registry";
import { useRendererStore } from "@store/renderer";
import type { HostWidgetAttached } from "@api/registry";
import { useExportStore } from "@features/export/store/export";
import {
  ExportContent,
  ExportItem,
} from "./components/ExportItem";

import exportCss from "./components/export.css?inline";

interface ExportWidgetOptions extends Record<string, unknown> {
  handler?: () => void;
}

export class ExportInjector {
  private readonly injector = new DomInjector();
  private menuItem: HTMLElement | null = null;

  constructor() {
    useRegistryStore.getState().define<ExportWidgetOptions>({
      type: finebi.export.widgetType,
      defaultConfig: {
        cls: [
          "cursor-pointer", "bi-down-list-item", "bi-list-item-active",
          "bi-f-v-c", "bi-f-h", "v-middle", "h-left", "onlyoffice-export",
        ].join(" "),
      },
      init(el: HTMLElement, options: ExportWidgetOptions, on: HostWidgetAttached) {
        el.setAttribute(finebi.export.itemAttr, "1");
        el.setAttribute("role", "button");
        el.style.height = "30px";
        el.style.position = "relative";
        el.title = "Export this dashboard as Excel and upload to DocSpace";
        useRendererStore.getState().mount(<ExportContent />, el);

        on("EVENT_CHANGE", () => options.handler?.());
        el.addEventListener("click", (e) => { e.preventDefault(); e.stopPropagation(); });
      },
    });

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
        const item = this.createItem(doc);
        list.appendChild(item);
        this.menuItem = item;
        return false;
      },
    });
  }

  sync(): void {
    if (!this.menuItem) return;
    this.applyState(this.menuItem);
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
    if (doc === document) {
      const el = useRegistryStore.getState().create<ExportWidgetOptions>(finebi.export.widgetType, {
        handler: () => void useExportStore.getState().run(),
      });
      if (el) { this.applyState(el); return el; }
    }
    return this.createItemDom(doc);
  }

  private createItemDom(doc: Document): HTMLElement {
    const onActivate = (event: MouseEvent) => {
      event.preventDefault();
      event.stopPropagation();
      void useExportStore.getState().run();
    };
    const item = useRendererStore.getState().toElement(<ExportItem onActivate={onActivate} />, { doc });
    item.setAttribute(finebi.export.itemAttr, "1");
    this.applyState(item);
    return item;
  }

  private applyState(item: HTMLElement): void {
    const { busy, enabled } = useExportStore.getState();
    item.classList.toggle("onlyoffice-export--disabled", !enabled);
    item.classList.toggle("onlyoffice-export--busy", busy);
    const label = item.querySelector(".onlyoffice-export__label");
    if (label) label.textContent = busy ? "Uploading…" : "Export to DocSpace";
    item.title = enabled
      ? "Export this dashboard as Excel and upload to DocSpace"
      : "Configure DocSpace and sign in to enable this feature";
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
