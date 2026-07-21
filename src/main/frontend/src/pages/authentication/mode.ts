import { PluginCoreAuthenticationMode } from "@/api/plugin";

interface ModeText {
  title: string;
  lead: string;
  submit: string;
  emailLabel: string;
  emailPlaceholder: string;
  passwordLabel: string;
  passwordPlaceholder: string;
}

export const MODES: Record<PluginCoreAuthenticationMode, ModeText> = {
  setup: {
    title: "Connect DocSpace",
    lead:
      "Enter your DocSpace server address and sign in with an admin account " +
      "to configure the tenant for all FineBI users.",
    submit: "Save and continue",
    emailLabel: "DocSpace Email",
    emailPlaceholder: "Please enter DocSpace email",
    passwordLabel: "DocSpace Password",
    passwordPlaceholder: "Please enter DocSpace password",
  },
  admin: {
    title: "Sign in to DocSpace",
    lead: "FineBI requests access to your ONLYOFFICE DocSpace",
    submit: "Login",
    emailLabel: "DocSpace Email",
    emailPlaceholder: "Please enter DocSpace email",
    passwordLabel: "DocSpace Password",
    passwordPlaceholder: "Please enter DocSpace password",
  },
  user: {
    title: "Sign in to DocSpace",
    lead: "FineBI requests access to your ONLYOFFICE DocSpace",
    submit: "Login",
    emailLabel: "DocSpace Email",
    emailPlaceholder: "Please enter DocSpace email",
    passwordLabel: "DocSpace Password",
    passwordPlaceholder: "Please enter DocSpace password",
  },
};
