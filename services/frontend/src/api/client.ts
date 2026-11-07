'use client';

import createClient, { type Middleware } from 'openapi-fetch';
import type { paths } from './schema';

export const API_URL = (process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8000').replace(/\/+$/, '');

/** Fired when the API answers 401: the session ended (logout elsewhere, expiry, suspension). */
export const UNAUTHENTICATED_EVENT = 'agrishare:unauthenticated';

const UNSAFE_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

// The session lives in an HttpOnly cookie set by the API. State-changing requests must also
// carry the CSRF token, which the frontend reads from GET /api/auth/csrf (the CSRF cookie is
// HttpOnly too, and belongs to the API's domain anyway).
let csrfToken: string | null = null;
let csrfRequest: Promise<string> | null = null;

async function fetchCsrfToken(): Promise<string> {
  const response = await fetch(`${API_URL}/api/auth/csrf`, { credentials: 'include' });
  if (!response.ok) {
    throw new Error(`CSRF token request failed (${response.status})`);
  }
  const body = (await response.json()) as { token: string };
  csrfToken = body.token;
  return body.token;
}

function getCsrfToken(): Promise<string> {
  if (csrfToken) {
    return Promise.resolve(csrfToken);
  }
  csrfRequest ??= fetchCsrfToken().finally(() => {
    csrfRequest = null;
  });
  return csrfRequest;
}

/** The API clears the CSRF cookie at login and logout: the next request fetches a new token. */
export function forgetCsrfToken() {
  csrfToken = null;
}

// Copies of the requests sent, so a request refused for a stale CSRF token can be sent again
const retryable = new WeakMap<Request, Request>();

const sessionMiddleware: Middleware = {
  async onRequest({ request }) {
    if (UNSAFE_METHODS.has(request.method)) {
      request.headers.set('X-XSRF-TOKEN', await getCsrfToken());
      retryable.set(request, request.clone());
    }
    return request;
  },

  async onResponse({ request, response }) {
    const path = new URL(request.url).pathname;
    if (path === '/api/auth/login' || path === '/api/auth/logout' || path === '/api/auth/logout-all') {
      forgetCsrfToken();
    }

    if (response.status === 403) {
      const copy = retryable.get(request);
      const body = await response.clone().json().catch(() => null);
      if (copy && body?.code === 'CSRF') {
        // Token expired or cleared: get a new one and send the request once more
        forgetCsrfToken();
        copy.headers.set('X-XSRF-TOKEN', await getCsrfToken());
        return fetch(copy, { credentials: 'include' });
      }
    }

    if (response.status === 401 && !['/api/auth/me', '/api/auth/login', '/api/auth/logout'].includes(path)) {
      window.dispatchEvent(new Event(UNAUTHENTICATED_EVENT));
    }
    return response;
  },
};

export const apiClient = createClient<paths>({
  baseUrl: API_URL,
  credentials: 'include',
});

apiClient.use(sessionMiddleware);
