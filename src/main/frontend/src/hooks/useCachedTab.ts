import { useEffect } from "preact/hooks";

export function useCachedTab(onReopen: () => void): void {
  useEffect(() => {
    let wasHidden = false;
    const observer = new IntersectionObserver((entries) => {
      for (const entry of entries) {
        if (!entry.isIntersecting) {
          wasHidden = true;
        } else if (wasHidden) {
          wasHidden = false;
          onReopen();
        }
      }
    });

    observer.observe(document.body);
    return () => observer.disconnect();
  }, []);
}
