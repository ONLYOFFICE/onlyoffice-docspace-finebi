import type { FunctionComponent } from "preact";

import OnlyofficeIcon from "@resources/images/onlyoffice-logo.svg";

interface ContentProps {
  label?: string;
}

export const ExportContent: FunctionComponent<ContentProps> = ({ label = "Export to DocSpace" }) => (
  <>
    <OnlyofficeIcon width={20} height={20} aria-hidden="true" />
    <div className="bi-label onlyoffice-export__label">{label}</div>
  </>
);

interface ItemProps extends ContentProps {
  onActivate?: (event: MouseEvent) => void;
}

export const ExportItem: FunctionComponent<ItemProps> = ({ label, onActivate }) => (
  <div
    className="bi-basic-button cursor-pointer bi-icon-text-item bi-f-v-c bi-f-h v-middle h-left onlyoffice-export"
    role="button"
    onClick={onActivate}
  >
    <ExportContent label={label} />
  </div>
);
