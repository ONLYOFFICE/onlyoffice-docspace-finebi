import { render, type ComponentChild } from "preact";

/**
 * Renders Preact vnodes into real DOM — either into a container you provide, or
 * into a throwaway container from which the produced element is extracted and
 * detached (ready to move into the page, including cross-document into iframes).
 */
export class Renderer {
  /** Render `node` into an existing `container` and return it. */
  mount(node: ComponentChild, container: Element): Element {
    render(node, container);
    return container;
  }

  /**
   * Render `node` inside a throwaway container and return its single root
   * element, detached and ready to move into the page.
   *
   * Defaults to a `<div>` created in the global document. Pass `doc` to build
   * inside another document (e.g. an iframe), or `container` to use a custom
   * wrapper element instead of a `<div>`.
   */
  toElement(
    node: ComponentChild,
    { doc = document, container = doc.createElement("div") }: { doc?: Document; container?: Element } = {},
  ): HTMLElement {
    render(node, container);
    const element = container.firstElementChild;

    if (!(element instanceof HTMLElement))
      throw new Error("Renderer: component did not produce a single root element");

    element.remove();
    return element;
  }
}
