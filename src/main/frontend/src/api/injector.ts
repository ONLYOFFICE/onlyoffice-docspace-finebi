/** Describes how to detect a host element and inject content into it. */
export interface InjectionRule<T extends Element = Element> {
  /** CSS selector applied to each scanned document to find candidate hosts. */
  selector: string;
  /** Attribute written on the host after a successful injection (deduplication key). */
  marker: string;
  /** Runs once per document (the first time it is scanned) — e.g. inject shared styles. */
  setup?(doc: Document): void;
  /** Return false to skip a candidate; omit to accept all selector matches. */
  filter?(host: T): boolean;
  /**
   * Perform the injection into `host`.
   * Return `false` to skip marking — the next scan will retry (e.g. mount not ready yet).
   * Any other return value marks the host with `marker` to prevent future attempts.
   */
  inject(host: T, doc: Document): boolean | void;
}

interface RegisteredRule {
  rule: InjectionRule<Element>;
  /** Documents that have already had `rule.setup` run against them. */
  setupDone: WeakSet<Document>;
}

/**
 * Observes documents for matching elements and injects content via registered rules.
 * Rules are registered once; observation is started per-document.
 *
 * Performance: MutationObserver callbacks are coalesced with requestAnimationFrame so
 * burst DOM updates (chart redraws, data loads) trigger at most one scan per frame.
 * Per-document `setup` runs a single time; scans short-circuit on stale/detached documents.
 *
 * Note: `disconnect()` is terminal — create a fresh instance to observe again.
 */
export class DomInjector {
  private readonly rules: RegisteredRule[] = [];
  private readonly observedRoots = new WeakSet<Node>();
  private readonly observers: MutationObserver[] = [];
  private readonly pendingDocs = new Set<Document>();
  private scheduledFrame = 0;

  /** Register an injection rule. Returns a cleanup function to remove it. */
  addRule<T extends Element>(rule: InjectionRule<T>): () => void {
    const entry: RegisteredRule = { rule: rule as InjectionRule<Element>, setupDone: new WeakSet() };
    this.rules.push(entry);
    return () => {
      const i = this.rules.indexOf(entry);
      if (i >= 0) this.rules.splice(i, 1);
    };
  }

  /** Start observing a document: immediate scan + MutationObserver for future changes. */
  observe(doc: Document): void {
    const root = doc.body;
    if (!root || this.observedRoots.has(root)) return;
    this.observedRoots.add(root);
    this.scan(doc);
    const observer = new MutationObserver(() => this.schedule(doc));
    observer.observe(root, { childList: true, subtree: true });
    this.observers.push(observer);
  }

  /** Disconnect all observers and cancel any pending scan. Terminal — use a new instance to resume. */
  disconnect(): void {
    cancelAnimationFrame(this.scheduledFrame);
    this.scheduledFrame = 0;
    this.pendingDocs.clear();
    for (const obs of this.observers) obs.disconnect();
    this.observers.length = 0;
  }

  /** Run all registered rules against a single document immediately. */
  scan(doc: Document): void {
    // Skip when there is nothing to do or the document was detached (destroyed iframe).
    if (this.rules.length === 0 || !doc.body?.isConnected) return;
    for (const { rule, setupDone } of this.rules) {
      if (rule.setup && !setupDone.has(doc)) {
        setupDone.add(doc);
        rule.setup(doc);
      }
      for (const host of doc.querySelectorAll(rule.selector)) {
        if (host.hasAttribute(rule.marker)) continue;
        if (rule.filter && !rule.filter(host)) continue;
        if (rule.inject(host, doc) !== false) {
          host.setAttribute(rule.marker, "1");
        }
      }
    }
  }

  // Coalesce rapid mutations: collect affected documents, flush once per animation frame.
  private schedule(doc: Document): void {
    this.pendingDocs.add(doc);
    if (this.scheduledFrame) return;
    this.scheduledFrame = requestAnimationFrame(() => {
      this.scheduledFrame = 0;
      const docs = [...this.pendingDocs];
      this.pendingDocs.clear();
      for (const d of docs) this.scan(d);
    });
  }
}
