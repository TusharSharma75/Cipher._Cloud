import { createContext, useContext, useState, useEffect, useCallback, useRef, type ReactNode } from 'react';
import type { User } from '@/types';
import apiService from '@/services/api';

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (usernameOrEmail: string, password: string) => Promise<{ success: boolean; requiresOtp?: boolean; otpSessionId?: string; message?: string }>;
  verifyOtp: (otpSessionId: string, otpCode: string) => Promise<{ success: boolean; message?: string }>;
  signup: (username: string, email: string, password: string, confirmPassword: string, adminSecret?: string) => Promise<{ success: boolean; message?: string }>;
  logout: () => void;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const isLoadingRef = useRef(true);

  const logout = useCallback(() => {
    apiService.logoutApi().catch(console.error);
    setUser(null);
  }, []);

  const refreshUser = useCallback(async () => {
    try {
      const response = await apiService.getCurrentUser();
      if (response.success && response.data) {
        setUser(response.data);
      }
    } catch (error) {
      console.error('Failed to refresh user:', error);
      logout();
    }
  }, [logout]);

  useEffect(() => {
    const { accessToken } = apiService.loadTokens();
    if (accessToken) {
      refreshUser().finally(() => {
        if (isLoadingRef.current) {
          isLoadingRef.current = false;
          setIsLoading(false);
        }
      });
    } else {
      isLoadingRef.current = false;
      setIsLoading(false);
    }
  }, [refreshUser]);

  const login = async (usernameOrEmail: string, password: string) => {
    try {
      const response = await apiService.login({ usernameOrEmail, password });
      if (response.success && response.data) {
        if (response.data.requiresOtp) {
          return { 
            success: true, 
            requiresOtp: true, 
            otpSessionId: response.data.otpSessionId 
          };
        }
        setUser({
          id: response.data.userId,
          username: response.data.username,
          email: response.data.email,
          role: response.data.role as 'ADMIN' | 'USER' | 'GUEST',
          otpEnabled: response.data.otpEnabled,
          storageQuota: response.data.storageQuota,
          usedStorage: response.data.usedStorage,
          storageUsagePercentage: response.data.storageUsagePercentage,
          accountLocked: false,
        });
        return { success: true };
      }
      return { success: false, message: response.message || 'Login failed' };
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } };
      return { success: false, message: err.response?.data?.message || 'Login failed' };
    }
  };

  const verifyOtp = async (otpSessionId: string, otpCode: string) => {
    try {
      const response = await apiService.verifyOtp({ otpSessionId, otpCode });
      if (response.success && response.data) {
        setUser({
          id: response.data.userId,
          username: response.data.username,
          email: response.data.email,
          role: response.data.role as 'ADMIN' | 'USER' | 'GUEST',
          otpEnabled: response.data.otpEnabled,
          storageQuota: response.data.storageQuota,
          usedStorage: response.data.usedStorage,
          storageUsagePercentage: response.data.storageUsagePercentage,
          accountLocked: false,
        });
        return { success: true };
      }
      return { success: false, message: response.message || 'OTP verification failed' };
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } };
      return { success: false, message: err.response?.data?.message || 'OTP verification failed' };
    }
  };

  const signup = async (username: string, email: string, password: string, confirmPassword: string, adminSecret?: string) => {
    try {
      const response = await apiService.signup({ username, email, password, confirmPassword, adminSecret });
      if (response.success) {
        return { success: true, message: 'Account created successfully' };
      }
      return { success: false, message: response.message || 'Signup failed' };
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } };
      return { success: false, message: err.response?.data?.message || 'Signup failed' };
    }
  };

  return (
    <AuthContext.Provider value={{
      user,
      isAuthenticated: !!user,
      isLoading,
      login,
      verifyOtp,
      signup,
      logout,
      refreshUser,
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}