import type { FunctionComponent } from "preact";
import OnlyofficeIcon from "@resources/images/onlyoffice-logo.svg";

import "./dataset.css";

export const DatasetContent: FunctionComponent = () => (
  <>
    <span className="onlyoffice-import__icon">
      <OnlyofficeIcon width={20} height={20} aria-hidden="true" />
    </span>
    <div className="bi-label">Import from DocSpace</div>
  </>
);

interface ItemProps {
  onActivate?: (event: MouseEvent) => void;
}

export const DatasetItem: FunctionComponent<ItemProps> = ({ onActivate }) => (
  <div className="onlyoffice-import__item" role="button" onClick={onActivate}>
    <DatasetContent />
  </div>
);
