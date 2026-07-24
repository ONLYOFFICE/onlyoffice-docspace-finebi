import type { FolderEntry } from "@api/plugin";
import { useTranslation } from "@i18n";

interface FolderSelectorProps {
  value: string;
  folders: FolderEntry[];
  onChange: (id: string) => void;
  disabled?: boolean;
}

export function FolderSelector({
  value, folders, onChange, disabled }: FolderSelectorProps) {
  const translate = useTranslation();
  return (
    <div>
      <div className="onlyoffice-folder-dialog__label">
        {translate("import.folder.label")}
      </div>
      <select
        className="onlyoffice-folder-dialog__select"
        value={value}
        disabled={disabled}
        onChange={(e) => onChange((e.target as HTMLSelectElement).value)}
      >
        {folders.map((f) => (
          <option key={f.id} value={f.id}>
            {f.name}
          </option>
        ))}
      </select>
    </div>
  );
}
