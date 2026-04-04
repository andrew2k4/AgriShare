
"use client";

import { useState, useEffect } from "react";
import { Navbar } from "@/components/layout/Navbar";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription, CardFooter } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuth, useUser } from "@/firebase";
import { useRouter } from "next/navigation";
import { Sprout, Mail, Lock, LogIn, UserPlus, Loader2, AlertCircle } from "lucide-react";
import { toast } from "@/hooks/use-toast";
import { signInWithPopup, GoogleAuthProvider, signInWithEmailAndPassword, createUserWithEmailAndPassword } from "firebase/auth";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";

export default function LoginPage() {
  const { user, isUserLoading } = useUser();
  const auth = useAuth();
  const router = useRouter();
  const [isSignUp, setIsSignUp] = useState(false);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (user && !isUserLoading) {
      router.push("/");
    }
  }, [user, isUserLoading, router]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (!email || !password) {
      toast({
        variant: "destructive",
        title: "Champs requis",
        description: "Veuillez remplir l'e-mail et le mot de passe.",
      });
      return;
    }

    setLoading(true);
    try {
      if (isSignUp) {
        await createUserWithEmailAndPassword(auth, email, password);
        toast({
          title: "Compte créé",
          description: "Bienvenue sur AgriShare !",
        });
      } else {
        await signInWithEmailAndPassword(auth, email, password);
        toast({
          title: "Connexion réussie",
          description: "Content de vous revoir.",
        });
      }
    } catch (error: any) {
      console.error("Auth error:", error);
      let friendlyMessage = "Une erreur inattendue est survenue.";
      
      switch (error.code) {
        case 'auth/invalid-email':
          friendlyMessage = "L'adresse e-mail n'est pas valide.";
          break;
        case 'auth/user-disabled':
          friendlyMessage = "Ce compte a été désactivé.";
          break;
        case 'auth/user-not-found':
          friendlyMessage = "Aucun compte trouvé avec cet e-mail. Veuillez vous inscrire.";
          break;
        case 'auth/wrong-password':
          friendlyMessage = "Le mot de passe est incorrect.";
          break;
        case 'auth/email-already-in-use':
          friendlyMessage = "Cet e-mail est déjà utilisé par un autre compte.";
          break;
        case 'auth/weak-password':
          friendlyMessage = "Le mot de passe doit contenir au moins 6 caractères.";
          break;
        case 'auth/operation-not-allowed':
          friendlyMessage = "La connexion par e-mail n'est pas activée dans la console Firebase.";
          break;
        case 'auth/popup-blocked':
          friendlyMessage = "Le popup de connexion a été bloqué par votre navigateur.";
          break;
        default:
          friendlyMessage = error.message || "Erreur d'authentification.";
      }
      
      setErrorMessage(friendlyMessage);
      toast({
        variant: "destructive",
        title: "Erreur",
        description: friendlyMessage,
      });
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleSignIn = async () => {
    setErrorMessage(null);
    setLoading(true);
    const provider = new GoogleAuthProvider();
    try {
      await signInWithPopup(auth, provider);
      toast({
        title: "Connexion Google réussie",
      });
    } catch (error: any) {
      console.error("Google Auth error:", error);
      setErrorMessage(error.message);
      toast({
        variant: "destructive",
        title: "Erreur Google",
        description: error.message,
      });
    } finally {
      setLoading(false);
    }
  };

  if (isUserLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <div className="flex flex-col items-center gap-4">
          <Loader2 className="h-10 w-10 animate-spin text-primary" />
          <p className="text-muted-foreground animate-pulse">Chargement d'AgriShare...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <main className="container mx-auto px-4 py-12 flex justify-center items-center">
        <Card className="w-full max-w-md shadow-xl border-primary/20">
          <CardHeader className="text-center space-y-1">
            <div className="mx-auto bg-primary w-12 h-12 rounded-xl flex items-center justify-center mb-2 shadow-lg shadow-primary/20">
              <Sprout className="text-white h-7 w-7" />
            </div>
            <CardTitle className="text-2xl font-bold text-primary">AgriShare</CardTitle>
            <CardDescription>
              {isSignUp ? "Créez votre compte investisseur" : "Accédez à votre espace gestion"}
            </CardDescription>
          </CardHeader>
          <CardContent>
            {errorMessage && (
              <Alert variant="destructive" className="mb-6">
                <AlertCircle className="h-4 w-4" />
                <AlertTitle>Erreur</AlertTitle>
                <AlertDescription>{errorMessage}</AlertDescription>
              </Alert>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <div className="relative">
                  <Mail className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
                  <Input 
                    id="email" 
                    type="email" 
                    placeholder="nom@exemple.com" 
                    className="pl-10"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    disabled={loading}
                    required
                  />
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="password">Mot de passe</Label>
                <div className="relative">
                  <Lock className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
                  <Input 
                    id="password" 
                    type="password" 
                    className="pl-10"
                    placeholder="••••••••"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    disabled={loading}
                    required
                  />
                </div>
              </div>
              <Button type="submit" className="w-full bg-primary mt-2" disabled={loading}>
                {loading ? (
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                ) : isSignUp ? (
                  <UserPlus className="mr-2 h-4 w-4" />
                ) : (
                  <LogIn className="mr-2 h-4 w-4" />
                )}
                {isSignUp ? "Créer mon compte" : "Se connecter"}
              </Button>
            </form>

            <div className="relative my-6">
              <div className="absolute inset-0 flex items-center">
                <span className="w-full border-t" />
              </div>
              <div className="relative flex justify-center text-xs uppercase">
                <span className="bg-background px-2 text-muted-foreground">Ou</span>
              </div>
            </div>

            <Button variant="outline" className="w-full" onClick={handleGoogleSignIn} disabled={loading}>
              <svg className="mr-2 h-4 w-4" viewBox="0 0 24 24">
                <path
                  d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                  fill="#4285F4"
                />
                <path
                  d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                  fill="#34A853"
                />
                <path
                  d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
                  fill="#FBBC05"
                />
                <path
                  d="M12 5.38c1.62 0 3.06.56 4.21 1.66l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                  fill="#EA4335"
                />
              </svg>
              Continuer avec Google
            </Button>
          </CardContent>
          <CardFooter className="flex justify-center border-t pt-4">
            <Button variant="link" onClick={() => { setIsSignUp(!isSignUp); setErrorMessage(null); }} className="text-primary text-sm" disabled={loading}>
              {isSignUp ? "Vous avez déjà un compte ? Se connecter" : "Nouveau sur AgriShare ? Créer un compte"}
            </Button>
          </CardFooter>
        </Card>
      </main>
    </div>
  );
}
