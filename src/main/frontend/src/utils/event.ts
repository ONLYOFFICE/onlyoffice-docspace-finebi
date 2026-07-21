export type EventType = string;
export type EventAction = string;

export const EventUtils = Object.freeze({
  send(type: EventType, action: EventAction, target?: string): void {
    window.parent.postMessage({ type, action }, target ?? window.location.origin);
  },
});