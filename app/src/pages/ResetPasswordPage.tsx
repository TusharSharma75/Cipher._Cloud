import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'

/**
 * ResetPasswordPage is no longer used.
 * The full forgot password + OTP + reset flow is handled
 * entirely inside ForgotPasswordPage.tsx.
 *
 * This file simply redirects anyone who lands on /reset-password
 * back to /forgot-password so they go through the correct flow.
 */
export default function ResetPasswordPage() {
  const navigate = useNavigate()

  useEffect(() => {
    navigate('/forgot-password', { replace: true })
  }, [navigate])

  return null
}