import { createContext, useContext, useState } from 'react';
import api from './api/client';

const AuthContext = createContext(null);

function readUser() {
  try {
    return JSON.parse(localStorage.getItem('user') || 'null');
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readUser);

  const login = async (username, password) => {
    const { data } = await api.post('/auth/login', { username, password });
    localStorage.setItem('token', data.token);
    localStorage.setItem('user', JSON.stringify(data.user));
    setUser(data.user);
    return data.user;
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUser(null);
  };

  const refresh = (u) => {
    localStorage.setItem('user', JSON.stringify(u));
    setUser(u);
  };

  const can = (...roles) => !!user && roles.includes(user.role);

  return (
    <AuthContext.Provider value={{ user, login, logout, refresh, can }}>{children}</AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
