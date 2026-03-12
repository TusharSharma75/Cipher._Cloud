export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;
  timestamp?: string;
  errorCode?: string;
  errors?: Record<string, string>;
}

export interface User {
  id: number;
  username: string;
  email: string;
  role: 'ADMIN' | 'USER' | 'GUEST';
  otpEnabled: boolean;
  storageQuota: number;
  usedStorage: number;
  storageUsagePercentage: number;
  accountLocked: boolean;
  lastLogin?: string;
  createdAt?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  requiresOtp: boolean;
  otpSessionId?: string;
  userId: number;
  username: string;
  email: string;
  role: string;
  otpEnabled: boolean;
  storageQuota: number;
  usedStorage: number;
  storageUsagePercentage: number;
}

export interface LoginRequest {
  usernameOrEmail: string;
  password: string;
  otpCode?: string;
}

export interface SignupRequest {
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
}

export interface OtpVerifyRequest {
  otpSessionId: string;
  otpCode: string;
  useBackupCode?: boolean;
}

export interface FileMetadata {
  id: number;
  originalFilename: string;
  storedFilename: string;
  contentType: string;
  fileSize: number;
  formattedFileSize: string;
  versionNumber: number;
  folderPath: string;
  sha256Hash: string;
  integrityStatus: 'VERIFIED' | 'PENDING' | 'FAILED' | 'CORRUPTED';
  encryptionVersion: string;
  primaryLocation: string;
  backupLocation?: string;
  replicationStatus: string;
  isDeleted: boolean;
  uploadTimestamp: string;
  ownerId: number;
  ownerUsername: string;
  hasVersions: boolean;
  versions?: FileVersion[];
  shared: boolean;
}

export interface FileVersion {
  id: number;
  versionNumber: number;
  fileSize: number;
  formattedFileSize: string;
  sha256Hash: string;
  integrityStatus: string;
  uploadTimestamp: string;
  isLatest: boolean;
}

export interface FileShare {
  id: number;
  fileId: number;
  fileName: string;
  sharedByUsername: string;
  shareToken: string;
  permission: 'READ' | 'WRITE' | 'ADMIN';
  isPublic: boolean;
  passwordProtected: boolean;
  expiryDate?: string;
  maxDownloads?: number;
  downloadCount: number;
  isActive: boolean;
  createdAt: string;
  shareUrl: string;
}

export interface CreateShareRequest {
  fileId: number;
  sharedWithUserId?: number;
  permission?: 'READ' | 'WRITE' | 'ADMIN';
  isPublic?: boolean;
  password?: string;
  expiryDate?: string;
  maxDownloads?: number;
}

export interface ActivityLog {
  id: number;
  userId: number;
  username: string;
  action: string;
  actionDisplayName: string;
  fileId?: number;
  fileName?: string;
  timestamp: string;
  ipAddress?: string;
  deviceInfo?: string;
  status: string;
  errorMessage?: string;
}

export interface AnalyticsDashboard {
  totalUsers: number;
  activeUsers: number;
  newUsersToday: number;
  lockedAccounts: number;
  otpEnabledUsers: number;
  totalStorageUsed: number;
  formattedTotalStorageUsed: string;
  totalStorageQuota: number;
  formattedTotalStorageQuota: string;
  overallStorageUsagePercentage: number;
  totalFiles: number;
  uploadsToday: number;
  downloadsToday: number;
  loginsToday: number;
  failedLoginsToday: number;
  fileTypeStats: FileTypeStat[];
  uploadTrends: DailyActivity[];
  downloadTrends: DailyActivity[];
  topUsersByStorage: TopUser[];
  recentActivity: ActivityLog[];
  integrityFailures: number;
  quotaViolations: number;
}

export interface FileTypeStat {
  fileType: string;
  count: number;
  totalSize: number;
  formattedTotalSize: string;
  percentage: number;
}

export interface DailyActivity {
  date: string;
  count: number;
  totalSize?: number;
  formattedTotalSize?: string;
}

export interface TopUser {
  userId: number;
  username: string;
  email: string;
  value: number;
  formattedValue: string;
}