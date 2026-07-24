import { create } from "zustand";
import type { ComponentChild } from "preact";
import { Renderer } from "@api/renderer";

interface RendererState {
  /** Render `node` into an existing `container` and return it. */
  mount(node: ComponentChild, container: Element): Element;
  /** Unmount the root previously mounted into `container`. */
  unmount(container: Element): void;
  /** Render `node` and return its single root element, detached from a throwaway container. */
  toElement(node: ComponentChild, options?: { doc?: Document; container?: Element }): HTMLElement;
}

export const useRendererStore = create<RendererState>()(() => {
  const renderer = new Renderer();
  return {
    mount: (node, container) => renderer.mount(node, container),
    unmount: (container) => renderer.unmount(container),
    toElement: (node, options) => renderer.toElement(node, options),
  };
});
