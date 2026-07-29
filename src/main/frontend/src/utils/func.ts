export const FuncUtils = Object.freeze({
  sleep(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
  },
  errorMessage(err: unknown): string {
    return err instanceof Error && err.message ? err.message : String(err);
  },
  withTimeout<T>(promise: Promise<T>, ms: number, onTimeout: () => Error): Promise<T> {
    return new Promise<T>((resolve, reject) => {
      let settled = false;
      const timer = window.setTimeout(() => {
        if (settled) return;
        settled = true;
        reject(onTimeout());
      }, ms);

      promise.then(
        (value) => {
          if (settled) return;
          settled = true;
          window.clearTimeout(timer);
          resolve(value);
        },
        (err) => {
          if (settled) return;
          settled = true;
          window.clearTimeout(timer);
          reject(err);
        },
      );
    });
  },
});
