import SpinnerSvg from "@resources/images/spinner.svg";

import "./spinner.css";

interface SpinnerProps {
  className?: string;
}

export function Spinner({ className = "onlyoffice-spinner--sm" }: SpinnerProps) {
  return <SpinnerSvg aria-hidden="true" className={`onlyoffice-spinner ${className}`} />;
}
