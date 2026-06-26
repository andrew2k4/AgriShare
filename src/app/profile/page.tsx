"use client";

import { Navbar } from "@/components/layout/Navbar";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { User, Mail, Shield, LogOut, Settings, Bell, FileText, Loader2 } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { useUser, useAuth } from "@/firebase";
import { signOut } from "firebase/auth";
import { useRouter } from "next/navigation";

export default function ProfilePage() {
  const { user, isUserLoading } = useUser();
  const auth = useAuth();
  const router = useRouter();

  const handleLogout = async () => {
    await signOut(auth);
    router.push("/login");
  };

  if (isUserLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <Loader2 className="h-10 w-10 animate-spin text-primary" />
      </div>
    );
  }

  if (!user) {
    router.push("/login");
    return null;
  }

  return (
    <div className="min-h-screen bg-background pb-12">
      <Navbar />
      <main className="container mx-auto px-4 py-8">
        <header className="mb-8">
          <h1 className="text-3xl font-bold text-primary">Mon Profil</h1>
          <p className="text-muted-foreground">Gérez vos informations et préférences.</p>
        </header>

        <div className="grid gap-6 md:grid-cols-3">
          <div className="md:col-span-1 space-y-6">
            <Card>
              <CardContent className="pt-6">
                <div className="flex flex-col items-center text-center space-y-3">
                  <div className="h-24 w-24 rounded-full bg-primary/10 flex items-center justify-center text-primary border-2 border-primary/20">
                    <User className="h-12 w-12" />
                  </div>
                  <div>
                    <h2 className="text-xl font-bold">{user.displayName || "Utilisateur"}</h2>
                    <Badge className="bg-primary hover:bg-primary/90 mt-1">Investisseur</Badge>
                  </div>
                  <div className="w-full pt-4 space-y-2">
                    <div className="flex items-center gap-2 text-sm text-muted-foreground justify-center">
                      <Mail className="h-4 w-4" />
                      {user.email}
                    </div>
                  </div>
                  <Button variant="outline" className="w-full border-primary text-primary mt-4">
                    Modifier Profil
                  </Button>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-lg">Paramètres rapides</CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                 <Button variant="ghost" className="w-full justify-start gap-3 h-12">
                   <Bell className="h-5 w-5 text-muted-foreground" />
                   Notifications
                 </Button>
                 <Button variant="ghost" className="w-full justify-start gap-3 h-12">
                   <Shield className="h-5 w-5 text-muted-foreground" />
                   Sécurité & MDP
                 </Button>
                 <Button 
                   variant="ghost" 
                   className="w-full justify-start gap-3 h-12 text-destructive hover:text-destructive hover:bg-destructive/5"
                   onClick={handleLogout}
                 >
                   <LogOut className="h-5 w-5" />
                   Déconnexion
                 </Button>
              </CardContent>
            </Card>
          </div>

          <div className="md:col-span-2 space-y-6">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Settings className="h-5 w-5 text-primary" />
                  Configuration de la Ferme
                </CardTitle>
                <CardDescription>Paramètres de l'actif fixe et gestion globale.</CardDescription>
              </CardHeader>
              <CardContent className="space-y-6">
                <div className="grid gap-4 sm:grid-cols-2">
                  <div className="p-4 rounded-lg border bg-muted/30">
                    <p className="text-xs text-muted-foreground uppercase font-bold mb-1">Valeur estimée du terrain</p>
                    <p className="text-lg font-bold text-primary">15,000,000 FCFA</p>
                  </div>
                  <div className="p-4 rounded-lg border bg-muted/30">
                    <p className="text-xs text-muted-foreground uppercase font-bold mb-1">Loyer interne calculé</p>
                    <p className="text-lg font-bold text-primary">50,000 FCFA / mois</p>
                  </div>
                </div>

                <div className="p-6 border rounded-lg bg-primary/5 border-primary/20">
                  <h4 className="font-bold text-sm text-primary flex items-center gap-2 mb-2">
                    <Shield className="h-4 w-4" /> Mode Administrateur
                  </h4>
                  <p className="text-xs text-muted-foreground leading-relaxed">
                    Vous avez accès à la gestion complète des projets et des vétérinaires. 
                    Toutes les modifications sont tracées pour assurer la transparence envers les co-investisseurs.
                  </p>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <FileText className="h-5 w-5 text-primary" />
                  Rapports & Exports
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="flex items-center justify-between p-3 border rounded-lg hover:bg-muted/10 cursor-pointer transition-colors">
                  <span className="text-sm font-medium">Bilan Financier Mensuel (PDF)</span>
                  <Button size="sm" variant="outline">Télécharger</Button>
                </div>
                <div className="flex items-center justify-between p-3 border rounded-lg hover:bg-muted/10 cursor-pointer transition-colors">
                  <span className="text-sm font-medium">Répartition des dividendes (Excel)</span>
                  <Button size="sm" variant="outline">Télécharger</Button>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </main>
    </div>
  );
}