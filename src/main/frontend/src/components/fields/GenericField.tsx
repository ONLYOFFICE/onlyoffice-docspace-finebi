import type { ComponentProps } from "preact";

import "./common.css";

interface GenericFieldProps extends ComponentProps<"input"> {
  id: string;
  label: string;
}

export function GenericField({ label, className, ...props }: GenericFieldProps) {
  return (
    <div className="onlyoffice-input">
      <label className="onlyoffice-input__label" htmlFor={props.id}>{label}</label>
      <input
        className={["onlyoffice-input__control", className].filter(Boolean).join(" ")}
        {...props}
      />
    </div>
  );
}
