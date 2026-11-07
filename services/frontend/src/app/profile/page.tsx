"use client";

import Link from "next/link";
import { Navbar } from "@/components/layout/Navbar";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { User, Mail, Shield, LogOut, Settings, FileText, Loader2, AtSign, BadgeCheck } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { ROLE_LABELS, useSession } from "@/auth/AuthProvider";
import { PasswordCard } from "@/components/auth/PasswordCard";
import { SessionsCard } from "@/components/auth/SessionsCard";

export default function ProfilePage() {
  const { user, isLoading, logout } = useSession();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-background">
        <Loader2 className="h-10 w-10 animate-spin text-primary" />
      </div>
    );
  }

  if (!user) {
    // The session provider sends signed-out visitors to /login
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
                    <h2 className="text-xl font-bold">{user.name || user.username || "Utilisateur"}</h2>
                    <Badge className="bg-primary hover:bg-primary/90 mt-1">{ROLE_LABELS[user.role] ?? user.role}</Badge>
                  </div>
                  <div className="w-full pt-4 space-y-2">
                    {user.username ? (
                      <div className="flex items-center gap-2 text-sm text-muted-foreground justify-center">
                        <AtSign className="h-4 w-4" />
                        {user.username}
                      </div>
                    ) : (
                      <Link href="/choose-username" className="flex items-center gap-2 text-sm text-primary hover:underline justify-center">
                        <AtSign className="h-4 w-4" />
                        Choisir un nom d'utilisateur
                      </Link>
                    )}
                    <div className="flex items-center gap-2 text-sm text-muted-foreground justify-center">
                      <Mail className="h-4 w-4" />
                      {user.email}
                      {user.emailVerified && <BadgeCheck className="h-4 w-4 text-primary" aria-label="Adresse vérifiée" />}
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>

            <Button
              variant="ghost"
              className="w-full justify-start gap-3 h-12 text-destructive hover:text-destructive hover:bg-destructive/5"
              onClick={logout}
            >
              <LogOut className="h-5 w-5" />
              Déconnexion
            </Button>
          </div>

          <div className="md:col-span-2 space-y-6">
            <PasswordCard />
            <SessionsCard />

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

                {user.role === "ADMIN" && (
                <div className="p-6 border rounded-lg bg-primary/5 border-primary/20">
                  <h4 className="font-bold text-sm text-primary flex items-center gap-2 mb-2">
                    <Shield className="h-4 w-4" /> Mode Administrateur
                  </h4>
                  <p className="text-xs text-muted-foreground leading-relaxed">
                    Vous avez accès à la gestion complète des projets et des vétérinaires. 
                    Toutes les modifications sont tracées pour assurer la transparence envers les co-investisseurs.
                  </p>
                </div>
                )}
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