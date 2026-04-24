import axios, { type AxiosInstance, type AxiosError } from 'axios'
import type {
  ApiResponse,
  AuthResponse,
  LoginRequest,
  SignupRequest,
  OtpVerifyRequest,
  User,
  FileMetadata,
  FileShare,
  CreateShareRequest,
  AnalyticsDashboard,
} from '@/types'

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'

class ApiService {

  private client: AxiosInstance
  private refreshToken: string | null = null

  constructor() {

    this.client = axios.create({
      baseURL: API_BASE_URL,
      headers: { 'Content-Type': 'application/json' }
    })

    // REQUEST INTERCEPTOR
    this.client.interceptors.request.use(
      (config) => {
        const token = localStorage.getItem('accessToken')
        if (token) config.headers.Authorization = `Bearer ${token}`
        return config
      },
      (error) => Promise.reject(error)
    )

    // RESPONSE INTERCEPTOR
    this.client.interceptors.response.use(
      (response) => response,
      async (error: AxiosError<ApiResponse<unknown>>) => {
        if (error.response?.status === 401) {
          const originalRequest = error.config
          if (originalRequest && this.refreshToken) {
            try {
              const response = await this.refreshAccessToken(this.refreshToken)
              if (response.success && response.data) {
                localStorage.setItem('accessToken', response.data.accessToken)
                originalRequest.headers.Authorization = `Bearer ${response.data.accessToken}`
                return this.client(originalRequest)
              }
            } catch {
              this.logout()
              window.location.href = '/login'
            }
          } else {
            this.logout()
            window.location.href = '/login'
          }
        }
        return Promise.reject(error)
      }
    )
  }

  // TOKEN HANDLING
  setTokens(accessToken: string, refreshToken: string) {
    localStorage.setItem('accessToken', accessToken)
    localStorage.setItem('refreshToken', refreshToken)
    this.refreshToken = refreshToken
  }

  logout() {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    this.refreshToken = null
  }

  loadTokens() {
    const accessToken = localStorage.getItem('accessToken')
    const refreshToken = localStorage.getItem('refreshToken')
    if (refreshToken) this.refreshToken = refreshToken
    return { accessToken, refreshToken }
  }

  // AUTH APIs
  async login(data: LoginRequest): Promise<ApiResponse<AuthResponse>> {
    const response = await this.client.post<ApiResponse<AuthResponse>>('/auth/login', data)
    if (response.data.success && response.data.data && !response.data.data.requiresOtp) {
      this.setTokens(response.data.data.accessToken, response.data.data.refreshToken)
    }
    return response.data
  }

  async verifyOtp(data: OtpVerifyRequest): Promise<ApiResponse<AuthResponse>> {
    const response = await this.client.post<ApiResponse<AuthResponse>>('/auth/verify-otp', data)
    if (response.data.success && response.data.data) {
      this.setTokens(response.data.data.accessToken, response.data.data.refreshToken)
    }
    return response.data
  }

  async signup(data: SignupRequest): Promise<ApiResponse<void>> {
    const response = await this.client.post<ApiResponse<void>>('/auth/signup', data)
    return response.data
  }

  async refreshAccessToken(token: string): Promise<ApiResponse<AuthResponse>> {
    const response = await this.client.post<ApiResponse<AuthResponse>>('/auth/refresh', { refreshToken: token })
    return response.data
  }

  async logoutApi(): Promise<ApiResponse<void>> {
    const response = await this.client.post<ApiResponse<void>>('/auth/logout')
    this.logout()
    return response.data
  }

  // 2FA APIs
  async enable2FA(): Promise<ApiResponse<void>> {
    const response = await this.client.post<ApiResponse<void>>('/auth/2fa/enable')
    return response.data
  }

  async disable2FA(): Promise<ApiResponse<void>> {
    const response = await this.client.post<ApiResponse<void>>('/auth/2fa/disable')
    return response.data
  }

  // FORGOT PASSWORD
  async forgotPassword(email: string) {
    return this.sendResetOtp(email)
  }

  async sendResetOtp(email: string) {
    const response = await this.client.post('/auth/forgot-password-otp', { email })
    return response.data
  }

  async resetPasswordWithOtp(email: string, otp: string, newPassword: string) {
    const response = await this.client.post('/auth/reset-password-otp', { email, otp, newPassword })
    return response.data
  }

  // USER APIs
  async getCurrentUser(): Promise<ApiResponse<User>> {
    const response = await this.client.get<ApiResponse<User>>('/user/me')
    return response.data
  }

  async updateProfile(data: Partial<User>): Promise<ApiResponse<User>> {
    const response = await this.client.put<ApiResponse<User>>('/user/me', data)
    return response.data
  }

  // ✅ FIXED: matches GET /api/admin/users → AdminController.getAllUsers()
  async getAllUsers(page = 0, size = 20) {
    const response = await this.client.get(`/admin/users?page=${page}&size=${size}`)
    return response.data
  }

  // ✅ FIXED: matches PUT /api/admin/quota → AdminController.updateQuota()
  async updateUserQuota(userId: number, quotaInBytes: number) {
    const response = await this.client.put('/admin/quota', {
      userId,
      storageQuota: quotaInBytes,
    })
    return response.data
  }

  // FILE APIs
  async uploadFile(
    file: File,
    folderPath = '/',
    description?: string,
    onProgress?: (progress: number) => void,
    storageProvider = 'BACKBLAZE_B2'
  ): Promise<ApiResponse<FileMetadata>> {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('folderPath', folderPath)
    formData.append('storageProvider', storageProvider)
    if (description) formData.append('description', description)

    const response = await this.client.post<ApiResponse<FileMetadata>>('/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          onProgress(Math.round((progressEvent.loaded * 100) / progressEvent.total))
        }
      }
    })
    return response.data
  }

  async getUserFiles(folderPath = '/', page = 0, size = 20) {
    const response = await this.client.get(
      `/files?folderPath=${encodeURIComponent(folderPath)}&page=${page}&size=${size}`
    )
    return response.data
  }

  async downloadFile(fileId: number): Promise<Blob> {
    const response = await this.client.get(`/files/${fileId}/download`, { responseType: 'blob' })
    return response.data
  }

  async deleteFile(fileId: number, permanent = false) {
    const response = await this.client.delete(`/files/${fileId}?permanent=${permanent}`)
    return response.data
  }

  // FILE SHARING APIs
  async getFileShares(fileId: number) {
    const response = await this.client.get(`/share/file/${fileId}`)
    return response.data
  }

  async createShare(data: CreateShareRequest) {
    const response = await this.client.post('/share', data)
    return response.data
  }

  async revokeShare(shareId: number) {
    const response = await this.client.delete(`/share/${shareId}`)
    return response.data
  }

  // ACTIVITY
  async getMyActivityLogs(page = 0, size = 20) {
    const response = await this.client.get(`/activity-logs/my?page=${page}&size=${size}`)
    return response.data
  }

  // ADMIN ANALYTICS
  async getDashboardAnalytics(): Promise<ApiResponse<AnalyticsDashboard>> {
    const response = await this.client.get<ApiResponse<AnalyticsDashboard>>(
      '/admin/analytics/dashboard'
    )
    return response.data
  }

  // HEALTH
  async healthCheck() {
    const response = await this.client.get('/health')
    return response.data
  }
}

export const apiService = new ApiService()
export default apiService