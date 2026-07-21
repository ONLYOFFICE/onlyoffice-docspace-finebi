declare module "*.svg" {
  import type { FunctionComponent } from "preact";
  const SvgComponent: FunctionComponent<preact.JSX.SVGAttributes<SVGSVGElement>>;
  export default SvgComponent;
}

declare module "*.css" {
  const css: string;
  export default css;
}

declare module "*?inline" {
  const content: string;
  export default content;
}
