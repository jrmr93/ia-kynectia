const BASE_URL = 'http://localhost:4000/api/v1';

export class ApiError extends Error {
  statusCode: number;
  errors?: any;

  constructor(message: string, statusCode: number, errors?: any) {
    super(message);
    this.name = 'ApiError';
    this.statusCode = statusCode;
    this.errors = errors;
  }
}

export const getToken = (): string | null => {
  return localStorage.getItem('kynectia_admin_token');
};

export const setToken = (token: string): void => {
  localStorage.setItem('kynectia_admin_token', token);
};

export const clearToken = (): void => {
  localStorage.removeItem('kynectia_admin_token');
  localStorage.removeItem('kynectia_admin_user');
};

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  body?: any;
  params?: Record<string, any>;
  headers?: Record<string, string>;
  isFormData?: boolean;
}

export const apiRequest = async <T = any>(
  endpoint: string,
  options: RequestOptions = {}
): Promise<{ success: boolean; message?: string; data: T; pagination?: any }> => {
  const { method = 'GET', body, params, headers = {}, isFormData = false } = options;

  let url = `${BASE_URL}${endpoint}`;
  if (params) {
    const query = new URLSearchParams();
    Object.entries(params).forEach(([key, val]) => {
      if (val !== undefined && val !== null && val !== '') {
        query.append(key, String(val));
      }
    });
    const queryString = query.toString();
    if (queryString) {
      url += (url.includes('?') ? '&' : '?') + queryString;
    }
  }

  const token = getToken();
  const requestHeaders: Record<string, string> = {
    ...headers,
  };

  if (token) {
    requestHeaders['Authorization'] = `Bearer ${token}`;
  }

  if (!isFormData) {
    requestHeaders['Content-Type'] = 'application/json';
  }

  try {
    const response = await fetch(url, {
      method,
      headers: requestHeaders,
      body: isFormData ? body : body ? JSON.stringify(body) : undefined,
    });

    const data = await response.json().catch(() => null);

    if (!response.ok) {
      if (response.status === 401) {
        clearToken();
        // Emitir evento para reaccionar en el contexto
        window.dispatchEvent(new Event('auth:unauthorized'));
      }
      throw new ApiError(
        data?.message || `Error en la solicitud HTTP (${response.status})`,
        response.status,
        data?.errors
      );
    }

    return data;
  } catch (error: any) {
    if (error instanceof ApiError) {
      throw error;
    }
    throw new ApiError(
      error.message || 'No fue posible conectar con el servidor backend.',
      0
    );
  }
};
