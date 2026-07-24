import { useLocation } from "preact-iso";

import { PickerPage } from "@pages/picker";
import { DocSpacePage } from "@pages/docspace";
import { SettingsPage } from "@pages/settings";
import { PlaceholderPage } from "@pages/placeholder";
import { AuthenticationPage } from "@pages/authentication";

import { ImportUrlUtils } from "@features/import/utils/url";

import { usePluginStore } from "@store/plugin";

import manifest from "@manifest";

function AdminRoutes() {
  const mode = usePluginStore((s) => s.config!.mode);
  switch (mode) {
    case "setup":
    case "admin":
      return <AuthenticationPage />;
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
