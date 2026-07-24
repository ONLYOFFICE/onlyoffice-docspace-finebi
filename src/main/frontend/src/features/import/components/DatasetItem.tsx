import type { FunctionComponent } from "preact";
import OnlyofficeIcon from "@resources/images/onlyoffice-logo.svg";

import { useLoggedIn } from "@hooks/useSessionMode";
import { useTranslation } from "@i18n";

import "./dataset.css";

interface ItemProps {
  onActivate?: (event: MouseEvent) => void;
}

export const DatasetItem: FunctionComponent<ItemProps> = ({ onActivate }) => {
  const translate = useTranslation();
  return (
    <div
      className="bi-basic-button cursor-pointer bi-down-list-item bi-list-item-active onlyoffice-import__item bi-f-v-c bi-f-h v-middle h-left"
      style={{ height: "30px", position: "relative" }}
      role="button"
      onClick={onActivate}
    >
      <div
        className="bi-f-c bi-f-h v-middle h-center f-s-n c-e f-c"
        style={{ width: "36px", height: "30px", position: "relative" }}
      >
        <OnlyofficeIcon width={20} height={20} aria-hidden="true" />
      </div>
      <div
        className="bi-label list-item-text bi-text f-s-n c-e l-c"
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
        {translate("import.menu")}
      </div>
    </div>
  );
};

export const DatasetMenuItem: FunctionComponent<ItemProps> = ({ onActivate }) => {
  const loggedIn = useLoggedIn();
  if (!loggedIn) return null;
  return <DatasetItem onActivate={onActivate} />;
};
