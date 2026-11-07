/** Error body returned by the API (GlobalExceptionHandler / ApiErrorWriter). */
export interface ApiError {
  status?: number;
  message?: string;
  code?: string;
}

const MESSAGES: Record<string, string> = {
  INVALID_CREDENTIALS: "Identifiant ou mot de passe incorrect.",
  EMAIL_NOT_VERIFIED: "Confirmez d'abord votre adresse e-mail grâce au lien que nous vous avons envoyé.",
  ACCOUNT_SUSPENDED: "Ce compte est suspendu. Contactez un administrateur.",
  TOO_MANY_ATTEMPTS: "Trop de tentatives. Patientez quelques minutes avant de réessayer.",
  USERNAME_TAKEN: "Ce nom d'utilisateur est déjà pris.",
  USERNAME_ALREADY_SET: "Ce compte a déjà un nom d'utilisateur.",
  WRONG_PASSWORD: "Le mot de passe actuel est incorrect.",
  CSRF: "Votre session a expiré. Rechargez la page et réessayez.",
  UNAUTHENTICATED: "Votre session a expiré. Reconnectez-vous.",
  GOOGLE_EMAIL_NOT_VERIFIED: "L'adresse e-mail de votre compte Google n'est pas vérifiée.",
  GOOGLE_ACCOUNT_MISMATCH: "Cette adresse e-mail est déjà liée à un autre compte Google.",
  GOOGLE_LOGIN_FAILED: "La connexion avec Google a échoué ou a été annulée.",
};

/** A French message for an API error, falling back to a generic one. */
export function errorMessage(error: unknown, fallback = "Une erreur inattendue est survenue."): string {
  const apiError = (error ?? {}) as ApiError;
  if (apiError.code && MESSAGES[apiError.code]) {
    return MESSAGES[apiError.code];
  }
  if (apiError.status === 400 && apiError.message) {
    // Input rules are checked in the forms first; this covers anything they missed
    return `Données invalides : ${apiError.message}`;
  }
  return fallback;
}

/** Messages for the ?error=CODE the API puts on /login after a failed Google sign-in. */
export function googleErrorMessage(code: string | null): string | null {
  if (!code) {
    return null;
  }
  return MESSAGES[code] ?? MESSAGES.GOOGLE_LOGIN_FAILED;
}

/** The machine-readable code of an API error, if any. */
export function errorCode(error: unknown): string | undefined {
  return (error as ApiError | undefined)?.code;
}
