import { useCallback, useEffect } from "preact/hooks";

export type EventPublisher = {
  publish(type: string, detail?: Record<string, unknown>): void;
};

function postParent(payload: Record<string, unknown>): void {
  if (!window.parent || window.parent === window) return;
  try {
    window.parent.postMessage(payload, "*");
  } catch {
    console.error("Failed to post message to parent");
  }
}

const publisher: EventPublisher = {
  publish(type, detail = {}) {
    window.dispatchEvent(new CustomEvent(type, { detail }));
    postParent({ type, ...detail });
  },
};

export function useEventPublisher(
  type?: string,
  detail: Record<string, unknown> = {},
  options: { clear?: Record<string, unknown>; enabled?: boolean } = {},
): EventPublisher {
  const { clear, enabled = true } = options;
  const detailKey = JSON.stringify(detail);
  const clearKey = clear ? JSON.stringify(clear) : "";

  useEffect(() => {
    if (!enabled || !type) return;
    publisher.publish(type, JSON.parse(detailKey) as Record<string, unknown>);
    return () => {
      if (clearKey) publisher.publish(type, JSON.parse(clearKey) as Record<string, unknown>);
    };
  }, [type, detailKey, clearKey, enabled]);

  const publish = useCallback<EventPublisher["publish"]>(
    (eventType, eventDetail) => publisher.publish(eventType, eventDetail),
    [],
  );

  return { publish };
}
