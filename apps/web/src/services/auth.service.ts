import { apiClient, authClient, getSessionSignal } from '@/lib/api-client'
import type {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  RegisterResponse,
  ForgotPasswordRequest,
  ResetPasswordRequest,
  SendVerificationRequest,
  UserResponse,
  GoogleAuthRequest,
} from '@/types'

export const authService = {
  login: async (data: LoginRequest): Promise<AuthResponse> => {
    const res = await authClient.post<AuthResponse>('/api/auth/login', data, { signal: getSessionSignal() })
    return res.data
  },

  google: async (data: GoogleAuthRequest): Promise<AuthResponse> => {
    const res = await authClient.post<AuthResponse>('/api/auth/google', data, { signal: getSessionSignal() })
    return res.data
  },

  register: async (data: RegisterRequest): Promise<RegisterResponse> => {
    const res = await authClient.post<RegisterResponse>('/api/auth/register', data)
    return res.data
  },

  sendVerification: async (data: SendVerificationRequest): Promise<void> => {
    await authClient.post('/api/auth/send-code', data)
  },

  verifyCode: async (data: {
    channel: 'EMAIL' | 'PHONE'
    identifier: string
    code: string
    purpose: string
  }): Promise<AuthResponse> => {
    const res = await authClient.post<AuthResponse>('/api/auth/verify-code', data, { signal: getSessionSignal() })
    return res.data
  },

  forgotPassword: async (data: ForgotPasswordRequest): Promise<void> => {
    await authClient.post('/api/auth/forgot-password', data)
  },

  resetPassword: async (data: ResetPasswordRequest): Promise<AuthResponse> => {
    const res = await authClient.post<AuthResponse>('/api/auth/reset-password', data, { signal: getSessionSignal() })
    return res.data
  },

  refreshToken: async (): Promise<AuthResponse> => {
    const res = await authClient.post<AuthResponse>('/api/auth/refresh')
    return res.data
  },

  logout: async (): Promise<void> => {
    await authClient.post('/api/auth/logout', undefined, { signal: getSessionSignal() })
  },

  getMe: async (signal?: AbortSignal): Promise<UserResponse> => {
    const res = await apiClient.get<UserResponse>('/me', { signal })
    return res.data
  },
}
