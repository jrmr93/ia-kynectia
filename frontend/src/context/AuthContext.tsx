import React, { createContext, useContext, useState, useEffect } from 'react';
import type { AdminUser } from '../types';
import { apiRequest, setToken, clearToken, getToken } from '../api/client';

interface AuthContextType {
  admin: AdminUser | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  hasPermission: (permissionCode: string) => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [admin, setAdmin] = useState<AdminUser | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  const fetchProfile = async () => {
    const token = getToken();
    if (!token) {
      setAdmin(null);
      setLoading(false);
      return;
    }

    try {
      const response = await apiRequest<AdminUser>('/auth/me');
      setAdmin(response.data);
    } catch {
      clearToken();
      setAdmin(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProfile();

    const handleUnauthorized = () => {
      setAdmin(null);
    };

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => {
      window.removeEventListener('auth:unauthorized', handleUnauthorized);
    };
  }, []);

  const login = async (email: string, password: string) => {
    const response = await apiRequest<{ token: string; admin: AdminUser }>('/auth/login', {
      method: 'POST',
      body: { email, password },
    });

    setToken(response.data.token);
    setAdmin(response.data.admin);
  };

  const logout = async () => {
    try {
      await apiRequest('/auth/logout', { method: 'POST' });
    } catch {
      // Ignorar errores al cerrar sesión
    } finally {
      clearToken();
      setAdmin(null);
    }
  };

  const hasPermission = (permissionCode: string): boolean => {
    if (!admin) return false;
    if (admin.roleName === 'superadmin') return true;
    return admin.permissions?.includes(permissionCode) ?? false;
  };

  return (
    <AuthContext.Provider value={{ admin, loading, login, logout, hasPermission }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth debe ser utilizado dentro de un AuthProvider');
  }
  return context;
};
