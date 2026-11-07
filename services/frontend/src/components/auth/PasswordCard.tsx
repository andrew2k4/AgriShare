"use client";

import { useState } from "react";
import { KeyRound, Loader2 } from "lucide-react";
import { apiClient } from "@/api/client";
import { FieldError } from "@/components/auth/AuthCard";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { toast } from "@/hooks/use-toast";
import { useSession } from "@/auth/AuthProvider";
import { errorMessage } from "@/auth/errors";
import { passwordError } from "@/auth/rules";

export function PasswordCard() {
  const { user } = useSession();
  const [current, setCurrent] = useState("");
  const [next, setNext] = useState("");
  const [confirm, setConfirm] = useState("");
  const [errors, setErrors] = useState<{ next?: string | null; confirm?: string | null }>({});
  const [loading, setLoading] = useState(false);

  if (!user) {
    return null;
  }

  // Google-only accounts set a password through the e-mailed link, which proves the address
  const sendSetupLink = async () => {
    setLoading(true);
    try {
      const { response, error } = await apiClient.POST("/api/auth/password/forgot", { body: { email: user.email } });
      if (!response.ok) {
        toast({ variant: "destructive", title: "E-mail non envoyé", description: errorMessage(error) });
        return;
      }
      toast({ title: "E-mail envoyé", description: `Un lien pour définir votre mot de passe a été envoyé à ${user.email}.` });
    } catch {
      toast({ variant: "destructive", title: "E-mail non envoyé", description: "Le serveur est injoignable." });
    } finally {
      setLoading(false);
    }
  };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    const fieldErrors = {
      next: passwordError(next),
      confirm: next === confirm ? null : "Les deux mots de passe ne correspondent pas.",
    };
    setErrors(fieldErrors);
    if (fieldErrors.next || fieldErrors.confirm) {
      return;
    }
    setLoading(true);
    try {
      const { response, error } = await apiClient.POST("/api/auth/password/change", {
        body: { currentPassword: current, newPassword: next },
      });
      if (!response.ok) {
        toast({ variant: "destructive", title: "Mot de passe inchangé", description: errorMessage(error) });
        return;
      }
    } catch {
      toast({ variant: "destructive", title: "Mot de passe inchangé", description: "Le serveur est injoignable." });
      return;
    } finally {
      setLoading(false);
    }
    setCurrent("");
    setNext("");
    setConfirm("");
    toast({ title: "Mot de passe modifié", description: "Vos autres appareils ont été déconnectés." });
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <KeyRound className="h-5 w-5 text-primary" />
          Mot de passe
        </CardTitle>
        {!user.hasPassword && (
          <CardDescription>
            Vous vous connectez avec Google. Vous pouvez aussi définir un mot de passe pour vous connecter avec votre
            adresse e-mail ou votre nom d'utilisateur.
          </CardDescription>
        )}
      </CardHeader>
      <CardContent>
        {!user.hasPassword ? (
          <Button variant="outline" onClick={sendSetupLink} disabled={loading}>
            {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
            Recevoir un lien pour définir un mot de passe
          </Button>
        ) : (
          <form onSubmit={submit} className="grid gap-4 sm:grid-cols-3" noValidate>
            <div className="space-y-1">
              <Label htmlFor="current">Mot de passe actuel</Label>
              <Input id="current" type="password" autoComplete="current-password" value={current}
                     onChange={(e) => setCurrent(e.target.value)} disabled={loading} />
            </div>
            <div className="space-y-1">
              <Label htmlFor="next">Nouveau</Label>
              <Input id="next" type="password" autoComplete="new-password" value={next}
                     onChange={(e) => setNext(e.target.value)} disabled={loading} />
              <FieldError message={errors.next} />
            </div>
            <div className="space-y-1">
              <Label htmlFor="confirm">Confirmer</Label>
              <Input id="confirm" type="password" autoComplete="new-password" value={confirm}
                     onChange={(e) => setConfirm(e.target.value)} disabled={loading} />
              <FieldError message={errors.confirm} />
            </div>
            <div className="sm:col-span-3">
              <Button type="submit" className="bg-primary" disabled={loading || !current}>
                {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                Changer le mot de passe
              </Button>
            </div>
          </form>
        )}
      </CardContent>
    </Card>
  );
}
