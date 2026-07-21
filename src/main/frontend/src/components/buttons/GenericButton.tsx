import type { ComponentProps } from "preact";

import "./generic.css";

type GenericButtonProps = ComponentProps<"button">;

export function GenericButton({
  children,
  className = "onlyoffice-button",
  type = "button",
  ...props
}: GenericButtonProps) {
  return (
    <button className={className} type={type} {...props}>
      {children}
    </button>
  );
}
