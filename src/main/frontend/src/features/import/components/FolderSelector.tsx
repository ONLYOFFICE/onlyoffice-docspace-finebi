import type { FolderEntry } from "@api/plugin";

interface FolderSelectorProps {
  value: string;
  folders: FolderEntry[];
  onChange: (id: string) => void;
}

export function FolderSelector({ value, folders, onChange }: FolderSelectorProps) {
  return (
    <div>
      <div className="onlyoffice-folder-dialog__label">
        Select destination folder:
      </div>
      <select
        className="onlyoffice-folder-dialog__select"
        value={value}
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
