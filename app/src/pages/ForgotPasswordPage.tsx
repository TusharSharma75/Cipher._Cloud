import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import apiService from '@/services/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle
} from '@/components/ui/card'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Cloud, Loader2, Shield, CheckCircle } from 'lucide-react'
import { toast } from 'sonner'

type Step = 'email' | 'otp' | 'success'

export default function ForgotPasswordPage() {
  const navigate = useNavigate()

  const [step, setStep] = useState<Step>('email')
  const [email, setEmail] = useState('')
  const [otp, setOtp] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState('')

  // ─── Step 1: Send OTP ───────────────────────────────────────────
  const handleSendOtp = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')

    if (!email) {
      setError('Please enter your email')
      return
    }

    setIsLoading(true)
    try {
      const res = await apiService.sendResetOtp(email)
      if (res.success) {
        toast.success('OTP sent to your email')
        setStep('otp')
      } else {
        setError(res.message || 'Failed to send OTP')
      }
    } catch {
      setError('Server error. Please try again.')
    } finally {
      setIsLoading(false)
    }
  }

  // ─── Step 2: Verify OTP + Reset Password ────────────────────────
  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')

    if (!otp) {
      setError('Please enter the OTP')
      return
    }

    if (!newPassword || newPassword.length < 8) {
      setError('Password must be at least 8 characters')
      return
    }

    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]+$/
    if (!passwordRegex.test(newPassword)) {
      setError('Password must contain uppercase, lowercase, number, and special character')
      return
    }

    if (newPassword !== confirmPassword) {
      setError('Passwords do not match')
      return
    }

    setIsLoading(true)
    try {
      const res = await apiService.resetPasswordWithOtp(email, otp, newPassword)
      if (res.success) {
        toast.success('Password reset successfully!')
        setStep('success')
      } else {
        setError(res.message || 'Invalid or expired OTP')
      }
    } catch {
      setError('Server error. Please try again.')
    } finally {
      setIsLoading(false)
    }
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

          {/* ── Step: email ── */}
          {step === 'email' && (
            <>
              <CardHeader>
                <CardTitle>Forgot Password</CardTitle>
                <CardDescription>
                  Enter your email and we'll send you a 6-digit OTP
                </CardDescription>
              </CardHeader>
              <CardContent>
                <form onSubmit={handleSendOtp} className="space-y-4">
                  {error && (
                    <Alert variant="destructive">
                      <AlertDescription>{error}</AlertDescription>
                    </Alert>
                  )}
                  <div className="space-y-2">
                    <Label htmlFor="email">Email Address</Label>
                    <Input
                      id="email"
                      type="email"
                      placeholder="Enter your registered email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      required
                    />
                  </div>
                  <Button type="submit" className="w-full" disabled={isLoading}>
                    {isLoading ? (
                      <><Loader2 className="animate-spin mr-2 h-4 w-4" />Sending OTP...</>
                    ) : 'Send OTP'}
                  </Button>
                </form>
              </CardContent>
              <CardFooter>
                <p className="text-sm text-muted-foreground text-center w-full">
                  Remember your password?{' '}
                  <Link to="/login" className="text-primary hover:underline">Sign in</Link>
                </p>
              </CardFooter>
            </>
          )}

          {/* ── Step: otp ── */}
          {step === 'otp' && (
            <>
              <CardHeader>
                <CardTitle>Enter OTP & New Password</CardTitle>
                <CardDescription>
                  A 6-digit OTP was sent to <strong>{email}</strong>. It expires in 10 minutes.
                </CardDescription>
              </CardHeader>
              <CardContent>
                <form onSubmit={handleResetPassword} className="space-y-4">
                  {error && (
                    <Alert variant="destructive">
                      <AlertDescription>{error}</AlertDescription>
                    </Alert>
                  )}
                  <div className="space-y-2">
                    <Label htmlFor="otp">OTP Code</Label>
                    <Input
                      id="otp"
                      type="text"
                      placeholder="Enter 6-digit OTP"
                      maxLength={6}
                      value={otp}
                      onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))}
                      required
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="newPassword">New Password</Label>
                    <Input
                      id="newPassword"
                      type="password"
                      placeholder="Create new password"
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      required
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="confirmPassword">Confirm Password</Label>
                    <Input
                      id="confirmPassword"
                      type="password"
                      placeholder="Confirm new password"
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      required
                    />
                  </div>
                  <Button type="submit" className="w-full" disabled={isLoading}>
                    {isLoading ? (
                      <><Loader2 className="animate-spin mr-2 h-4 w-4" />Resetting...</>
                    ) : 'Reset Password'}
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    className="w-full"
                    onClick={() => { setStep('email'); setError('') }}
                  >
                    ← Back / Resend OTP
                  </Button>
                </form>
              </CardContent>
            </>
          )}

          {/* ── Step: success ── */}
          {step === 'success' && (
            <>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <CheckCircle className="text-green-500 w-6 h-6" />
                  Password Reset Successful
                </CardTitle>
                <CardDescription>
                  Your password has been updated. You can now sign in with your new password.
                </CardDescription>
              </CardHeader>
              <CardContent>
                <Button className="w-full" onClick={() => navigate('/login')}>
                  Go to Login
                </Button>
              </CardContent>
            </>
          )}

        </Card>

        <div className="flex items-center justify-center gap-2 text-xs text-muted-foreground mt-4">
          <Shield className="w-4 h-4" />
          <span>Secured with AES-256 & RSA-2048</span>
        </div>

      </div>
    </div>
  )
}