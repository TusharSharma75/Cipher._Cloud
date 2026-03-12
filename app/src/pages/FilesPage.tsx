import { useEffect, useState } from 'react';
import apiService from '@/services/api';
import type { FileMetadata, FileShare } from '@/types';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { 
  Card, 
  CardContent, 
  CardDescription, 
  CardHeader, 
  CardTitle 
} from '@/components/ui/card';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Progress } from '@/components/ui/progress';
import { Badge } from '@/components/ui/badge';
import { 
  Upload, 
  Download, 
  Trash2, 
  MoreVertical, 
  FileText, 
  Shield,
  AlertTriangle,
  Share2,
  FolderOpen,
  X,
  Copy,
  Cloud,
  Database
} from 'lucide-react';
import { toast } from 'sonner';

// ── Cloud provider display config ──────────────────────────────────────
const CLOUD_PROVIDERS: Record<string, { label: string; color: string; bg: string; icon: string }> = {
  BACKBLAZE_B2: {
    label: 'Backblaze B2',
    color: 'text-orange-700',
    bg: 'bg-orange-100 border-orange-300',
    icon: '🔥',
  },
  SUPABASE: {
    label: 'Supabase',
    color: 'text-emerald-700',
    bg: 'bg-emerald-100 border-emerald-300',
    icon: '⚡',
  },
  LOCAL: {
    label: 'Local',
    color: 'text-slate-700',
    bg: 'bg-slate-100 border-slate-300',
    icon: '💾',
  },
};

function CloudBadge({ providerType }: { providerType: string }) {
  const provider = CLOUD_PROVIDERS[providerType] ?? {
    label: providerType,
    color: 'text-blue-700',
    bg: 'bg-blue-100 border-blue-300',
    icon: '☁️',
  };
  return (
    <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full border text-xs font-medium ${provider.bg} ${provider.color}`}>
      <span>{provider.icon}</span>
      {provider.label}
    </span>
  );
}

export default function FilesPage() {
  const [files, setFiles] = useState<FileMetadata[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [uploadProgress, setUploadProgress] = useState<number | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  // Cloud selection — default Backblaze B2
  const [selectedCloud, setSelectedCloud] = useState<'BACKBLAZE_B2' | 'SUPABASE'>('BACKBLAZE_B2');

  const [shareDialogOpen, setShareDialogOpen] = useState(false);
  const [selectedFileForShare, setSelectedFileForShare] = useState<FileMetadata | null>(null);
  const [shares, setShares] = useState<FileShare[]>([]);
  const [isPublic, setIsPublic] = useState(false);

  useEffect(() => {
    fetchFiles();
  }, []);

  const fetchFiles = async () => {
    try {
      setIsLoading(true);
      const response = await apiService.getUserFiles('/', 0, 50);
      if (response.success && response.data) {
        setFiles(response.data.content);
      }
    } catch (error) {
      console.error('Failed to fetch files:', error);
      toast.error('Failed to load files');
    } finally {
      setIsLoading(false);
    }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setSelectedFile(e.target.files[0]);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) return;

    try {
      setUploadProgress(0);
      const response = await apiService.uploadFile(
        selectedFile,
        '/',
        undefined,
        (progress) => setUploadProgress(progress),
        selectedCloud          // ← pass chosen cloud to API
      );

      if (response.success) {
        toast.success(`File uploaded to ${CLOUD_PROVIDERS[selectedCloud].label}!`);
        setSelectedFile(null);
        fetchFiles();
      } else {
        toast.error(response.message || 'Upload failed');
      }
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } };
      toast.error(err.response?.data?.message || 'Upload failed');
    } finally {
      setUploadProgress(null);
    }
  };

  const handleDownload = async (file: FileMetadata) => {
    try {
      const blob = await apiService.downloadFile(file.id);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = file.originalFilename;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      toast.success('File downloaded');
    } catch (error) {
      console.error('Download failed:', error);
      toast.error('Download failed');
    }
  };

  const handleDelete = async (file: FileMetadata) => {
    if (!confirm(`Are you sure you want to delete "${file.originalFilename}"?`)) return;

    try {
      const response = await apiService.deleteFile(file.id);
      if (response.success) {
        toast.success('File deleted');
        fetchFiles();
      } else {
        toast.error(response.message || 'Delete failed');
      }
    } catch (error) {
      console.error('Delete failed:', error);
      toast.error('Delete failed');
    }
  };

  const openShareDialog = async (file: FileMetadata) => {
    setSelectedFileForShare(file);
    setShareDialogOpen(true);
    try {
      const response = await apiService.getFileShares(file.id);
      if (response.success && response.data) {
        setShares(response.data);
      }
    } catch (error) {
      console.error('Failed to fetch shares:', error);
    }
  };

  const createShare = async () => {
    if (!selectedFileForShare) return;
    try {
      const response = await apiService.createShare({
        fileId: selectedFileForShare.id,
        isPublic,
        permission: 'READ',
      });
      if (response.success && response.data) {
        toast.success('Share link created');
        setShares([...shares, response.data]);
      } else {
        toast.error(response.message || 'Failed to create share');
      }
    } catch (error) {
      console.error('Failed to create share:', error);
      toast.error('Failed to create share');
    }
  };

  const revokeShare = async (shareId: number) => {
    try {
      const response = await apiService.revokeShare(shareId);
      if (response.success) {
        toast.success('Share revoked');
        setShares(shares.filter(s => s.id !== shareId));
      }
    } catch (error) {
      console.error('Failed to revoke share:', error);
      toast.error('Failed to revoke share');
    }
  };

  const copyShareLink = (shareToken: string) => {
    const link = `${window.location.origin}/share/public/${shareToken}`;
    navigator.clipboard.writeText(link);
    toast.success('Link copied to clipboard');
  };

  const formatDate = (dateString: string) => {
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
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">My Files</h1>
          <p className="text-muted-foreground">Manage your secure files</p>
        </div>
      </div>

      {/* ── Upload Section ── */}
      <Card>
        <CardHeader>
          <CardTitle>Upload File</CardTitle>
          <CardDescription>Choose your cloud and upload with end-to-end encryption</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">

          {/* Cloud Selector */}
          <div className="space-y-2">
            <p className="text-sm font-medium">Select Cloud Storage</p>
            <div className="flex gap-3">

              {/* Backblaze B2 */}
              <button
                type="button"
                onClick={() => setSelectedCloud('BACKBLAZE_B2')}
                className={`flex-1 flex items-center gap-3 p-3 rounded-lg border-2 transition-all ${
                  selectedCloud === 'BACKBLAZE_B2'
                    ? 'border-orange-400 bg-orange-50'
                    : 'border-border bg-background hover:bg-muted'
                }`}
              >
                <div className={`w-10 h-10 rounded-lg flex items-center justify-center text-xl ${
                  selectedCloud === 'BACKBLAZE_B2' ? 'bg-orange-100' : 'bg-muted'
                }`}>
                  🔥
                </div>
                <div className="text-left">
                  <p className={`text-sm font-semibold ${selectedCloud === 'BACKBLAZE_B2' ? 'text-orange-700' : ''}`}>
                    Backblaze B2
                  </p>
                  <p className="text-xs text-muted-foreground">Default · US West</p>
                </div>
                {selectedCloud === 'BACKBLAZE_B2' && (
                  <div className="ml-auto w-5 h-5 rounded-full bg-orange-400 flex items-center justify-center">
                    <span className="text-white text-xs">✓</span>
                  </div>
                )}
              </button>

              {/* Supabase */}
              <button
                type="button"
                onClick={() => setSelectedCloud('SUPABASE')}
                className={`flex-1 flex items-center gap-3 p-3 rounded-lg border-2 transition-all ${
                  selectedCloud === 'SUPABASE'
                    ? 'border-emerald-400 bg-emerald-50'
                    : 'border-border bg-background hover:bg-muted'
                }`}
              >
                <div className={`w-10 h-10 rounded-lg flex items-center justify-center text-xl ${
                  selectedCloud === 'SUPABASE' ? 'bg-emerald-100' : 'bg-muted'
                }`}>
                  ⚡
                </div>
                <div className="text-left">
                  <p className={`text-sm font-semibold ${selectedCloud === 'SUPABASE' ? 'text-emerald-700' : ''}`}>
                    Supabase
                  </p>
                  <p className="text-xs text-muted-foreground">Asia Pacific</p>
                </div>
                {selectedCloud === 'SUPABASE' && (
                  <div className="ml-auto w-5 h-5 rounded-full bg-emerald-400 flex items-center justify-center">
                    <span className="text-white text-xs">✓</span>
                  </div>
                )}
              </button>

            </div>
          </div>

          {/* File input + Upload button */}
          <div className="flex items-center gap-4">
            <Input
              type="file"
              onChange={handleFileSelect}
              className="flex-1"
            />
            <Button
              onClick={handleUpload}
              disabled={!selectedFile || uploadProgress !== null}
            >
              {uploadProgress !== null ? (
                <>
                  <Upload className="w-4 h-4 mr-2 animate-pulse" />
                  {uploadProgress}%
                </>
              ) : (
                <>
                  <Upload className="w-4 h-4 mr-2" />
                  Upload to {selectedCloud === 'BACKBLAZE_B2' ? 'B2' : 'Supabase'}
                </>
              )}
            </Button>
          </div>

          {uploadProgress !== null && (
            <Progress value={uploadProgress} className="mt-2" />
          )}

        </CardContent>
      </Card>

      {/* ── Files List ── */}
      <Card>
        <CardHeader>
          <CardTitle>Your Files</CardTitle>
          <CardDescription>{files.length} files in storage</CardDescription>
        </CardHeader>
        <CardContent>
          {files.length === 0 ? (
            <div className="text-center py-12 text-muted-foreground">
              <FolderOpen className="w-16 h-16 mx-auto mb-4 opacity-50" />
              <p className="text-lg font-medium">No files yet</p>
              <p>Upload your first file to get started</p>
            </div>
          ) : (
            <div className="space-y-2">
              {files.map((file) => (
                <div
                  key={file.id}
                  className="flex items-center justify-between p-4 rounded-lg border hover:bg-accent transition-colors"
                >
                  <div className="flex items-center gap-4">
                    <div className="w-10 h-10 bg-primary/10 rounded-lg flex items-center justify-center">
                      <FileText className="w-5 h-5 text-primary" />
                    </div>
                    <div>
                      <p className="font-medium">{file.originalFilename}</p>
                      <div className="flex items-center gap-2 flex-wrap text-sm text-muted-foreground mt-1">
                        <span>{file.formattedFileSize}</span>
                        <span>•</span>
                        <span>{formatDate(file.uploadTimestamp)}</span>
                        <span>•</span>

                        {/* ── Cloud Badge ── */}
                        {file.primaryLocation && (
                          <CloudBadge providerType={file.primaryLocation} />
                        )}

                        {/* Backup cloud badge if replicated */}
                        {file.backupLocation && file.replicationStatus === 'COMPLETED' && (
                          <>
                            <span className="text-xs text-muted-foreground">+</span>
                            <CloudBadge providerType={file.backupLocation} />
                          </>
                        )}

                        {/* Integrity status */}
                        {file.integrityStatus === 'VERIFIED' && (
                          <span className="flex items-center gap-1 text-green-600">
                            <Shield className="w-3 h-3" />
                            Verified
                          </span>
                        )}
                        {file.integrityStatus === 'FAILED' && (
                          <span className="flex items-center gap-1 text-red-600">
                            <AlertTriangle className="w-3 h-3" />
                            Failed
                          </span>
                        )}
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => handleDownload(file)}
                      title="Download"
                    >
                      <Download className="w-4 h-4" />
                    </Button>

                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => openShareDialog(file)}
                      title="Share"
                    >
                      <Share2 className="w-4 h-4" />
                    </Button>

                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon">
                          <MoreVertical className="w-4 h-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem
                          onClick={() => handleDelete(file)}
                          className="text-red-600"
                        >
                          <Trash2 className="w-4 h-4 mr-2" />
                          Delete
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* ── Share Dialog ── */}
      <Dialog open={shareDialogOpen} onOpenChange={setShareDialogOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>Share File</DialogTitle>
            <DialogDescription>
              Create shareable links for "{selectedFileForShare?.originalFilename}"
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="isPublic"
                checked={isPublic}
                onChange={(e) => setIsPublic(e.target.checked)}
                className="rounded"
              />
              <label htmlFor="isPublic">Public link (no password required)</label>
            </div>

            <Button onClick={createShare} className="w-full">
              <Share2 className="w-4 h-4 mr-2" />
              Create Share Link
            </Button>

            {shares.length > 0 && (
              <div className="space-y-2">
                <h4 className="font-medium">Active Shares</h4>
                {shares.map((share) => (
                  <div
                    key={share.id}
                    className="flex items-center justify-between p-3 rounded-lg border"
                  >
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium truncate">
                        {share.isPublic ? 'Public Link' : 'Private Link'}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {share.downloadCount} downloads
                        {share.expiryDate && ` • Expires ${formatDate(share.expiryDate)}`}
                      </p>
                    </div>
                    <div className="flex items-center gap-2">
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => copyShareLink(share.shareToken)}
                      >
                        <Copy className="w-4 h-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => revokeShare(share.id)}
                      >
                        <X className="w-4 h-4 text-red-500" />
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}