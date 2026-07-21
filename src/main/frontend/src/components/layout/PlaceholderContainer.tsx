import HorizontalLogoSvg from "@resources/images/horizontal-logo.svg";

import { Subtext } from "../typography/Subtext";
import { PageContainer } from "./PageContainer";

import OwlSvg from "@resources/images/owl.svg";

import "./placeholder.css";

interface PlaceholderContainerProps {
  header: string;
  message: string;
}

export function PlaceholderContainer({ header, message }: PlaceholderContainerProps) {
  return (
    <PageContainer className="onlyoffice-placeholder-container">
      <div className="onlyoffice-placeholder-container__logo">
        <HorizontalLogoSvg />
      </div>
      <div className="onlyoffice-placeholder-container__owl">
        <OwlSvg />
      </div>
      <div className="onlyoffice-placeholder-container__text">
        <span className="onlyoffice-placeholder-container__header">{header}</span>
        <Subtext>{message}</Subtext>
      </div>
    </PageContainer>
  );
}
