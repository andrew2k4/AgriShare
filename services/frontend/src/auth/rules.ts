// Same rules as the backend (CredentialRules), checked before sending the form.

export const PASSWORD_MIN_LENGTH = 8;
export const PASSWORD_MAX_LENGTH = 128;

const USERNAME = /^[a-z][a-z0-9._-]{2,29}$/;
const EMAIL = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

export function emailError(email: string): string | null {
  return EMAIL.test(email.trim()) ? null : "Adresse e-mail invalide.";
}

export function usernameError(username: string): string | null {
  return USERNAME.test(username.trim().toLowerCase())
    ? null
    : "3 à 30 caractères, commençant par une lettre : lettres, chiffres, « . », « _ » ou « - ».";
}

export function passwordError(password: string): string | null {
  const length = [...password].length;
  if (length < PASSWORD_MIN_LENGTH) {
    return `Au moins ${PASSWORD_MIN_LENGTH} caractères.`;
  }
  if (length > PASSWORD_MAX_LENGTH) {
    return `Au plus ${PASSWORD_MAX_LENGTH} caractères.`;
  }
  if (password.trim().length === 0) {
    return "Le mot de passe ne peut pas contenir que des espaces.";
  }
  return null;
}

/** Reads the token the API puts in the URL fragment of e-mailed links (#token=...). */
export function tokenFromHash(): string | null {
  if (typeof window === 'undefined') {
    return null;
  }
  const token = new URLSearchParams(window.location.hash.slice(1)).get('token');
  // Drop it from the address bar and the history once read
  window.history.replaceState(null, '', window.location.pathname + window.location.search);
  return token;
}
