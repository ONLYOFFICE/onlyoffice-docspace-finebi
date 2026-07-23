import { useEffect, useState } from "preact/hooks";
import manifest from "@manifest";

function isViewOpen(): boolean {
  const marker = `/url${manifest.aliases.main.from}`;
  for (const frame of document.querySelectorAll("iframe")) {
    const src = frame.getAttribute("src") ?? "";
    if (!src.includes(marker) || src.includes("/admin"))
      continue;

    const rectangle = frame.getBoundingClientRect();
    if (rectangle.width < 80 || rectangle.height < 80)
      continue;

    const style = window.getComputedStyle(frame);
    if (style.display === "none" || style.visibility === "hidden" || style.opacity === "0")
      continue;

    return true;
  }

  return false;
}

export function useViewOpen(): boolean {
  const [open, setOpen] = useState(false);

  useEffect(() => {
    let frame = 0;
    const update = () => setOpen(isViewOpen());
    const schedule = () => {
      if (frame) return;
      frame = requestAnimationFrame(() => {
        frame = 0;
        update();
      });
    };

    update();

    const observer = new MutationObserver(schedule);
    observer.observe(document.body, {
      childList: true,
      subtree: true,
      attributes: true,
      attributeFilter: ["style", "class", "src"],
    });

    window.addEventListener("focus", update);

    return () => {
      observer.disconnect();
      window.removeEventListener("focus", update);
      if (frame) cancelAnimationFrame(frame);
    };
  }, []);

  return open;
}
