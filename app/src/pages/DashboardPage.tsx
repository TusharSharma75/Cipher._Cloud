import { useEffect, useState } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import apiService from '@/services/api';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Progress } from '@/components/ui/progress';
import { Button } from '@/components/ui/button';
import {
  FolderOpen,
  Upload,
  Shield,
  FileText,
  TrendingUp,
  Lock,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import type { FileMetadata } from '@/types';
import { toast } from 'sonner';

export default function DashboardPage() {
  const { user, refreshUser } = useAuth();

  const [recentFiles, setRecentFiles] = useState<FileMetadata[]>([]);
  const [totalFiles, setTotalFiles] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [isToggling2FA, setIsToggling2FA] = useState(false);

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    try {
      setIsLoading(true);

      const filesResponse = await apiService.getUserFiles('/', 0, 5);

      if (filesResponse.success && filesResponse.data) {
        setRecentFiles(filesResponse.data.content);
        setTotalFiles(filesResponse.data.totalElements);
      }
    } catch (error) {
      console.error('Failed to fetch dashboard data:', error);
      toast.error('Failed to load dashboard data');
    } finally {
      setIsLoading(false);
    }
  };

  const handleToggle2FA = async () => {
    try {
      setIsToggling2FA(true);

      if (user?.otpEnabled) {
        const res = await apiService.disable2FA();
        if (res.success) {
          toast.success('Two-factor authentication disabled');
          await refreshUser();
        } else {
          toast.error(res.message || 'Failed to disable 2FA');
        }
      } else {
        const res = await apiService.enable2FA();
        if (res.success) {
          toast.success('Two-factor authentication enabled');
          await refreshUser();
        } else {
          toast.error(res.message || 'Failed to enable 2FA');
        }
      }
    } catch (error) {
      console.error(error);
      toast.error('Something went wrong');
    } finally {
      setIsToggling2FA(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Welcome */}
      <div>
        <h1 className="text-3xl font-bold">
          Welcome back, {user?.username}!
        </h1>
        <p className="text-muted-foreground">
          Here's what's happening with your secure storage
        </p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Total Files */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">
              Total Files
            </CardTitle>
            <FileText className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{totalFiles}</div>
            <p className="text-xs text-muted-foreground">
              Files in your storage
            </p>
          </CardContent>
        </Card>

        {/* Storage Used */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">
              Storage Used
            </CardTitle>
            <TrendingUp className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {user?.storageUsagePercentage?.toFixed(1)}%
            </div>
            <Progress
              value={user?.storageUsagePercentage || 0}
              className="mt-2 h-2"
            />
          </CardContent>
        </Card>

        {/* Security Status */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">
              Security Status
            </CardTitle>
            <Shield className="h-4 w-4 text-green-500" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-green-600">
              Protected
            </div>
            <p className="text-xs text-muted-foreground">
              AES-256 Encryption Active
            </p>
          </CardContent>
        </Card>

        {/* 2FA Status */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="text-sm font-medium">
              2FA Status
            </CardTitle>
            <Lock className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div
              className={`text-2xl font-bold ${
                user?.otpEnabled
                  ? 'text-green-600'
                  : 'text-yellow-600'
              }`}
            >
              {user?.otpEnabled ? 'Enabled' : 'Disabled'}
            </div>
            <p className="text-xs text-muted-foreground mb-3">
              {user?.otpEnabled
                ? 'Two-factor authentication is active'
                : 'Enable for extra security'}
            </p>

            <Button
              size="sm"
              variant={user?.otpEnabled ? 'destructive' : 'default'}
              onClick={handleToggle2FA}
              disabled={isToggling2FA}
            >
              {isToggling2FA
                ? 'Processing...'
                : user?.otpEnabled
                ? 'Disable 2FA'
                : 'Enable 2FA'}
            </Button>
          </CardContent>
        </Card>
      </div>

      {/* Recent Files */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle>Recent Files</CardTitle>
            <CardDescription>
              Your recently uploaded files
            </CardDescription>
          </div>
          <Button variant="outline" size="sm" asChild>
            <Link to="/files">
              <FolderOpen className="w-4 h-4 mr-2" />
              View All
            </Link>
          </Button>
        </CardHeader>
        <CardContent>
          {recentFiles.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              <FileText className="w-12 h-12 mx-auto mb-4 opacity-50" />
              <p>No files yet</p>
              <p className="text-sm">
                Upload your first file to get started
              </p>
            </div>
          ) : (
            <div className="space-y-3">
              {recentFiles.map((file) => (
                <div
                  key={file.id}
                  className="flex items-center justify-between p-3 rounded-lg border hover:bg-accent transition-colors"
                >
                  <div className="flex items-center gap-3">
                    <FileText className="w-8 h-8 text-primary" />
                    <div>
                      <p className="font-medium truncate max-w-[200px]">
                        {file.originalFilename}
                      </p>
                      <p className="text-sm text-muted-foreground">
                        {file.formattedFileSize}
                      </p>
                    </div>
                  </div>
                  {file.integrityStatus === 'VERIFIED' && (
                    <Shield className="w-4 h-4 text-green-500" />
                  )}
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Quick Actions */}
      <Card>
        <CardHeader>
          <CardTitle>Quick Actions</CardTitle>
          <CardDescription>
            Common tasks you can perform
          </CardDescription>
        </CardHeader>
        <CardContent className="flex flex-wrap gap-3">
          <Button asChild>
            <Link to="/files">
              <FolderOpen className="w-4 h-4 mr-2" />
              Browse Files
            </Link>
          </Button>

          <Button variant="outline" asChild>
            <Link to="/files">
              <Upload className="w-4 h-4 mr-2" />
              Upload File
            </Link>
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}