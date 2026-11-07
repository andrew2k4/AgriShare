"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { AlertCircle, CheckCircle2, Loader2 } from "lucide-react";
import { apiClient } from "@/api/client";
import { AuthCard, FieldError } from "@/components/auth/AuthCard";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { errorCode, errorMessage } from "@/auth/errors";
import { passwordError, tokenFromHash } from "@/auth/rules";

export default function ResetPasswordPage() {
  const [token, setToken] = useState<string | null | undefined>(undefined);
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [errors, setErrors] = useState<{ password?: string | null; confirm?: string | null }>({});
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [done, setDone] = useState(false);

  // Read once: tokenFromHash() clears the fragment, and React runs effects twice in development
  const read = useRef(false);
  useEffect(() => {
    if (read.current) {
      return;
    }
    read.current = true;
    setToken(tokenFromHash());
  }, []);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    const fieldErrors = {
      password: passwordError(password),
      confirm: password === confirm ? null : "Les deux mots de passe ne correspondent pas.",
    };
    setErrors(fieldErrors);
    if (fieldErrors.password || fieldErrors.confirm || !token) {
      return;
    }
    setLoading(true);
    try {
      const { response, error: apiError } = await apiClient.POST("/api/auth/password/reset", {
        body: { token, newPassword: password },
      });
      if (!response.ok) {
        setError(response.status === 400 && !errorCode(apiError)
          ? "Ce lien n'est plus valable. Demandez-en un nouveau."
          : errorMessage(apiError));
        return;
      }
      setDone(true);
    } catch {
      setError("Le serveur est injoignable. Vérifiez votre connexion et réessayez.");
    } finally {
      setLoading(false);
    }
  };

  if (token === undefined) {
    return <AuthCard title="Nouveau mot de passe"><div className="flex justify-center py-6"><Loader2 className="h-8 w-8 animate-spin text-primary" /></div></AuthCard>;
  }

  return (
    <AuthCard
      title="Nouveau mot de passe"
      footer={<Button asChild variant="link"><Link href="/login">Retour à la connexion</Link></Button>}
    >
      {done ? (
        <div className="space-y-4">
          <Alert>
            <CheckCircle2 className="h-4 w-4" />
            <AlertTitle>Mot de passe modifié</AlertTitle>
            <AlertDescription>Vos autres sessions ont été fermées. Connectez-vous avec votre nouveau mot de passe.</AlertDescription>
          </Alert>
          <Button asChild className="w-full bg-primary"><Link href="/login">Se connecter</Link></Button>
        </div>
      ) : !token ? (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertTitle>Lien incomplet</AlertTitle>
          <AlertDescription>
            Ouvrez le lien reçu par e-mail tel quel, ou <Link href="/forgot-password" className="underline">demandez-en un nouveau</Link>.
          </AlertDescription>
        </Alert>
      ) : (
        <form onSubmit={submit} className="space-y-4" noValidate>
          {error && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>
                {error} <Link href="/forgot-password" className="underline">Nouveau lien</Link>
              </AlertDescription>
            </Alert>
          )}
          <div className="space-y-1">
            <Label htmlFor="password">Nouveau mot de passe</Label>
            <Input id="password" type="password" autoComplete="new-password" value={password}
                   onChange={(e) => setPassword(e.target.value)} disabled={loading} />
            <FieldError message={errors.password} />
          </div>
          <div className="space-y-1">
            <Label htmlFor="confirm">Confirmer</Label>
            <Input id="confirm" type="password" autoComplete="new-password" value={confirm}
                   onChange={(e) => setConfirm(e.target.value)} disabled={loading} />
            <FieldError message={errors.confirm} />
          </div>
          <Button type="submit" className="w-full bg-primary" disabled={loading}>
            {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            Enregistrer
          </Button>
        </form>
      )}
    </AuthCard>
  );
}
