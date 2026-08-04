import { PlaceholderContainer } from "@components";
import { ImportUrlUtils } from "@features/import/utils/url";
import { useTenantListener } from "@features/authentication/hooks/useTenantListener";

import { useEventPublisher } from "@hooks/useEventPublisher";

import { usePluginStore } from "@store/plugin";

import { useTranslation } from "@i18n";
import manifest from "@manifest";

export function PlaceholderPage() {
  const translate = useTranslation();
  const config = usePluginStore((s) => s.config);

  const picker = ImportUrlUtils.isPicker();

  useEventPublisher(
    manifest.events.frontend.filePicker,
    { action: "close" },
    { enabled: picker },
  );

  useTenantListener();

  if (picker) return null;

  if (!config) {
    return (
      <PlaceholderContainer
        header={translate("placeholder.load.failed.header")}
        message={translate("placeholder.load.failed.message")}
      />
    );
  }

  const unauthorized = config.mode === "unauthorized";
  const header = unauthorized
    ? translate("placeholder.access.denied")
    : translate("placeholder.please.wait");
  const message =
    config.mode === "notconfigured"
      ? translate("placeholder.not.configured")
      : unauthorized
        ? translate("placeholder.access.denied.message")
        : config.response.message;
  return <PlaceholderContainer header={header} message={message} />;
}
