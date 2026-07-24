import { DatasetMenuItem } from "./components/DatasetItem";

import { DomInjector } from "@api/injector";

import { openShellOverlay } from "@features/import/components/ShellOverlay";
import { ImportUrlUtils } from "@features/import/utils/url";

import { useRendererStore } from "@store/renderer";

import finebi from "@config/finebi.json";

export class DatasetInjector {
  private readonly injector = new DomInjector();

  constructor() {
    this.injector.addRule<HTMLElement>({
      selector: finebi.selectors.downListPopup,
      marker: finebi.dataset.hostMarker,
      filter: (popup) =>
        !!popup.querySelector(finebi.selectors.spiderExcelTable) &&
        !popup.querySelector(".onlyoffice-import__host"),
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
    const host = document.createElement("div");
    host.className = "onlyoffice-import__host";
    useRendererStore.getState().mount(
      <DatasetMenuItem
        onActivate={(event) => {
          event.stopPropagation();
          dismissPopup(
            (event.currentTarget as HTMLElement).closest<HTMLElement>(finebi.selectors.popupView),
          );
          openShellOverlay(ImportUrlUtils.picker());
        }}
      />,
      host,
    );

    return host;
  }
}

function dismissPopup(popup: HTMLElement | null): void {
  document.body.dispatchEvent(new MouseEvent("mousedown", { bubbles: true }));
  if (popup) popup.style.display = "none";
}
