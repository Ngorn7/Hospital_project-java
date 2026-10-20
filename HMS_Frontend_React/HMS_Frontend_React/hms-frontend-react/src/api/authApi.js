import api from './client'

export const login = async (credentials) => (await api.post('/auth/login', credentials)).data
export const getCurrentUser = async () => (await api.get('/auth/me')).data
export const updateCurrentUser = async (profile) => (await api.put('/auth/me', profile)).data
export const registerAccount = async (account) => (await api.post('/auth/register', account)).status

export const uploadCurrentUserAvatar = async (file) => {
  const form = new FormData()
  form.append('file', file)
  return (await api.put('/auth/me/avatar', form)).data
}
export const getCurrentUserAvatar = async () =>
  (await api.get('/auth/me/avatar', { responseType: 'blob' })).data
export const removeCurrentUserAvatar = async () => (await api.delete('/auth/me/avatar')).data
