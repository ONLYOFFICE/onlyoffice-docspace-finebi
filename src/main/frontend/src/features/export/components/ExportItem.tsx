import type { FunctionComponent } from "preact";

import OnlyofficeIcon from "@resources/images/onlyoffice-logo.svg";

interface ContentProps {
  label?: string;
}

export const ExportContent: FunctionComponent<ContentProps> = ({ label = "Export to DocSpace" }) => (
  <>
    <div
      className="bi-f-c bi-f-h v-middle h-center f-s-n c-e f-c"
      style={{ width: "36px", height: "30px", position: "relative" }}
    >
      <OnlyofficeIcon width={20} height={20} aria-hidden="true" />
    </div>
    <div
      className="bi-label list-item-text bi-text f-s-n c-e l-c onlyoffice-export__label"
      style={{
        height: "30px",
        lineHeight: "30px",
        paddingRight: "10px",
        textAlign: "left",
        whiteSpace: "pre",
        textOverflow: "ellipsis",
        position: "relative",
      }}
    >
      {label}
    </div>
  </>
);

interface ItemProps extends ContentProps {
  onActivate?: (event: MouseEvent) => void;
}

export const ExportItem: FunctionComponent<ItemProps> = ({ label, onActivate }) => (
  <div
    className="bi-basic-button cursor-pointer bi-down-list-item bi-list-item-active onlyoffice-export bi-f-v-c bi-f-h v-middle h-left"
    style={{ height: "30px", position: "relative" }}
    role="button"
    onClick={onActivate}
  >
    <ExportContent label={label} />
  </div>
);
