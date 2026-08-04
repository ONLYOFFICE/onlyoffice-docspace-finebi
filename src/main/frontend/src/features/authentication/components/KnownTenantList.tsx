import { Cross } from "@/components/icons/Cross";

import type { PluginCoreKnownTenant } from "@api/plugin";

import { UrlUtils } from "@utils/url";

import { useTranslation } from "@i18n";

import "./tenants.css";

interface KnownTenantListProps {
  tenants: PluginCoreKnownTenant[];
  disabled?: boolean;
  signedInTenantUrl?: string;
  onSelect?: (tenant: PluginCoreKnownTenant) => void;
  onRemove: (tenant: PluginCoreKnownTenant) => void;
}

export function KnownTenantList({
  tenants,
  disabled,
  signedInTenantUrl = "",
  onSelect,
  onRemove,
}: KnownTenantListProps) {
  const translate = useTranslation();
  if (tenants.length === 0) return null;

  const signedIn = UrlUtils.normalize(signedInTenantUrl);

  return (
    <div className="onlyoffice-known-tenants">
      <ul className="onlyoffice-known-tenants__list">
        {tenants.map((tenant) => {
          const host = UrlUtils.extractHost(tenant.url) || tenant.url;
          const isCurrent =
            signedIn.length > 0 && UrlUtils.normalize(tenant.url) === signedIn;
          return (
            <li
              key={tenant.url}
              className={[
                "onlyoffice-known-tenants__item",
                isCurrent ? "onlyoffice-known-tenants__item--active" : "",
              ]
                .filter(Boolean)
                .join(" ")}
            >
              <button
                type="button"
                className="onlyoffice-known-tenants__select"
                disabled={disabled || isCurrent || !onSelect}
                onClick={() => onSelect?.(tenant)}
                title={tenant.url}
              >
                <span className="onlyoffice-known-tenants__host">{host}</span>
                {tenant.email ? (
                  <span className="onlyoffice-known-tenants__email">{tenant.email}</span>
                ) : null}
                {isCurrent ? (
                  <span className="onlyoffice-known-tenants__badge">
                    {translate("settings.tenants.active")}
                  </span>
                ) : null}
              </button>
              <button
                type="button"
                className="onlyoffice-known-tenants__remove"
                disabled={disabled}
                aria-label={translate("settings.tenants.remove")}
                title={translate("settings.tenants.remove")}
                onClick={(event) => {
                  event.preventDefault();
                  event.stopPropagation();
                  onRemove(tenant);
                }}
              >
                <Cross />
              </button>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
