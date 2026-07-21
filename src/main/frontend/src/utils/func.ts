export const FuncUtils = Object.freeze({
  sleep(ms: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, ms));
  },
  errorMessage(err: unknown): string {
    return err instanceof Error && err.message ? err.message : String(err);
  },
});
