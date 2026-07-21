import { render } from "preact";

import { NavigationApp, PageApp } from "@/App";
import { usePageStore } from "@store/page";
import { useRegistryStore } from "@store/registry";
import { PlaceholderPage } from "@pages/placeholder";
import type { IHostGlobal } from "@/types/host";

import "./index.css";

function getHostGlobal(): IHostGlobal | undefined {
  return (window as { BI?: IHostGlobal }).BI;
}

async function BootPage(root: HTMLElement): Promise<void> {
  try {
    await usePageStore.getState().load();
    render(<PageApp />, root);
  } catch {
    render(<PlaceholderPage />, root);
  }
}

function BindHostGlobal(): void {
  const apply = () => {
    const BI = getHostGlobal();
    if (BI) useRegistryStore.getState().setBi(BI);
  };
  apply();
  if (!getHostGlobal() && document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", apply, { once: true });
  }
}

function BootNavigation(): void {
  BindHostGlobal();
  const root = document.createElement("div");
  (document.body ?? document.documentElement).appendChild(root);
  render(<NavigationApp />, root);
}

const mount = document.getElementById("app");
if (mount) void BootPage(mount);
else BootNavigation();
