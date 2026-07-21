import "./address.css";

interface AddressProps {
  children: string;
}

export function Address({ children }: AddressProps) {
  return <span className="onlyoffice-address">{children}</span>;
}
