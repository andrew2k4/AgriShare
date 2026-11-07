"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AlertCircle, Loader2 } from "lucide-react";
import { apiClient } from "@/api/client";
import { AuthCard, FieldError } from "@/components/auth/AuthCard";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { postponeUsername, useSession } from "@/auth/AuthProvider";
import { errorMessage } from "@/auth/errors";
import { usernameError } from "@/auth/rules";

/** Accounts created with Google have no username yet: they choose one here, once. */
export default function ChooseUsernamePage() {
  const { user, isLoading, refresh } = useSession();
  const router = useRouter();
  const [username, setUsername] = useState("");
  const [fieldError, setFieldError] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!isLoading && user?.username) {
      router.replace("/");
    }
  }, [isLoading, user, router]);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    const invalid = usernameError(username);
    setFieldError(invalid);
    if (invalid) {
      return;
    }
    setLoading(true);
    try {
      const { response, error: apiError } = await apiClient.PUT("/api/auth/username", {
        body: { username: username.trim().toLowerCase() },
      });
      if (!response.ok) {
        setError(errorMessage(apiError));
        return;
      }
      await refresh();
      router.replace("/");
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthCard
      title="Choisissez un nom d'utilisateur"
      description="Vous pourrez l'utiliser pour vous connecter, à la place de votre adresse e-mail. Il ne pourra plus être modifié. Vous pourrez aussi le choisir plus tard depuis votre profil."
      footer={
        <Button variant="link" onClick={() => {
          if (user) {
            postponeUsername(user);
          }
          router.replace("/");
        }}>
          Plus tard
        </Button>
      }
    >
      <form onSubmit={submit} className="space-y-4" noValidate>
        {error && (
          <Alert variant="destructive">
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}
        <div className="space-y-1">
          <Label htmlFor="username">Nom d'utilisateur</Label>
          <Input id="username" autoComplete="username" value={username}
                 onChange={(e) => setUsername(e.target.value)} disabled={loading} />
          <FieldError message={fieldError} />
        </div>
        <Button type="submit" className="w-full bg-primary" disabled={loading}>
          {loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
          Valider
        </Button>
      </form>
    </AuthCard>
  );
}
