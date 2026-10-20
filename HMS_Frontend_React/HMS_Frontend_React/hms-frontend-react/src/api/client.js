import axios from 'axios'

export const TOKEN_KEY = 'hms.accessToken'
export const UNAUTHORIZED_EVENT = 'hms:unauthorized'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 15000,
  headers: { Accept: 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// A 401 from any call except login means the session has ended: clear it and let AuthContext sign out.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const url = error.config?.url || ''
    if (error.response?.status === 401 && !url.includes('/auth/login')) {
      localStorage.removeItem(TOKEN_KEY)
      window.dispatchEvent(new Event(UNAUTHORIZED_EVENT))
    }
    return Promise.reject(error)
  },
)

export function errorMessage(error) {
  const data = error?.response?.data
  if (data?.message) return data.message
  const status = error?.response?.status
  if (status === 403) return 'You do not have permission to perform this action.'
  if (status === 404) return 'The requested record was not found.'
  if (status === 401) return 'Your session has ended. Please sign in again.'
  if (error?.code === 'ERR_NETWORK')
    return 'Could not reach the HMS server. Check that the backend is running.'
  return error?.message || 'The request could not be completed.'
}

export default api
