import "./form.css";

interface FormErrorProps {
  message: string;
}

export function FormError({ message }: FormErrorProps) {
  if (!message) return null;

  return (
    <p className="onlyoffice-form-error" role="alert">
      {message}
    </p>
  );
}
