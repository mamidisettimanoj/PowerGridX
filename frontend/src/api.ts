import { useCallback, useEffect, useState } from 'react';

/** Base URL of the PowerGridX API. Override with VITE_API_URL (see frontend/.env.example). */
export const API_BASE: string = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

export async function apiGet<T>(path: string, signal?: AbortSignal): Promise<T> {
  const res = await fetch(`${API_BASE}${path}`, { signal });
  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    try {
      const body = await res.json();
      if (body && typeof body.message === 'string') message = body.message;
    } catch {
      /* response was not JSON */
    }
    throw new Error(message);
  }
  return (await res.json()) as T;
}

export interface ApiState<T> {
  data: T | null;
  error: string | null;
  loading: boolean;
  reload: () => void;
}

/** Fetches `path` and tracks loading / error state. Pass null to skip the request. */
export function useApi<T>(path: string | null): ApiState<T> {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(path !== null);
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    if (path === null) return;
    const controller = new AbortController();
    setLoading(true);
    setError(null);
    apiGet<T>(path, controller.signal)
      .then((d) => {
        setData(d);
        setLoading(false);
      })
      .catch((e: unknown) => {
        if (controller.signal.aborted) return;
        setError(e instanceof TypeError ? 'Cannot reach the PowerGridX API. Is the backend running?' : e instanceof Error ? e.message : 'Request failed');
        setLoading(false);
      });
    return () => controller.abort();
  }, [path, attempt]);

  const reload = useCallback(() => setAttempt((n) => n + 1), []);
  return { data, error, loading, reload };
}

/** Turns a thrown value into a message that is safe to show in the UI. */
export function errorMessage(e: unknown): string {
  if (e instanceof TypeError) return 'Cannot reach the PowerGridX API. Is the backend running?';
  return e instanceof Error ? e.message : 'Request failed';
}
