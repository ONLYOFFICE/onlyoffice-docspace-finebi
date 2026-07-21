import { useDocSpaceStore } from "@store/docspace";

import "./frame.css";

export function Frame() {
  const visible = useDocSpaceStore((s) => s.frameVisible);
  const frameId = useDocSpaceStore((s) => s.frameId());

  return (
    <div className={`onlyoffice-frame-host${visible ? " onlyoffice-frame-host--visible" : ""}`}>
      <div id="onlyoffice-frame-container">
        <div id={frameId} />
      </div>
    </div>
  );
}
