import { describe, expect, it, vi } from 'vitest'

// Keep tests offline: the profile lookup fails instead of calling a real server.
vi.mock('../api/authApi', () => ({
  login: vi.fn(),
  getCurrentUser: vi.fn(() => Promise.reject(new Error('offline'))),
  getCurrentUserAvatar: vi.fn(),
  updateCurrentUser: vi.fn(),
  uploadCurrentUserAvatar: vi.fn(),
  removeCurrentUserAvatar: vi.fn(),
  registerAccount: vi.fn(),
}))
import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from '../auth/AuthContext'
import { GuestOnly, RequireAuth } from '../auth/guards'
import LoginPage from './LoginPage'
import { TOKEN_KEY } from '../api/client'

function renderAt(path, token = '') {
  localStorage.clear()
  if (token) localStorage.setItem(TOKEN_KEY, token)
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<GuestOnly><LoginPage /></GuestOnly>} />
          <Route path="/dashboard" element={<RequireAuth><p>Dashboard for guests?</p></RequireAuth>} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  )
}

describe('LoginPage', () => {
  it('asks for credentials before calling the server', async () => {
    renderAt('/login')
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Enter your username or email and password.')
  })

  it('toggles password visibility', () => {
    renderAt('/login')
    const password = document.getElementById('password')
    expect(password).toHaveAttribute('type', 'password')
    fireEvent.click(screen.getByRole('button', { name: 'Show password' }))
    expect(password).toHaveAttribute('type', 'text')
  })

  it('sends signed-in users past the login page', async () => {
    renderAt('/login', 'fake-token')
    expect(await screen.findByText('Could not load your account')).toBeInTheDocument()
    expect(screen.queryByText('Sign in to your account')).not.toBeInTheDocument()
  })
})
