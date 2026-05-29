import type React from 'react'

type SettingsPageProps = {
  username: string
  currentPassword: string
  newPassword: string
  confirmPassword: string
  isSubmitting: boolean
  errorMessage: string | null
  successMessage: string | null
  onBack: () => void
  onCurrentPasswordChange: (value: string) => void
  onNewPasswordChange: (value: string) => void
  onConfirmPasswordChange: (value: string) => void
  onSubmit: (event: React.FormEvent<HTMLFormElement>) => void
}

export default function SettingsPage({
  username,
  currentPassword,
  newPassword,
  confirmPassword,
  isSubmitting,
  errorMessage,
  successMessage,
  onBack,
  onCurrentPasswordChange,
  onNewPasswordChange,
  onConfirmPasswordChange,
  onSubmit,
}: SettingsPageProps) {
  const passwordsMatch = newPassword === confirmPassword

  return (
    <main className="content-page">
      <div className="page-toolbar">
        <div>
          <h2>Inställningar</h2>
          <p>
            Konto: <strong>{username}</strong>
          </p>
        </div>
        <button className="secondary-button small-button" onClick={onBack} type="button">
          Tillbaka
        </button>
      </div>

      <section className="settings-card">
        {successMessage && (
          <div className="success-card settings-success-banner">
            {successMessage}
          </div>
        )}

        <form className="settings-form" onSubmit={onSubmit}>
          <label>
            Nuvarande lösenord
            <input
              type="password"
              value={currentPassword}
              onChange={(event) => onCurrentPasswordChange(event.target.value)}
              autoComplete="current-password"
              required
            />
          </label>

          <label>
            Nytt lösenord
            <input
              type="password"
              value={newPassword}
              onChange={(event) => onNewPasswordChange(event.target.value)}
              autoComplete="new-password"
              required
            />
          </label>

          <label>
            Bekräfta nytt lösenord
            <input
              type="password"
              value={confirmPassword}
              onChange={(event) => onConfirmPasswordChange(event.target.value)}
              autoComplete="new-password"
              required
            />
          </label>

          {errorMessage && <p className="error-message">{errorMessage}</p>}
          {!passwordsMatch && confirmPassword !== '' && (
            <p className="error-message">Lösenorden matchar inte.</p>
          )}

          <div className="settings-actions">
            <button
              className="primary-button"
              type="submit"
              disabled={
                isSubmitting ||
                currentPassword.trim() === '' ||
                newPassword.trim() === '' ||
                confirmPassword.trim() === '' ||
                !passwordsMatch
              }
            >
              {isSubmitting ? 'Byter lösenord…' : 'Byt lösenord'}
            </button>
          </div>
        </form>
      </section>

      <section className="note-card">
        <p>
          Här kan du byta lösenord. Andra inställningar som e-postadress kommer
          att läggas till i framtiden.
        </p>
      </section>
    </main>
  )
}
