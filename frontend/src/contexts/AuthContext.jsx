import React, { createContext, useContext, useState, useEffect } from 'react';
import api from '../utils/axios';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    const token = localStorage.getItem('token');
    if (storedUser && token) {
      setUser(JSON.parse(storedUser));
    }
    setLoading(false);
  }, []);

  const storeSession = ({ token, user }) => {
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(user));
    setUser(user);
    return user;
  };

  const login = async (rollNumber, password) => {
    const response = await api.post('/auth/login', { rollNumber, password });
    if (response.data.otpRequired) return response.data;
    return { otpRequired: false, user: storeSession(response.data.login) };
  };

  const verifyLoginOtp = async (challengeId, otp) => {
    const response = await api.post('/auth/login/verify-otp', { challengeId, otp });
    return storeSession(response.data);
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUser(null);
  };

  if (loading) {
    return <div>Loading...</div>;
  }

  return (
    <AuthContext.Provider value={{ user, login, verifyLoginOtp, logout, isAuthenticated: !!user }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
