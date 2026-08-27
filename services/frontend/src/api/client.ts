'use client';

import createClient, { type Middleware } from 'openapi-fetch';
import type { paths } from './schema';
import { getAuth } from 'firebase/auth';

const authMiddleware: Middleware = {
  async onRequest({ request }) {
    const token = await getAuth().currentUser?.getIdToken();
    if (token) {
      request.headers.set('Authorization', `Bearer ${token}`);
    }
    return request;
  },
};

export const apiClient = createClient<paths>({
  baseUrl: process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8000',
});

apiClient.use(authMiddleware);
