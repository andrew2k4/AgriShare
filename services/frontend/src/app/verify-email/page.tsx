"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { AlertCircle, CheckCircle2, Loader2 } from "lucide-react";
import { apiClient } from "@/api/client";
import { AuthCard } from "@/components/auth/AuthCard";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { tokenFromHash } from "@/auth/rules";

type State = "verifying" | "done" | "invalid";

export default function VerifyEmailPage() {
  const [state, setState] = useState<State>("verifying");
  // The link works once: never send it twice (React runs effects twice in development)
  const sent = useRef(false);

  useEffect(() => {
    if (sent.current) {
      return;
    }
    sent.current = true;
    const token = tokenFromHash();
    if (!token) {
      setState("invalid");
      return;
    }
    apiClient.POST("/api/auth/verify-email", { body: { token } })
      .then(({ response }) => setState(response.ok ? "done" : "invalid"))
      .catch(() => setState("invalid"));
  }, []);

  return (
    <AuthCard title="Confirmation de l'adresse e-mail">
      {state === "verifying" && (
        <div className="flex justify-center py-6"><Loader2 className="h-8 w-8 animate-spin text-primary" /></div>
      )}
      {state === "done" && (
        <div className="space-y-4">
          <Alert>
            <CheckCircle2 className="h-4 w-4" />
            <AlertTitle>Adresse confirmée</AlertTitle>
            <AlertDescription>Votre compte est activé, vous pouvez vous connecter.</AlertDescription>
          </Alert>
          <Button asChild className="w-full bg-primary"><Link href="/login">Se connecter</Link></Button>
        </div>
      )}
      {state === "invalid" && <InvalidLink />}
    </AuthCard>
  );
}

function InvalidLink() {
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState(false);

  const resend = async (e: React.FormEvent) => {
    e.preventDefault();
    await apiClient.POST("/api/auth/resend-verification", { body: { email: email.trim() } });
    setSent(true);
  };

  return (
    <div className="space-y-4">
      <Alert variant="destructive">
        <AlertCircle className="h-4 w-4" />
        <AlertTitle>Lien invalide ou expiré</AlertTitle>
        <AlertDescription>
          Ce lien a déjà servi ou n'est plus valable. Si votre compte est déjà confirmé, connectez-vous ;
          sinon, demandez un nouveau lien.
        </AlertDescription>
      </Alert>
      {sent ? (
        <p className="text-sm text-muted-foreground">
          Si un compte en attente correspond à cette adresse, un nouveau lien vient d'y être envoyé.
        </p>
      ) : (
        <form onSubmit={resend} className="space-y-2">
          <Label htmlFor="email">Votre adresse e-mail</Label>
          <Input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          <Button type="submit" variant="outline" className="w-full">Recevoir un nouveau lien</Button>
        </form>
      )}
      <Button asChild variant="link" className="w-full"><Link href="/login">Retour à la connexion</Link></Button>
    </div>
  );
}
