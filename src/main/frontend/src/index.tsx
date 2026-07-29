import { render } from "preact";

import type { IHostGlobal } from "@/types/host";

import { NavigationApp, PageApp } from "@/App";

import { useRegisterExportControl } from "@features/export";
import { useRegisterHeaderControl } from "@features/header";

import { PlaceholderPage } from "@pages/placeholder";

import { usePluginStore } from "@store/plugin";
import { useRegistryStore } from "@store/registry";

import { syncHostLocale } from "@i18n";

import "./index.css";

function getHostGlobal(): IHostGlobal | undefined {
  return (window as { BI?: IHostGlobal }).BI;
}

async function BootPage(root: HTMLElement): Promise<void> {
  try {
    await usePluginStore.getState().load();
    render(<PageApp />, root);
  } catch {
    render(<PlaceholderPage />, root);
  }
}

function BindHostGlobal(): void {
  const apply = () => {
    const BI = getHostGlobal();
    if (BI) {
      useRegistryStore.getState().setBi(BI);
      syncHostLocale();
    }
  };
  apply();
  if (!getHostGlobal() && document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", apply, { once: true });
  }
}

function BootNavigation(): void {
  BindHostGlobal();
  useRegisterHeaderControl();
  useRegisterExportControl();
  const root = document.createElement("div");
  (document.body ?? document.documentElement).appendChild(root);
  render(<NavigationApp />, root);
}

syncHostLocale();

const mount = document.getElementById("app");
if (mount) void BootPage(mount);
else BootNavigation();
