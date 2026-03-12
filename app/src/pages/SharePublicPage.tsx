import { useState } from 'react'
import { useParams } from 'react-router-dom'
import {
  Cloud,
  Download,
  Shield,
  FileText,
  Lock,
  AlertTriangle,
  Loader2,
  CheckCircle
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle
} from '@/components/ui/card'
import { Alert, AlertDescription } from '@/components/ui/alert'

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'

interface ShareInfo {
  fileName: string
  fileSize: string
}

type PageState = 'ready' | 'password' | 'downloading' | 'error' | 'expired'

export default function SharePublicPage() {
  const { shareToken } = useParams<{ shareToken: string }>()

  const [pageState, setPageState] = useState<PageState>('ready')
  const [shareInfo, setShareInfo] = useState<ShareInfo | null>(null)
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [downloadSuccess, setDownloadSuccess] = useState(false)

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return bytes + ' B'
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
    if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
    return (bytes / (1024 * 1024 * 1024)).toFixed(1) + ' GB'
  }

  const handleDownload = async (pwd?: string) => {
    if (!shareToken) return

    setError('')
    setPageState('downloading')

    try {
      const url = pwd
        ? `${API_BASE_URL}/share/public/${shareToken}?password=${encodeURIComponent(pwd)}`
        : `${API_BASE_URL}/share/public/${shareToken}`

      const token = localStorage.getItem('accessToken')

      const response = await fetch(url, {
        method: 'GET',
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      })

      if (response.status === 403) {
        const text = await response.text()
        if (text.toLowerCase().includes('password')) {
          setPageState('password')
          setError('This file is password protected. Please enter the password.')
        } else {
          setPageState('expired')
        }
        return
      }

      if (response.status === 404) {
        setPageState('error')
        setError('This share link does not exist or has been revoked.')
        return
      }

      if (!response.ok) {
        setPageState('error')
        setError('Failed to download the file. The link may be invalid or expired.')
        return
      }

      // Get filename from Content-Disposition header
      const disposition = response.headers.get('Content-Disposition')
      let fileName = 'download'
      if (disposition) {
        const match = disposition.match(/filename="?([^"]+)"?/)
        if (match) fileName = match[1]
      }

      // Get file size from Content-Length header
      const contentLength = response.headers.get('Content-Length')
      const fileSize = contentLength ? formatFileSize(parseInt(contentLength)) : ''

      // Trigger browser download
      const blob = await response.blob()
      const objectUrl = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = objectUrl
      a.download = fileName
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(objectUrl)
      document.body.removeChild(a)

      setShareInfo({ fileName, fileSize })
      setDownloadSuccess(true)
      setPageState('ready')

    } catch {
      setPageState('error')
      setError('Network error. Please check your connection and try again.')
    }
  }

  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!password) {
      setError('Please enter the password')
      return
    }
    await handleDownload(password)
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-background to-muted p-4">
      <div className="w-full max-w-md">

        {/* Logo */}
        <div className="flex items-center justify-center gap-3 mb-8">
          <div className="w-14 h-14 bg-primary rounded-xl flex items-center justify-center shadow-lg">
            <Cloud className="w-8 h-8 text-primary-foreground" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">CipherCloud X</h1>
            <p className="text-sm text-muted-foreground">Enterprise Secure Storage</p>
          </div>
        </div>

        <Card>

          {/* ── Ready to Download ── */}
          {pageState === 'ready' && (
            <>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <FileText className="w-5 h-5 text-primary" />
                  Shared File
                </CardTitle>
                <CardDescription>
                  Someone shared a file with you via CipherCloud X
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">

                {downloadSuccess && (
                  <Alert className="border-green-500 bg-green-50">
                    <CheckCircle className="w-4 h-4 text-green-600" />
                    <AlertDescription className="text-green-700 ml-2">
                      File downloaded successfully!
                    </AlertDescription>
                  </Alert>
                )}

                <div className="flex items-center gap-4 p-4 rounded-lg border bg-muted/40">
                  <div className="w-12 h-12 bg-primary/10 rounded-lg flex items-center justify-center">
                    <FileText className="w-6 h-6 text-primary" />
                  </div>
                  <div>
                    <p className="font-medium">
                      {shareInfo?.fileName || 'Secure File'}
                    </p>
                    <p className="text-sm text-muted-foreground">
                      {shareInfo?.fileSize || 'Click download to access this file'}
                    </p>
                  </div>
                </div>

                <Button className="w-full" onClick={() => handleDownload()}>
                  <Download className="w-4 h-4 mr-2" />
                  {downloadSuccess ? 'Download Again' : 'Download File'}
                </Button>

              </CardContent>
            </>
          )}

          {/* ── Downloading ── */}
          {pageState === 'downloading' && (
            <>
              <CardHeader>
                <CardTitle>Preparing Download...</CardTitle>
                <CardDescription>
                  Decrypting and preparing your file
                </CardDescription>
              </CardHeader>
              <CardContent className="flex flex-col items-center py-8 gap-4">
                <Loader2 className="animate-spin w-12 h-12 text-primary" />
                <p className="text-sm text-muted-foreground">
                  This may take a moment for large files
                </p>
              </CardContent>
            </>
          )}

          {/* ── Password Required ── */}
          {pageState === 'password' && (
            <>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Lock className="w-5 h-5 text-amber-500" />
                  Password Required
                </CardTitle>
                <CardDescription>
                  This file is protected. Enter the password to download.
                </CardDescription>
              </CardHeader>
              <CardContent>
                <form onSubmit={handlePasswordSubmit} className="space-y-4">
                  {error && (
                    <Alert variant="destructive">
                      <AlertDescription>{error}</AlertDescription>
                    </Alert>
                  )}
                  <div className="space-y-2">
                    <Label htmlFor="password">Password</Label>
                    <Input
                      id="password"
                      type="password"
                      placeholder="Enter file password"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      required
                    />
                  </div>
                  <Button type="submit" className="w-full">
                    <Download className="w-4 h-4 mr-2" />
                    Download File
                  </Button>
                </form>
              </CardContent>
            </>
          )}

          {/* ── Expired ── */}
          {pageState === 'expired' && (
            <>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <AlertTriangle className="w-5 h-5 text-amber-500" />
                  Link Expired
                </CardTitle>
                <CardDescription>
                  This share link has expired or reached its download limit.
                </CardDescription>
              </CardHeader>
              <CardContent>
                <Alert>
                  <AlertDescription>
                    Please ask the file owner to create a new share link.
                  </AlertDescription>
                </Alert>
              </CardContent>
            </>
          )}

          {/* ── Error ── */}
          {pageState === 'error' && (
            <>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <AlertTriangle className="w-5 h-5 text-red-500" />
                  Link Not Found
                </CardTitle>
                <CardDescription>
                  This share link is invalid or has been revoked.
                </CardDescription>
              </CardHeader>
              <CardContent>
                <Alert variant="destructive">
                  <AlertDescription>{error}</AlertDescription>
                </Alert>
              </CardContent>
            </>
          )}

        </Card>

        <div className="flex items-center justify-center gap-2 text-xs text-muted-foreground mt-4">
          <Shield className="w-4 h-4" />
          <span>End-to-end encrypted with AES-256 & RSA-2048</span>
        </div>

      </div>
    </div>
  )
}