import { useState } from "preact/hooks";
import type { ComponentProps } from "preact";

import { Eye, EyeClosed } from "@components";

import "./common.css";
import "./password.css";

interface PasswordFieldProps extends Omit<ComponentProps<"input">, "type"> {
  id: string;
  label: string;
}

export function PasswordField({ label, className, ...props }: PasswordFieldProps) {
  const [show, setShow] = useState(false);
  return (
    <div className="onlyoffice-input onlyoffice-input--password">
      <label className="onlyoffice-input__label" htmlFor={props.id}>{label}</label>
      <div className="onlyoffice-input__control-wrap">
        <input
          className={["onlyoffice-input__control", className].filter(Boolean).join(" ")}
          {...props}
          type={show ? "text" : "password"}
        />
        <button
          type="button"
          className="onlyoffice-input__toggle"
          aria-label={show ? "Hide password" : "Show password"}
          onClick={() => setShow(!show)}
        >
          {show ? <Eye /> : <EyeClosed />}
        </button>
      </div>
    </div>
  );
}
