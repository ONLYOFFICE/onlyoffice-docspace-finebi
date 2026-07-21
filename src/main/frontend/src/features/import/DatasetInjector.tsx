import { DomInjector } from "@api/injector";
import { useRegistryStore } from "@store/registry";
import { useRendererStore } from "@store/renderer";
import finebi from "@config/finebi.json";
import { useImportStore } from "@features/import/store/import";
import { DatasetContent, DatasetItem } from "./components/DatasetItem";

interface DatasetWidgetOptions extends Record<string, unknown> {
  handler?: () => void;
}

/**
 * Imperative FineBI DOM injector for the “Import from DocSpace” Add Dataset row.
 * Clicks call {@link useImportStore}.pick — no external callbacks.
 */
export class DatasetInjector {
  private readonly injector = new DomInjector();

  constructor() {
    useRegistryStore.getState().define<DatasetWidgetOptions>({
      type: finebi.dataset.widgetType,
      defaultConfig: { cls: "onlyoffice-import__item" },
      init: (el, options, on) => {
        useRendererStore.getState().mount(<DatasetContent />, el);
        // FineUI event = semantic activation (keyboard + mouse); the DOM
        // listener only stops propagation, which needs the raw Event.
        on("EVENT_CHANGE", () => {
          dismissPopup(el.closest<HTMLElement>(finebi.selectors.popupView));
          options.handler?.();
        });
        el.addEventListener("click", (event) => event.stopPropagation());
      },
    });

    this.injector.addRule<HTMLElement>({
      selector: finebi.selectors.downListPopup,
      marker: finebi.dataset.hostMarker,
      filter: (popup) => !!popup.querySelector(finebi.selectors.spiderExcelTable),
      inject: (popup) => {
        const list = popup.querySelector(finebi.selectors.spiderExcelTable)?.parentElement;
        if (!list) return false;
        list.appendChild(this.createItem());
      },
    });
  }

  observe(doc: Document = document): void {
    this.injector.observe(doc);
  }

  disconnect(): void {
    this.injector.disconnect();
  }

  private createItem(): HTMLElement {
    return useRegistryStore.getState().create<DatasetWidgetOptions>(finebi.dataset.widgetType, {
      handler: () => void useImportStore.getState().pick(),
    }) ?? this.createItemDom();
  }

  private createItemDom(): HTMLElement {
    return useRendererStore.getState().toElement(
      <DatasetItem
        onActivate={(event) => {
          event.stopPropagation();
          dismissPopup((event.currentTarget as HTMLElement).closest<HTMLElement>(finebi.selectors.popupView));
          void useImportStore.getState().pick();
        }}
      />,
    );
  }
}

function dismissPopup(popup: HTMLElement | null): void {
  document.body.dispatchEvent(new MouseEvent("mousedown", { bubbles: true }));
  if (popup) popup.style.display = "none";
}
