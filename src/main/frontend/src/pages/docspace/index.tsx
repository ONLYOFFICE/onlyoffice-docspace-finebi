import { useTenantListener } from "@features/authentication/hooks/useTenantListener";
import { Room } from "./Room";

export function DocSpacePage() {
  useTenantListener();
  return <Room />;
}
