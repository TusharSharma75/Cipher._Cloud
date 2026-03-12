import { useEffect, useState } from 'react';
import apiService from '@/services/api';
import type { AnalyticsDashboard, User } from '@/types';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Progress } from '@/components/ui/progress';
import { 
  Users, 
  FileText, 
  TrendingUp, 
  Shield, 
  AlertTriangle,
  CheckCircle,
  XCircle,
  Lock,
  Activity,
  BarChart3
} from 'lucide-react';
import { toast } from 'sonner';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';

export default function AdminPage() {
  const [analytics, setAnalytics] = useState<AnalyticsDashboard | null>(null);
  const [users, setUsers] = useState<User[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [quotaDialogOpen, setQuotaDialogOpen] = useState(false);
  const [newQuota, setNewQuota] = useState('');

  useEffect(() => {
    fetchDashboardData();
    fetchUsers();
  }, []);

  const fetchDashboardData = async () => {
    try {
      const response = await apiService.getDashboardAnalytics();
      if (response.success && response.data) {
        setAnalytics(response.data);
      }
    } catch (error) {
      console.error('Failed to fetch analytics:', error);
      toast.error('Failed to load analytics');
    }
  };

  const fetchUsers = async () => {
    try {
      const response = await apiService.getAllUsers(0, 100);
      if (response.success && response.data) {
        setUsers(response.data.content);
      }
    } catch (error) {
      console.error('Failed to fetch users:', error);
      toast.error('Failed to load users');
    } finally {
      setIsLoading(false);
    }
  };

  const handleUpdateQuota = async () => {
    if (!selectedUser || !newQuota) return;

    try {
      const quotaInBytes = parseInt(newQuota) * 1024 * 1024 * 1024; // Convert GB to bytes
      const response = await apiService.updateUserQuota(selectedUser.id, quotaInBytes);
      
      if (response.success) {
        toast.success('Quota updated successfully');
        setQuotaDialogOpen(false);
        fetchUsers();
      } else {
        toast.error(response.message || 'Failed to update quota');
      }
    } catch (error) {
      console.error('Failed to update quota:', error);
      toast.error('Failed to update quota');
    }
  };

  const openQuotaDialog = (user: User) => {
    setSelectedUser(user);
    setNewQuota((user.storageQuota / (1024 * 1024 * 1024)).toString());
    setQuotaDialogOpen(true);
  };

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const formatDate = (dateString?: string) => {
    if (!dateString) return 'Never';
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
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
      <div>
        <h1 className="text-3xl font-bold">Admin Dashboard</h1>
        <p className="text-muted-foreground">System overview and management</p>
      </div>

      {/* Overview Stats */}
      {analytics && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Total Users</CardTitle>
              <Users className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{analytics.totalUsers}</div>
              <p className="text-xs text-muted-foreground">
                {analytics.newUsersToday} new today
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Total Files</CardTitle>
              <FileText className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{analytics.totalFiles}</div>
              <p className="text-xs text-muted-foreground">
                {analytics.uploadsToday} uploads today
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Storage Used</CardTitle>
              <TrendingUp className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{analytics.formattedTotalStorageUsed}</div>
              <p className="text-xs text-muted-foreground">
                of {analytics.formattedTotalStorageQuota}
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Security Alerts</CardTitle>
              <Shield className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{analytics.integrityFailures + analytics.quotaViolations}</div>
              <p className="text-xs text-muted-foreground">
                {analytics.failedLoginsToday} failed logins today
              </p>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Main Content Tabs */}
      <Tabs defaultValue="overview" className="space-y-4">
        <TabsList>
          <TabsTrigger value="overview">
            <BarChart3 className="w-4 h-4 mr-2" />
            Overview
          </TabsTrigger>
          <TabsTrigger value="users">
            <Users className="w-4 h-4 mr-2" />
            Users
          </TabsTrigger>
          <TabsTrigger value="security">
            <Shield className="w-4 h-4 mr-2" />
            Security
          </TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="space-y-4">
          {analytics && (
            <>
              {/* File Type Distribution */}
              <Card>
                <CardHeader>
                  <CardTitle>File Type Distribution</CardTitle>
                  <CardDescription>Breakdown of files by type</CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    {analytics.fileTypeStats.map((stat) => (
                      <div key={stat.fileType} className="space-y-1">
                        <div className="flex justify-between text-sm">
                          <span>{stat.fileType}</span>
                          <span className="text-muted-foreground">
                            {stat.count} files ({stat.percentage}%)
                          </span>
                        </div>
                        <Progress value={stat.percentage} className="h-2" />
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>

              {/* Recent Activity */}
              <Card>
                <CardHeader>
                  <CardTitle>Recent Activity</CardTitle>
                  <CardDescription>Latest system events</CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    {analytics.recentActivity.slice(0, 10).map((activity) => (
                      <div 
                        key={activity.id}
                        className="flex items-center gap-3 p-3 rounded-lg border"
                      >
                        <Activity className="w-4 h-4 text-muted-foreground" />
                        <div className="flex-1">
                          <p className="font-medium">{activity.actionDisplayName}</p>
                          <p className="text-sm text-muted-foreground">
                            by {activity.username}
                            {activity.fileName && ` • ${activity.fileName}`}
                          </p>
                        </div>
                        <span className="text-sm text-muted-foreground">
                          {formatDate(activity.timestamp)}
                        </span>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>
            </>
          )}
        </TabsContent>

        <TabsContent value="users">
          <Card>
            <CardHeader>
              <CardTitle>User Management</CardTitle>
              <CardDescription>Manage users and their quotas</CardDescription>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>User</TableHead>
                    <TableHead>Role</TableHead>
                    <TableHead>Storage</TableHead>
                    <TableHead>2FA</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {users.map((user) => (
                    <TableRow key={user.id}>
                      <TableCell>
                        <div>
                          <p className="font-medium">{user.username}</p>
                          <p className="text-sm text-muted-foreground">{user.email}</p>
                        </div>
                      </TableCell>
                      <TableCell>
                        <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${
                          user.role === 'ADMIN' 
                            ? 'bg-purple-100 text-purple-800' 
                            : 'bg-blue-100 text-blue-800'
                        }`}>
                          {user.role}
                        </span>
                      </TableCell>
                      <TableCell>
                        <div className="space-y-1">
                          <div className="flex justify-between text-sm">
                            <span>{formatFileSize(user.usedStorage)}</span>
                            <span className="text-muted-foreground">
                              of {formatFileSize(user.storageQuota)}
                            </span>
                          </div>
                          <Progress 
                            value={user.storageUsagePercentage} 
                            className="h-2 w-32" 
                          />
                        </div>
                      </TableCell>
                      <TableCell>
                        {user.otpEnabled ? (
                          <CheckCircle className="w-5 h-5 text-green-500" />
                        ) : (
                          <XCircle className="w-5 h-5 text-gray-300" />
                        )}
                      </TableCell>
                      <TableCell>
                        {user.accountLocked ? (
                          <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-red-100 text-red-800">
                            <Lock className="w-3 h-3 mr-1" />
                            Locked
                          </span>
                        ) : (
                          <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-green-100 text-green-800">
                            <CheckCircle className="w-3 h-3 mr-1" />
                            Active
                          </span>
                        )}
                      </TableCell>
                      <TableCell>
                        <Button 
                          variant="outline" 
                          size="sm"
                          onClick={() => openQuotaDialog(user)}
                        >
                          Edit Quota
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="security">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            <Card>
              <CardHeader>
                <CardTitle>Security Overview</CardTitle>
                <CardDescription>System security metrics</CardDescription>
              </CardHeader>
              <CardContent>
                {analytics && (
                  <div className="space-y-4">
                    <div className="flex items-center justify-between p-4 rounded-lg border">
                      <div className="flex items-center gap-3">
                        <Lock className="w-5 h-5 text-green-500" />
                        <div>
                          <p className="font-medium">2FA Enabled Users</p>
                          <p className="text-sm text-muted-foreground">
                            {analytics.otpEnabledUsers} of {analytics.totalUsers} users
                          </p>
                        </div>
                      </div>
                      <span className="text-2xl font-bold">{analytics.otpEnabledUsers}</span>
                    </div>

                    <div className="flex items-center justify-between p-4 rounded-lg border">
                      <div className="flex items-center gap-3">
                        <AlertTriangle className="w-5 h-5 text-yellow-500" />
                        <div>
                          <p className="font-medium">Integrity Failures</p>
                          <p className="text-sm text-muted-foreground">
                            Files with failed integrity checks
                          </p>
                        </div>
                      </div>
                      <span className="text-2xl font-bold">{analytics.integrityFailures}</span>
                    </div>

                    <div className="flex items-center justify-between p-4 rounded-lg border">
                      <div className="flex items-center gap-3">
                        <XCircle className="w-5 h-5 text-red-500" />
                        <div>
                          <p className="font-medium">Quota Violations</p>
                          <p className="text-sm text-muted-foreground">
                            Attempted uploads over quota
                          </p>
                        </div>
                      </div>
                      <span className="text-2xl font-bold">{analytics.quotaViolations}</span>
                    </div>

                    <div className="flex items-center justify-between p-4 rounded-lg border">
                      <div className="flex items-center gap-3">
                        <Shield className="w-5 h-5 text-purple-500" />
                        <div>
                          <p className="font-medium">Locked Accounts</p>
                          <p className="text-sm text-muted-foreground">
                            Accounts temporarily locked
                          </p>
                        </div>
                      </div>
                      <span className="text-2xl font-bold">{analytics.lockedAccounts}</span>
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Upload Trends</CardTitle>
                <CardDescription>Upload activity over the last 30 days</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-2">
                  {analytics?.uploadTrends.slice(-10).map((trend, index) => (
                    <div key={index} className="flex items-center justify-between p-2 rounded hover:bg-accent">
                      <span className="text-sm">{formatDate(trend.date)}</span>
                      <div className="flex items-center gap-4">
                        <span className="text-sm font-medium">{trend.count} uploads</span>
                        {trend.formattedTotalSize && (
                          <span className="text-sm text-muted-foreground w-20 text-right">
                            {trend.formattedTotalSize}
                          </span>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          </div>
        </TabsContent>
      </Tabs>

      {/* Quota Dialog */}
      <Dialog open={quotaDialogOpen} onOpenChange={setQuotaDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Update Storage Quota</DialogTitle>
            <DialogDescription>
              Update storage quota for {selectedUser?.username}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div>
              <label className="text-sm font-medium">New Quota (GB)</label>
              <Input
                type="number"
                value={newQuota}
                onChange={(e) => setNewQuota(e.target.value)}
                min="1"
                placeholder="Enter quota in GB"
              />
            </div>
            <p className="text-sm text-muted-foreground">
              Current usage: {selectedUser && formatFileSize(selectedUser.usedStorage)}
            </p>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setQuotaDialogOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleUpdateQuota}>
              Update Quota
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
