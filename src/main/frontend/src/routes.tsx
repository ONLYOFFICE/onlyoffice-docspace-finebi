import { useLocation } from "preact-iso";
import manifest from "@manifest";

import { usePluginStore } from "@store/plugin";
import { ImportUrlUtils } from "@features/import/utils/url";
import { AuthenticationPage } from "@pages/authentication";
import { DocSpacePage } from "@pages/docspace";
import { PickerPage } from "@pages/picker";
import { PlaceholderPage } from "@pages/placeholder";
import { SettingsPage } from "@pages/settings";

function AdminRoutes() {
  const mode = usePluginStore((s) => s.config!.mode);
  switch (mode) {
    case "setup":
    case "admin":
      return <AuthenticationPage key={mode} />;
    case "settings":
      return <SettingsPage />;
    default:
      return <PlaceholderPage />;
  }
}

function MainRoutes() {
  const mode = usePluginStore((s) => s.config!.mode);
  switch (mode) {
    case "docspace":
      return ImportUrlUtils.isPicker() ? <PickerPage /> : <DocSpacePage />;
    case "user":
      return <AuthenticationPage />;
    default:
      return <PlaceholderPage />;
  }
}

export function AppRoutes() {
  const { path } = useLocation();
  return path.includes("/url" + manifest.aliases.admin.from) ? <AdminRoutes /> : <MainRoutes />;
}
