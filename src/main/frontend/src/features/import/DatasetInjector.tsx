import { DatasetItem } from "./components/DatasetItem";

import { useImportStore } from "@features/import/store/import";

import { useRendererStore } from "@store/renderer";
import { DomInjector } from "@api/injector";

import finebi from "@config/finebi.json";

/**
 * Imperative FineBI DOM injector for the “Import from DocSpace” Add Dataset row.
 * Clicks call {@link useImportStore}.pick — no external callbacks.
 */
export class DatasetInjector {
  private readonly injector = new DomInjector();

  constructor() {
    this.injector.addRule<HTMLElement>({
      selector: finebi.selectors.downListPopup,
      marker: finebi.dataset.hostMarker,
      filter: (popup) =>
        !!popup.querySelector(finebi.selectors.spiderExcelTable) &&
        !popup.querySelector(".onlyoffice-import__item"),
      inject: (popup) => {
        const list = popup.querySelector(finebi.selectors.spiderExcelTable)?.parentElement;
        if (!list) return false;
        list.appendChild(this.createItem());
        return false;
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
    return useRendererStore.getState().toElement(
      <DatasetItem
        onActivate={(event) => {
          event.stopPropagation();
          dismissPopup(
            (event.currentTarget as HTMLElement).closest<HTMLElement>(finebi.selectors.popupView),
          );
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
