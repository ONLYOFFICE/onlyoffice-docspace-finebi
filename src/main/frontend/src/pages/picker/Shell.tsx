import { Dialog } from "@components";

interface PickerShellProps {
  text: string;
  onClose: () => void;
}

export function PickerShell({ text, onClose }: PickerShellProps) {
  return (
    <Dialog title="Import from DocSpace" onClose={onClose}>
      <div class="onlyoffice-dialog__loader">{text}</div>
    </Dialog>
  );
}
