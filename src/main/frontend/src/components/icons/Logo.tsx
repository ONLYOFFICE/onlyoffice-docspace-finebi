import LogoSvg from "@resources/images/onlyoffice-logo.svg";

interface LogoProps {
  size?: number;
}

export function OnlyOfficeLogo({ size = 68 }: LogoProps) {
  return <LogoSvg width={size} height={size} aria-label="ONLYOFFICE" />;
}
