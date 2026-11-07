"use client";

import { Suspense, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { AlertCircle, Loader2, LogIn, MailCheck, UserPlus } from "lucide-react";
import { apiClient } from "@/api/client";
import { AuthCard, FieldError } from "@/components/auth/AuthCard";
import { GoogleButton } from "@/components/auth/GoogleButton";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { afterLoginPath, useSession } from "@/auth/AuthProvider";
import { errorCode, errorMessage, googleErrorMessage } from "@/auth/errors";
import { emailError, passwordError, usernameError } from "@/auth/rules";

export default function LoginPage() {
  return (
    <Suspense fallback={<FullPageSpinner />}>
      <LoginContent />
    </Suspense>
  );
}

function FullPageSpinner() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-background">
      <Loader2 className="h-10 w-10 animate-spin text-primary" />
    </div>
  );
}

function LoginContent() {
  const { user, isLoading } = useSession();
  const router = useRouter();
  const searchParams = useSearchParams();
  const [tab, setTab] = useState("login");
  const googleError = googleErrorMessage(searchParams.get("error"));

  useEffect(() => {
    if (user && !isLoading) {
      router.replace(afterLoginPath(user));
    }
  }, [user, isLoading, router]);

  if (isLoading || user) {
    return <FullPageSpinner />;
  }

  return (
    <AuthCard title="AgriShare" description={tab === "login" ? "Accédez à votre espace" : "Créez votre compte investisseur"}>
      {googleError && (
        <Alert variant="destructive" className="mb-6">
          <AlertCircle className="h-4 w-4" />
          <AlertTitle>Connexion impossible</AlertTitle>
          <AlertDescription>{googleError}</AlertDescription>
        </Alert>
      )}

      <Tabs value={tab} onValueChange={setTab}>
        <TabsList className="grid w-full grid-cols-2 mb-6">
          <TabsTrigger value="login">Connexion</TabsTrigger>
          <TabsTrigger value="register">Inscription</TabsTrigger>
        </TabsList>
        <TabsContent value="login">
          <LoginForm />
        </TabsContent>
        <TabsContent value="register">
          <RegisterForm />
        </TabsContent>
      </Tabs>

      <div className="flex items-center gap-3 my-6">
        <Separator className="flex-1" />
        <span className="text-xs text-muted-foreground">ou</span>
        <Separator className="flex-1" />
      </div>
      <GoogleButton />
    </AuthCard>
  );
}

function LoginForm() {
  const { refresh } = useSession();
  const router = useRouter();
  const [identifier, setIdentifier] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notVerified, setNotVerified] = useState(false);
  const [resent, setResent] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setNotVerified(false);
    setLoading(true);
    try {
      const { response, error: apiError } = await apiClient.POST("/api/auth/login", {
        body: { identifier: identifier.trim(), password },
      });
      if (!response.ok) {
        setNotVerified(errorCode(apiError) === "EMAIL_NOT_VERIFIED");
        setError(errorMessage(apiError));
        return;
      }
      const me = await refresh();
      router.replace(afterLoginPath(me));
    } catch {
      setError("Le serveur est injoignable. Vérifiez votre connexion et réessayez.");
    } finally {
      setLoading(false);
    }
  };

  const resend = async () => {
    // The API answers the same way whatever the address: nothing to read back
    await apiClient.POST("/api/auth/resend-verification", { body: { email: identifier.trim() } });
    setResent(true);
  };

  return (
    <form onSubmit={submit} className="space-y-4">
      {error && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>
            {error}
            {notVerified && identifier.includes("@") && !resent && (
              <Button type="button" variant="link" className="h-auto p-0 ml-1" onClick={resend}>
                Renvoyer l'e-mail
              </Button>
            )}
            {resent && <span className="block mt-1">E-mail renvoyé, pensez à vérifier vos spams.</span>}
          </AlertDescription>
        </Alert>
      )}
      <div className="space-y-2">
        <Label htmlFor="identifier">E-mail ou nom d'utilisateur</Label>
        <Input id="identifier" autoComplete="username" value={identifier}
               onChange={(e) => setIdentifier(e.target.value)} disabled={loading} required />
      </div>
      <div className="space-y-2">
        <div className="flex items-center justify-between">
          <Label htmlFor="password">Mot de passe</Label>
          <Link href="/forgot-password" className="text-xs text-primary hover:underline">Mot de passe oublié ?</Link>
        </div>
        <Input id="password" type="password" autoComplete="current-password" value={password}
               onChange={(e) => setPassword(e.target.value)} disabled={loading} required />
      </div>
      <Button type="submit" className="w-full bg-primary" disabled={loading}>
        {loading ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <LogIn className="mr-2 h-4 w-4" />}
        Se connecter
      </Button>
    </form>
  );
}

function RegisterForm() {
  const [form, setForm] = useState({ email: "", username: "", displayName: "", password: "", confirm: "" });
  const [errors, setErrors] = useState<Record<string, string | null>>({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [sentTo, setSentTo] = useState<string | null>(null);

  const set = (field: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm({ ...form, [field]: e.target.value });

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    const fieldErrors = {
      email: emailError(form.email),
      username: usernameError(form.username),
      password: passwordError(form.password),
      confirm: form.password === form.confirm ? null : "Les deux mots de passe ne correspondent pas.",
    };
    setErrors(fieldErrors);
    if (Object.values(fieldErrors).some(Boolean)) {
      return;
    }

    setLoading(true);
    try {
      const { response, error: apiError } = await apiClient.POST("/api/auth/register", {
        body: {
          email: form.email.trim(),
          username: form.username.trim().toLowerCase(),
          password: form.password,
          displayName: form.displayName.trim() || undefined,
        },
      });
      if (!response.ok) {
        setError(errorMessage(apiError));
        return;
      }
      setSentTo(form.email.trim());
    } catch {
      setError("Le serveur est injoignable. Vérifiez votre connexion et réessayez.");
    } finally {
      setLoading(false);
    }
  };

  if (sentTo) {
    return (
      <Alert>
        <MailCheck className="h-4 w-4" />
        <AlertTitle>Vérifiez votre boîte mail</AlertTitle>
        <AlertDescription>
          Si l'adresse <strong>{sentTo}</strong> peut être utilisée, un e-mail de confirmation vient de lui être
          envoyé. Ouvrez le lien qu'il contient (valable 24 heures) pour activer votre compte, puis connectez-vous.
        </AlertDescription>
      </Alert>
    );
  }

  return (
    <form onSubmit={submit} className="space-y-4" noValidate>
      {error && (
        <Alert variant="destructive">
          <AlertCircle className="h-4 w-4" />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}
      <div className="space-y-1">
        <Label htmlFor="reg-email">E-mail</Label>
        <Input id="reg-email" type="email" autoComplete="email" value={form.email} onChange={set("email")} disabled={loading} />
        <FieldError message={errors.email} />
      </div>
      <div className="space-y-1">
        <Label htmlFor="reg-username">Nom d'utilisateur</Label>
        <Input id="reg-username" autoComplete="username" value={form.username} onChange={set("username")} disabled={loading} />
        <FieldError message={errors.username} />
      </div>
      <div className="space-y-1">
        <Label htmlFor="reg-name">Nom affiché <span className="text-muted-foreground">(facultatif)</span></Label>
        <Input id="reg-name" autoComplete="name" value={form.displayName} onChange={set("displayName")} disabled={loading} />
      </div>
      <div className="space-y-1">
        <Label htmlFor="reg-password">Mot de passe</Label>
        <Input id="reg-password" type="password" autoComplete="new-password" value={form.password} onChange={set("password")} disabled={loading} />
        <FieldError message={errors.password} />
      </div>
      <div className="space-y-1">
        <Label htmlFor="reg-confirm">Confirmer le mot de passe</Label>
        <Input id="reg-confirm" type="password" autoComplete="new-password" value={form.confirm} onChange={set("confirm")} disabled={loading} />
        <FieldError message={errors.confirm} />
      </div>
      <Button type="submit" className="w-full bg-primary" disabled={loading}>
        {loading ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <UserPlus className="mr-2 h-4 w-4" />}
        Créer mon compte
      </Button>
    </form>
  );
}
