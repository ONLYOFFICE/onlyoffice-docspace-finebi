import type { ComponentProps } from "preact";

import "./dialog.css";

interface DialogButtonProps extends ComponentProps<"button"> {
  primary?: boolean;
}

export function DialogButton({ primary, children, type = "button", ...props }: DialogButtonProps) {
  const cls = ["onlyoffice-dialog__button", primary && "onlyoffice-dialog__button--primary"]
    .filter(Boolean)
    .join(" ");
  return <button className={cls} type={type} {...props}>{children}</button>;
}
