import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '@/contexts/AuthContext'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card'
import { Alert, AlertDescription } from '@/components/ui/alert'

import { Cloud, Loader2, Shield } from 'lucide-react'
import { toast } from 'sonner'

export default function LoginPage() {

  const { login, verifyOtp } = useAuth()
  const navigate = useNavigate()

  const [usernameOrEmail, setUsernameOrEmail] = useState('')
  const [password, setPassword] = useState('')

  const [otpCode, setOtpCode] = useState('')
  const [otpSessionId, setOtpSessionId] = useState('')
  const [requiresOtp, setRequiresOtp] = useState(false)

  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState('')

  // =============================
  // LOGIN
  // =============================

  const handleSubmit = async (e: React.FormEvent) => {

    e.preventDefault()

    setError('')
    setIsLoading(true)

    try {

      if (requiresOtp) {

        const result = await verifyOtp(otpSessionId, otpCode)

        if (result.success) {
          toast.success('Login successful')
        } else {
          setError(result.message || 'Invalid OTP')
        }

      } else {

        const result = await login(usernameOrEmail, password)

        if (result.success) {

          if (result.requiresOtp) {

            setRequiresOtp(true)
            setOtpSessionId(result.otpSessionId || '')

            toast.info('Enter OTP sent to email')

          } else {

            toast.success('Login successful')

          }

        } else {

          setError(result.message || 'Invalid credentials')

        }

      }

    } catch {

      setError('Something went wrong')

    } finally {

      setIsLoading(false)

    }
  }

  return (

    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-background to-muted p-4">

      <div className="w-full max-w-md">

        {/* LOGO */}
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

          <CardHeader>

            <CardTitle>
              {requiresOtp ? 'Two-Factor Authentication' : 'Sign In'}
            </CardTitle>

            <CardDescription>
              {requiresOtp ? 'Enter the code sent to your email' : 'Enter your credentials'}
            </CardDescription>

          </CardHeader>

          <CardContent>

            <form onSubmit={handleSubmit} className="space-y-4">

              {error && (
                <Alert variant="destructive">
                  <AlertDescription>{error}</AlertDescription>
                </Alert>
              )}

              {!requiresOtp ? (

                <>
                  <div>
                    <Label>Username or Email</Label>
                    <Input
                      value={usernameOrEmail}
                      onChange={(e) => setUsernameOrEmail(e.target.value)}
                      required
                    />
                  </div>

                  <div>
                    <Label>Password</Label>
                    <Input
                      type="password"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      required
                    />
                  </div>

                  <div className="text-right">
                    <button
                      type="button"
                      className="text-sm text-primary hover:underline"
                      onClick={() => navigate('/forgot-password')}
                    >
                      Forgot Password?
                    </button>
                  </div>
                </>

              ) : (

                <div>
                  <Label>OTP Code</Label>
                  <Input
                    value={otpCode}
                    onChange={(e) => setOtpCode(e.target.value)}
                    required
                  />
                </div>

              )}

              <Button className="w-full" disabled={isLoading}>

                {isLoading ? (
                  <>
                    <Loader2 className="animate-spin mr-2" />
                    Please wait
                  </>
                ) : requiresOtp ? 'Verify OTP' : 'Sign In'}

              </Button>

            </form>

          </CardContent>

          <CardFooter className="flex flex-col gap-3">

            <p className="text-sm text-muted-foreground">

              Don't have an account?

              <Link to="/signup" className="text-primary ml-1">
                Sign up
              </Link>

            </p>

            <div className="flex items-center gap-2 text-xs text-muted-foreground">
              <Shield className="w-4 h-4" />
              AES-256 secured
            </div>

          </CardFooter>

        </Card>

      </div>

    </div>
  )
}