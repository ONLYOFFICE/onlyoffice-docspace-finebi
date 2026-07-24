import { Room } from "./Room";
import { useTenantListener } from "@features/authentication/hooks/useTenantListener";

export function DocSpacePage() {
  useTenantListener();
  return <Room />;
}
