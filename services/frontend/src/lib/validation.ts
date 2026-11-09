// Field rules shared by the app's forms. Each rule returns a French message, or null when the value is valid.

import { useState } from "react";

export type FieldErrors<K extends string> = Partial<Record<K, string | null>>;

/** Highest amount accepted in a form: 1 000 milliards FCFA, far above any real project. */
export const MAX_AMOUNT = 1_000_000_000_000;
/** Length of the text columns in the database (VARCHAR(255)). */
export const MAX_TEXT = 255;

export function hasErrors(errors: FieldErrors<string>): boolean {
  return Object.values(errors).some(Boolean);
}

/** Today as YYYY-MM-DD in the user's time zone (toISOString() would give the UTC date). */
export function todayIso(): string {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${now.getFullYear()}-${month}-${day}`;
}

export function selectError(value: string, message = "Faites un choix."): string | null {
  return value ? null : message;
}

export function textError(
  value: string,
  { required = true, max = MAX_TEXT }: { required?: boolean; max?: number } = {},
): string | null {
  const text = value.trim();
  if (!text) {
    return required ? "Ce champ est obligatoire." : null;
  }
  if (text.length > max) {
    return `Au plus ${max} caractères.`;
  }
  return null;
}

/**
 * A number typed in an <input type="number">. The browser gives "" for text it cannot read,
 * so an empty value also covers garbage input.
 */
export function numberError(
  value: string,
  {
    required = true,
    allowZero = false,
    integer = false,
    max = MAX_AMOUNT,
    empty = "Saisissez une valeur.",
  }: { required?: boolean; allowZero?: boolean; integer?: boolean; max?: number; empty?: string } = {},
): string | null {
  if (value.trim() === "") {
    return required ? empty : null;
  }
  const number = Number(value);
  if (!Number.isFinite(number)) {
    return "Valeur invalide.";
  }
  if (number < 0) {
    return "La valeur ne peut pas être négative.";
  }
  if (number === 0 && !allowZero) {
    return "La valeur doit être supérieure à 0.";
  }
  if (integer && !Number.isInteger(number)) {
    return "Saisissez un nombre entier.";
  }
  if (number > max) {
    return `La valeur ne peut pas dépasser ${max.toLocaleString("fr-FR")}.`;
  }
  return null;
}

/** An amount in FCFA: a whole number (the franc CFA has no cents in use), positive unless allowZero. */
export function amountError(
  value: string,
  options: { required?: boolean; allowZero?: boolean; max?: number } = {},
): string | null {
  const error = numberError(value, { ...options, integer: true, empty: "Saisissez un montant." });
  if (error === "Saisissez un nombre entier.") {
    return "Le montant doit être un nombre entier de FCFA.";
  }
  return error;
}

/** A date from an <input type="date"> (YYYY-MM-DD). */
export function dateError(
  value: string,
  { required = true, notInFuture = false }: { required?: boolean; notInFuture?: boolean } = {},
): string | null {
  if (!value) {
    return required ? "Saisissez une date." : null;
  }
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  const year = match ? Number(match[1]) : NaN;
  if (!match || Number.isNaN(Date.parse(value)) || year < 1900 || year > 2100) {
    return "Date invalide.";
  }
  if (notInFuture && value > todayIso()) {
    return "La date ne peut pas être dans le futur.";
  }
  return null;
}

/**
 * Shows a form's errors only once the user has tried to submit it, then keeps them in sync with
 * what they type. `check()` returns true when the form can be sent.
 */
export function useFormErrors<K extends string>(errors: FieldErrors<K>) {
  const [shown, setShown] = useState(false);
  return {
    errors: (shown ? errors : {}) as FieldErrors<K>,
    check: () => {
      setShown(true);
      return !hasErrors(errors);
    },
    reset: () => setShown(false),
  };
}
