import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  getCurrentUser,
  getCurrentUserAvatar,
  login as requestLogin,
  removeCurrentUserAvatar,
  updateCurrentUser,
  uploadCurrentUserAvatar,
} from '../api/authApi'
import { errorMessage, TOKEN_KEY, UNAUTHORIZED_EVENT } from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const navigate = useNavigate()
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY) || '')
  const [profile, setProfile] = useState(null)
  const [profileError, setProfileError] = useState('')
  const [profileLoading, setProfileLoading] = useState(false)
  const [avatarUrl, setAvatarUrl] = useState('')
  const avatarUrlRef = useRef('')

  const replaceAvatar = useCallback((url) => {
    if (avatarUrlRef.current) URL.revokeObjectURL(avatarUrlRef.current)
    avatarUrlRef.current = url
    setAvatarUrl(url)
  }, [])

  const loadAvatar = useCallback(
    async (current) => {
      if (!current?.hasAvatar) return replaceAvatar('')
      try {
        replaceAvatar(URL.createObjectURL(await getCurrentUserAvatar()))
      } catch {
        replaceAvatar('')
      }
    },
    [replaceAvatar],
  )

  const clearSession = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY)
    setToken('')
    setProfile(null)
    setProfileError('')
    replaceAvatar('')
  }, [replaceAvatar])

  const refreshProfile = useCallback(async () => {
    setProfileLoading(true)
    setProfileError('')
    try {
      const current = await getCurrentUser()
      setProfile(current)
      await loadAvatar(current)
      return current
    } catch (error) {
      setProfile(null)
      setProfileError(errorMessage(error))
      return null
    } finally {
      setProfileLoading(false)
    }
  }, [loadAvatar])

  const signIn = useCallback(async (usernameOrEmail, password) => {
    const result = await requestLogin({ usernameOrEmail, password })
    if (!result?.accessToken) throw new Error('The server did not return an access token.')
    localStorage.setItem(TOKEN_KEY, result.accessToken)
    setToken(result.accessToken)
  }, [])

  const signOut = useCallback(() => {
    clearSession()
    navigate('/login', { replace: true })
  }, [clearSession, navigate])

  // Load the profile whenever we have a token (first load, or after sign-in).
  useEffect(() => {
    if (token && !profile) refreshProfile()
  }, [token, profile, refreshProfile])

  // The API client dispatches this when a request comes back 401.
  useEffect(() => {
    const onUnauthorized = () => {
      clearSession()
      navigate('/login', { replace: true })
    }
    window.addEventListener(UNAUTHORIZED_EVENT, onUnauthorized)
    return () => window.removeEventListener(UNAUTHORIZED_EVENT, onUnauthorized)
  }, [clearSession, navigate])

  useEffect(
    () => () => {
      if (avatarUrlRef.current) URL.revokeObjectURL(avatarUrlRef.current)
    },
    [],
  )

  const saveProfile = useCallback(
    async (values) => {
      const result = await updateCurrentUser(values)
      localStorage.setItem(TOKEN_KEY, result.accessToken)
      setToken(result.accessToken)
      setProfile(result.profile)
      return result.profile
    },
    [],
  )

  const uploadAvatar = useCallback(
    async (file) => {
      const updated = await uploadCurrentUserAvatar(file)
      setProfile(updated)
      await loadAvatar(updated)
      return updated
    },
    [loadAvatar],
  )

  const removeAvatar = useCallback(async () => {
    const updated = await removeCurrentUserAvatar()
    setProfile(updated)
    await loadAvatar(updated)
    return updated
  }, [loadAvatar])

  const value = useMemo(
    () => ({
      isAuthenticated: Boolean(token),
      token,
      profile,
      profileError,
      profileLoading,
      avatarUrl,
      signIn,
      signOut,
      refreshProfile,
      saveProfile,
      uploadAvatar,
      removeAvatar,
    }),
    [
      token, profile, profileError, profileLoading, avatarUrl,
      signIn, signOut, refreshProfile, saveProfile, uploadAvatar, removeAvatar,
    ],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used inside AuthProvider')
  return context
}
