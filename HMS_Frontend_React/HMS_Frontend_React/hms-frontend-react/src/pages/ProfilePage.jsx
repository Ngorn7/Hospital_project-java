import { useEffect, useRef, useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import { errorMessage } from '../api/client'
import { Spinner, toast, confirmDialog, showError } from '../components/ui'

const MAX_BYTES = 2 * 1024 * 1024
const ALLOWED_TYPES = ['image/jpeg', 'image/png']

export default function ProfilePage() {
  const { profile, avatarUrl, saveProfile, uploadAvatar, removeAvatar, refreshProfile } = useAuth()
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [avatarBusy, setAvatarBusy] = useState(false)
  const fileInput = useRef(null)

  useEffect(() => {
    if (profile) {
      setUsername(profile.username)
      setEmail(profile.email)
    }
  }, [profile])

  if (!profile) return <Spinner label="Loading profile…" />

  const dirty = username.trim() !== profile.username || email.trim() !== profile.email

  const submit = async (event) => {
    event.preventDefault()
    setError('')
    setSaving(true)
    try {
      await saveProfile({ username: username.trim(), email: email.trim() })
      toast('Profile updated')
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  const onFileChosen = async (event) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    if (!ALLOWED_TYPES.includes(file.type)) return showError('Unsupported file', 'Use a JPEG or PNG image.')
    if (file.size > MAX_BYTES) return showError('File too large', 'Profile images must be 2 MB or smaller.')
    setAvatarBusy(true)
    try {
      await uploadAvatar(file)
      toast('Profile photo updated')
    } catch (err) {
      showError('Could not upload photo', errorMessage(err))
    } finally {
      setAvatarBusy(false)
    }
  }

  const onRemoveAvatar = async () => {
    const confirmed = await confirmDialog({ title: 'Remove profile photo?', text: 'A default icon will be shown instead.', confirmText: 'Remove', danger: true })
    if (!confirmed) return
    setAvatarBusy(true)
    try {
      await removeAvatar()
      toast('Profile photo removed')
    } catch (err) {
      showError('Could not remove photo', errorMessage(err))
    } finally {
      setAvatarBusy(false)
    }
  }

  return (
    <>
      <div className="page-heading profile-page-heading">
        <div>
          <span className="eyebrow">CAREPOINT WORKSPACE</span>
          <h2>My Profile</h2>
          <p className="muted-copy">Manage your account details and profile photo.</p>
        </div>
        <span className="profile-secure-tag"><i className="bi bi-shield-check" /> Signed in securely</span>
      </div>

      <div className="profile-layout">
        <section className="panel profile-card">
          <div className="profile-cover">
            <span className="cover-mark"><i className="bi bi-person-badge" /></span>
            <span className="cover-caption">ACCOUNT <span>●</span> {profile.role}</span>
          </div>
          <div className="profile-identity">
            <div className="profile-photo-wrap">
              <div className="profile-photo">
                {avatarUrl ? <img src={avatarUrl} alt={`${profile.username} profile`} /> : <i className="bi bi-person-fill" />}
              </div>
            </div>
            <div className="profile-name-block">
              <h3>{profile.username}</h3>
              <span className="role-badge">{profile.role}</span>
            </div>
          </div>
          <div className="avatar-tools">
            <p>JPEG or PNG, up to 2 MB.</p>
            <div className="avatar-actions">
              <button className="btn btn-sm btn-outline-primary" disabled={avatarBusy} onClick={() => fileInput.current?.click()}>
                {avatarBusy ? 'Working…' : profile.hasAvatar ? 'Change photo' : 'Upload photo'}
              </button>
              {profile.hasAvatar && (
                <button className="btn btn-sm btn-light border" disabled={avatarBusy} onClick={onRemoveAvatar}>Remove</button>
              )}
              <input ref={fileInput} type="file" accept="image/jpeg,image/png" hidden onChange={onFileChosen} />
            </div>
          </div>
          <div className="profile-access">
            <span className="access-icon"><i className="bi bi-shield-lock" /></span>
            <div>
              <strong>Access is enforced by the server</strong>
              <small>Your role decides which records you can change.</small>
            </div>
            <i className="bi bi-check-circle-fill access-check" />
          </div>
        </section>

        <section className="panel profile-edit-panel">
          <div className="profile-form-heading">
            <div>
              <span className="eyebrow">ACCOUNT DETAILS</span>
              <h3>Edit your details</h3>
            </div>
            <span className="profile-id">ID #{profile.id}</span>
          </div>

          {error && <div className="alert alert-danger" role="alert">{error}</div>}

          <form className="profile-form" onSubmit={submit} noValidate>
            <div>
              <label className="form-label" htmlFor="profile-username">Username</label>
              <input id="profile-username" className="form-control" maxLength={80} value={username} onChange={(e) => setUsername(e.target.value)} />
              <span className="profile-field-hint">Changing your username refreshes your sign-in automatically.</span>
            </div>
            <div>
              <label className="form-label" htmlFor="profile-email">Email</label>
              <input id="profile-email" className="form-control" type="email" maxLength={255} value={email} onChange={(e) => setEmail(e.target.value)} />
            </div>
            <div className="profile-readonly">
              <span className="readonly-icon"><i className="bi bi-lock" /></span>
              <div>
                <small>Role</small>
                <strong>{profile.role}</strong>
              </div>
              <span className="readonly-note">Set by an administrator</span>
            </div>
            <div className="profile-form-footer">
              <span><i className="bi bi-info-circle" /> Changes apply to your next request.</span>
              <button className="btn btn-primary" type="submit" disabled={saving || !dirty}>
                {saving ? 'Saving…' : 'Save changes'}
              </button>
            </div>
          </form>
          <button className="btn btn-link p-0 mt-3 small" onClick={refreshProfile}>Reload from server</button>
        </section>
      </div>
    </>
  )
}
