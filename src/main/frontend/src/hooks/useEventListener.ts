import { useEffect } from "preact/hooks";

export function useEventListener(
  type: string,
  onEvent: (data: Record<string, unknown>) => void,
): void {
  useEffect(() => {
    const onCustom = (event: Event) => {
      const detail = (event as CustomEvent<Record<string, unknown>>).detail;
      onEvent(detail && typeof detail === "object" ? { type, ...detail } : { type });
    };

    const onMessage = (event: MessageEvent) => {
      const data = event.data as Record<string, unknown> | null;
      if (!data || data.type !== type) return;
      onEvent(data);
    };

    window.addEventListener(type, onCustom as EventListener);
    window.addEventListener("message", onMessage);
    return () => {
      window.removeEventListener(type, onCustom as EventListener);
      window.removeEventListener("message", onMessage);
    };
  }, [type, onEvent]);
}
