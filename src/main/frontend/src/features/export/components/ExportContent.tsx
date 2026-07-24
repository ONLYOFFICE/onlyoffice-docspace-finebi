import type { FunctionComponent } from "preact";

import OnlyofficeIcon from "@resources/images/onlyoffice-logo.svg";

import { translate } from "@i18n";

interface ContentProps {
  label?: string;
}

export const ExportContent: FunctionComponent<ContentProps> = ({
  label = translate("export.menu"),
}) => (
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
