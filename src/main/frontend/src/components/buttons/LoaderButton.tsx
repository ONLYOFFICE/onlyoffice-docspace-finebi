import type { ComponentProps } from "preact";

import SpinnerSvg from "@resources/images/spinner.svg";

import "./generic.css";
import "./loader.css";

interface LoaderButtonProps extends Omit<ComponentProps<"button">, "disabled"> {
  loading: boolean;
  spinnerClass?: string;
}

export function LoaderButton({
  loading,
  children,
  className = "onlyoffice-button",
  spinnerClass = "onlyoffice-spinner--sm",
  type = "button",
  ...props
}: LoaderButtonProps) {
  return (
    <button className={className} type={type} {...props} disabled={loading}>
      {loading ? (
        <SpinnerSvg aria-hidden="true" className={`onlyoffice-spinner ${spinnerClass}`} />
      ) : (
        <span>{children}</span>
      )}
    </button>
  );
}
