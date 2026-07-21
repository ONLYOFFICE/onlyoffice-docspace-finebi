import "./toast.css";

interface ToastProps {
  message: string;
  type?: "success" | "error";
}

export function Toast({ message, type }: ToastProps) {
  return <div className={`onlyoffice-toast onlyoffice-toast--${type}`}>{message}</div>;
}
