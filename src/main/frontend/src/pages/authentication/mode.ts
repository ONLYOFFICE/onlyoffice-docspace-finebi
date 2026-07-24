import { PluginCoreAuthenticationMode } from "@/api/plugin";
import { translate } from "@i18n";

interface ModeText {
  title: string;
  lead: string;
  submit: string;
  emailLabel: string;
  emailPlaceholder: string;
  passwordLabel: string;
  passwordPlaceholder: string;
}

export function authModeText(mode: PluginCoreAuthenticationMode): ModeText {
  if (mode === "setup") {
    return {
      title: translate("auth.title.setup"),
      lead: translate("auth.lead.setup"),
      submit: translate("auth.submit.setup"),
      emailLabel: translate("auth.email.label"),
      emailPlaceholder: translate("auth.email.placeholder"),
      passwordLabel: translate("auth.password.label"),
      passwordPlaceholder: translate("auth.password.placeholder"),
    };
  }

  return {
    title: translate("auth.title.login"),
    lead: translate("auth.lead.login"),
    submit: translate("auth.submit.login"),
    emailLabel: translate("auth.email.label"),
    emailPlaceholder: translate("auth.email.placeholder"),
    passwordLabel: translate("auth.password.label"),
    passwordPlaceholder: translate("auth.password.placeholder"),
  };
}
