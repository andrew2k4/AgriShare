"use client";

import { useState, useEffect } from "react";
import { Navbar } from "@/components/layout/Navbar";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription, CardFooter } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuth, useUser } from "@/firebase";
import { useRouter } from "next/navigation";
import { Sprout, Mail, Lock, LogIn, UserPlus, Loader2, AlertCircle, ShieldCheck } from "lucide-react";
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
        case 'auth/invalid-credential':
          friendlyMessage = "Identifiants invalides. Vérifiez votre e-mail/mot de passe ou créez un compte.";
          break;
        case 'auth/user-not-found':
          friendlyMessage = "Aucun compte trouvé. Veuillez vous inscrire.";
          break;
        case 'auth/wrong-password':
          friendlyMessage = "Mot de passe incorrect.";
          break;
        case 'auth/email-already-in-use':
          friendlyMessage = "Cet e-mail est déjà utilisé.";
          break;
        case 'auth/weak-password':
          friendlyMessage = "Le mot de passe est trop court (min. 6 caractères).";
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

  const fillAdminCredentials = () => {
    setEmail("admin@agrishare.cm");
    setPassword("password123");
    setIsSignUp(false);
  };

  if (isUserLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <Loader2 className="h-10 w-10 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <main className="container mx-auto px-4 py-12 flex flex-col items-center">
        <Card className="w-full max-w-md shadow-xl border-primary/20 bg-white">
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
                    placeholder="admin@agrishare.cm" 
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

            {!isSignUp && (
              <div className="mt-4">
                <Button 
                  variant="outline" 
                  className="w-full border-primary/20 text-primary hover:bg-primary/5 h-12"
                  onClick={fillAdminCredentials}
                >
                  <ShieldCheck className="mr-2 h-5 w-5" />
                  Utiliser le compte Test Admin
                </Button>
                <p className="text-[10px] text-center text-muted-foreground mt-2 italic">
                  Note: Si le compte n'existe pas, cliquez sur "Créer un compte" avec ces identifiants.
                </p>
              </div>
            )}
          </CardContent>
          <CardFooter className="flex flex-col gap-4 border-t pt-4">
            <Button variant="link" onClick={() => { setIsSignUp(!isSignUp); setErrorMessage(null); }} className="text-primary text-sm" disabled={loading}>
              {isSignUp ? "Déjà un compte ? Se connecter" : "Nouveau ? Créer un compte"}
            </Button>
          </CardFooter>
        </Card>
      </main>
    </div>
  );
}