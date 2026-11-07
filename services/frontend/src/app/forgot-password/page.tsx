"use client";

import { useState } from "react";
import Link from "next/link";
import { AlertCircle, Loader2, MailCheck } from "lucide-react";
import { apiClient } from "@/api/client";
import { AuthCard, FieldError } from "@/components/auth/AuthCard";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { errorMessage } from "@/auth/errors";
import { emailError } from "@/auth/rules";

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [fieldError, setFieldError] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [sent, setSent] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    const invalid = emailError(email);
    setFieldError(invalid);
    if (invalid) {
      return;
    }
    setLoading(true);
    try {
      const { response, error: apiError } = await apiClient.POST("/api/auth/password/forgot", {
        body: { email: email.trim() },
      });
      if (!response.ok) {
        setError(errorMessage(apiError));
        return;
      }
      setSent(true);
    } catch {
      setError("Le serveur est injoignable. Vérifiez votre connexion et réessayez.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthCard
      title="Mot de passe oublié"
      description="Recevez un lien pour choisir un nouveau mot de passe. Cela fonctionne aussi si vous n'en avez jamais défini."
      footer={<Button asChild variant="link"><Link href="/login">Retour à la connexion</Link></Button>}
    >
      {sent ? (
        <Alert>
          <MailCheck className="h-4 w-4" />
          <AlertTitle>Vérifiez votre boîte mail</AlertTitle>
          <AlertDescription>
            Si un compte correspond à <strong>{email.trim()}</strong>, un lien valable 30 minutes vient de lui être
            envoyé. Pensez à vérifier vos spams.
          </AlertDescription>
        </Alert>
      ) : (
        <form onSubmit={submit} className="space-y-4" noValidate>
          {error && (
            <Alert variant="destructive">
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}
          <div className="space-y-1">
            <Label htmlFor="email">Adresse e-mail</Label>
            <Input id="email" type="email" autoComplete="email" value={email}
                   onChange={(e) => setEmail(e.target.value)} disabled={loading} />
            <FieldError message={fieldError} />
          </div>
          <Button type="submit" className="w-full bg-primary" disabled={loading}>
            {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            Envoyer le lien
          </Button>
        </form>
      )}
    </AuthCard>
  );
}
