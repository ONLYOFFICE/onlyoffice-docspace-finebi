import type { FunctionComponent } from "preact";

import { ExportContent } from "./ExportContent";

import "./export.css";

interface ItemProps {
  label?: string;
  onActivate?: (event: MouseEvent) => void;
  running?: boolean;
}

export const ExportItem: FunctionComponent<ItemProps> = ({ label, onActivate, running }) => (
  <div
    className={`bi-basic-button cursor-pointer bi-down-list-item bi-list-item-active onlyoffice-export bi-f-v-c bi-f-h v-middle h-left${
      running ? " onlyoffice-export--running" : ""
    }`}
    style={{ height: "30px", position: "relative" }}
    role="button"
    onClick={onActivate}
  >
    <ExportContent label={label} />
  </div>
);
