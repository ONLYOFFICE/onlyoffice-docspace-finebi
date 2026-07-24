import { useCallback, useEffect } from "preact/hooks";

export type EventPublisher = {
  publish(type: string, detail?: Record<string, unknown>): void;
};

function rootWindow(): Window {
  let root: Window = window;
  try {
    while (root.parent && root.parent !== root) {
      void root.parent.document;
      root = root.parent;
    }
  } catch {
    // ignore
  }

  return root;
}

/**
 * Deliver to every same-origin window in the frame tree — not just the direct
 * parent/children. FineBI hosts each plugin page (manager, settings, picker) in
 * a sibling card iframe, so a message published from one must climb to the top
 * and fan back down to reach the others. The origin window is skipped: it is
 * handled by the local {@link CustomEvent} dispatch, not by postMessage.
 */
function broadcast(payload: Record<string, unknown>): void {
  const { origin } = window.location;
  const seen = new Set<Window>();
  const stack: Window[] = [rootWindow()];

  while (stack.length) {
    const win = stack.pop();
    if (!win || seen.has(win)) continue;
    seen.add(win);

    let doc: Document;
    try {
      doc = win.document;
    } catch {
      continue; // cross-origin window — cannot enumerate its frames
    }

    if (win !== window) {
      try {
        win.postMessage(payload, origin);
      } catch {
        console.error("Failed to post message to frame");
      }
    }

    for (const frame of doc.querySelectorAll("iframe")) {
      try {
        if (new URL(frame.src, win.location.href).origin !== origin) continue;
      } catch {
        continue;
      }
      const child = frame.contentWindow;
      if (child && !seen.has(child)) stack.push(child);
    }
  }
}

const publisher: EventPublisher = {
  publish(type, detail = {}) {
    const payload = { type, ...detail };
    window.dispatchEvent(new CustomEvent(type, { detail }));
    broadcast(payload);
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
