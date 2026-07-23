import { useCallback, useEffect } from "preact/hooks";

export type EventPublisher = {
  publish(type: string, detail?: Record<string, unknown>): void;
};

function postAncestors(payload: Record<string, unknown>): void {
  const seen = new Set<Window>();
  let current: Window | null = window;
  while (current && !seen.has(current)) {
    seen.add(current);
    let parent: Window | null = null;
    try {
      parent = current.parent;
    } catch {
      break;
    }

    if (!parent || parent === current) break;
    try {
      parent.postMessage(payload, "*");
    } catch {
      console.error("Failed to post message to parent frame");
    }

    current = parent;
  }
}

function postFrames(payload: Record<string, unknown>): void {
  const { origin } = window.location;
  for (const frame of document.querySelectorAll("iframe")) {
    try {
      if (new URL(frame.src, window.location.href).origin !== origin)
        continue;
      frame.contentWindow?.postMessage(payload, origin);
    } catch {
      console.error("Failed to post message to frame");
    }
  }
}

const publisher: EventPublisher = {
  publish(type, detail = {}) {
    const payload = { type, ...detail };
    window.dispatchEvent(new CustomEvent(type, { detail }));
    postAncestors(payload);
    postFrames(payload);
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
