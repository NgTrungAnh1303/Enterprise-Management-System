import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { authApi } from '../api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => sessionStorage.getItem('ems_token'));
  const [user, setUser] = useState(() => {
    const saved = sessionStorage.getItem('ems_user');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch {
        return null;
      }
    }
    return null;
  });
  const [loading, setLoading] = useState(true);

  
  useEffect(() => {
    const initAuth = async () => {
      const storedToken = sessionStorage.getItem('ems_token');
      if (storedToken) {
        try {
          const userData = await authApi.getMe();
          setUser(userData);
          sessionStorage.setItem('ems_user', JSON.stringify(userData));
        } catch {
          // Token expired or invalid
          sessionStorage.removeItem('ems_token');
          sessionStorage.removeItem('ems_user');
          setToken(null);
          setUser(null);
        }
      }
      setLoading(false);
    };

    initAuth();
  }, []);

  const login = useCallback(async (username, password) => {
    const res = await authApi.login({ username, password });
    const accessToken = res.accessToken;
    const userData = res.user;

    sessionStorage.setItem('ems_token', accessToken);
    sessionStorage.setItem('ems_user', JSON.stringify(userData));

    setToken(accessToken);
    setUser(userData);
    return userData;
  }, []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      
    } finally {
      sessionStorage.removeItem('ems_token');
      sessionStorage.removeItem('ems_user');
      setToken(null);
      setUser(null);
    }
  }, []);

  const value = {
    user,
    token,
    role: user?.role || null,
    isAuthenticated: Boolean(token && user),
    loading,
    login,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

export default AuthContext;

