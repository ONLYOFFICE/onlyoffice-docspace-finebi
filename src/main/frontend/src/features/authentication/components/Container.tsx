import type { ComponentChildren } from "preact";

import { Address, Lead, OnlyOfficeLogo, PageContainer } from "@components";

import "./container.css";

interface AuthenticationContainerProps {
  lead: string;
  address?: string;
  children: ComponentChildren;
}

export function AuthenticationContainer({ lead, address, children }: AuthenticationContainerProps) {
  return (
    <PageContainer className="onlyoffice-authentication-container">
      <div className="onlyoffice-authentication-container__column">
        <div className="onlyoffice-authentication-container__logo">
          <OnlyOfficeLogo />
        </div>
        <div className="onlyoffice-authentication-container__access">
          <Lead>{lead}</Lead>
          {address && <Address>{address}</Address>}
        </div>
        {children}
      </div>
    </PageContainer>
  );
}
