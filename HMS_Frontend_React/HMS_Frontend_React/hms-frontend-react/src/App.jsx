import { Navigate, Route, Routes } from 'react-router-dom'
import AppLayout from './components/AppLayout'
import { GuestOnly, RequireAuth, RequireRole } from './auth/guards'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import ResourcePage from './pages/ResourcePage'
import PrescriptionsPage from './pages/PrescriptionsPage'
import ProfilePage from './pages/ProfilePage'
import UserAccountsPage from './pages/UserAccountsPage'
import { ForbiddenPage, NotFoundPage } from './pages/StatusPages'

const RESOURCE_ROUTES = ['patients', 'doctors', 'departments', 'rooms', 'appointments', 'admissions', 'medicines', 'billing']

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<GuestOnly><LoginPage /></GuestOnly>} />
      <Route element={<RequireAuth><AppLayout /></RequireAuth>}>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<DashboardPage />} />
        {RESOURCE_ROUTES.map((key) => (
          <Route key={key} path={key} element={<ResourcePage key={key} resourceKey={key} />} />
        ))}
        <Route path="prescriptions" element={<PrescriptionsPage />} />
        <Route path="profile" element={<ProfilePage />} />
        <Route
          path="user-accounts"
          element={<RequireRole roles={['ADMIN']}><UserAccountsPage /></RequireRole>}
        />
      </Route>
      <Route path="/forbidden" element={<ForbiddenPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
