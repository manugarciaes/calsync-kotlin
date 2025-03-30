import { QueryClient, QueryFunction } from "@tanstack/react-query";

// API base URL - can be configured based on environment
const API_BASE_URL = "/api";

async function throwIfResNotOk(res: Response) {
  if (!res.ok) {
    try {
      // Try to parse JSON error response from Kotlin backend
      const errorData = await res.json();
      throw new Error(`${res.status}: ${errorData.error || res.statusText}`);
    } catch (e) {
      // If not JSON, use text or statusText
      const text = await res.text() || res.statusText;
      throw new Error(`${res.status}: ${text}`);
    }
  }
}

// Get auth token from local storage
function getAuthToken(): string | null {
  return localStorage.getItem("auth_token");
}

// Set auth token in local storage
export function setAuthToken(token: string): void {
  localStorage.setItem("auth_token", token);
}

// Remove auth token from local storage
export function removeAuthToken(): void {
  localStorage.removeItem("auth_token");
}

// Add authorization header if token exists
function getHeaders(hasBody: boolean = false): HeadersInit {
  const headers: HeadersInit = hasBody ? { "Content-Type": "application/json" } : {};
  const token = getAuthToken();
  
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }
  
  return headers;
}

export async function apiRequest(
  method: string,
  url: string,
  data?: unknown | undefined,
): Promise<Response> {
  // Ensure URL has the API prefix
  const fullUrl = url.startsWith("/api") ? url : `${API_BASE_URL}${url}`;
  
  const res = await fetch(fullUrl, {
    method,
    headers: getHeaders(!!data),
    body: data ? JSON.stringify(data) : undefined,
  });

  await throwIfResNotOk(res);
  return res;
}

type UnauthorizedBehavior = "returnNull" | "throw";
export const getQueryFn: <T>(options: {
  on401: UnauthorizedBehavior;
}) => QueryFunction<T> =
  ({ on401: unauthorizedBehavior }) =>
  async ({ queryKey }) => {
    const urlPath = queryKey[0] as string;
    const fullUrl = urlPath.startsWith("/api") ? urlPath : `${API_BASE_URL}${urlPath}`;
    
    const res = await fetch(fullUrl, {
      headers: getHeaders(),
    });

    if (unauthorizedBehavior === "returnNull" && res.status === 401) {
      // For 401 with returnNull behavior, silently return null
      return null;
    }

    await throwIfResNotOk(res);
    return await res.json();
  };

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      queryFn: getQueryFn({ on401: "throw" }),
      refetchInterval: false,
      refetchOnWindowFocus: false,
      staleTime: Infinity,
      retry: false,
    },
    mutations: {
      retry: false,
    },
  },
});
