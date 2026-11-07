"use client";

import { useCallback, useEffect, useState } from "react";
import { formatDistanceToNow } from "date-fns";
import { fr } from "date-fns/locale";
import { Laptop, Loader2, LogOut, Smartphone } from "lucide-react";
import { apiClient } from "@/api/client";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { toast } from "@/hooks/use-toast";
import { useSession } from "@/auth/AuthProvider";
import { errorMessage } from "@/auth/errors";

interface DeviceSession {
  handle: string;
  createdAt: string;
  lastAccessedAt: string;
  ipAddress?: string;
  userAgent?: string;
  current: boolean;
}

/** "Chrome sur macOS"-style label from a user agent, good enough to recognise a device. */
function describe(userAgent?: string): { label: string; mobile: boolean } {
  if (!userAgent) {
    return { label: "Appareil inconnu", mobile: false };
  }
  const browser = /Edg\//.test(userAgent) ? "Edge" : /Firefox\//.test(userAgent) ? "Firefox"
    : /Chrome\//.test(userAgent) ? "Chrome" : /Safari\//.test(userAgent) ? "Safari" : "Navigateur";
  const os = /Android/.test(userAgent) ? "Android" : /iPhone|iPad/.test(userAgent) ? "iOS"
    : /Mac OS X/.test(userAgent) ? "macOS" : /Windows/.test(userAgent) ? "Windows"
    : /Linux/.test(userAgent) ? "Linux" : "système inconnu";
  return { label: `${browser} sur ${os}`, mobile: /Mobile|Android|iPhone/.test(userAgent) };
}

export function SessionsCard() {
  const { signedOut } = useSession();
  const [sessions, setSessions] = useState<DeviceSession[] | null>(null);
  const [busy, setBusy] = useState<string | null>(null);

  const load = useCallback(async () => {
    const { data } = await apiClient.GET("/api/auth/sessions");
    setSessions((data as unknown as DeviceSession[]) ?? []);
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const revoke = async (handle: string) => {
    setBusy(handle);
    const { response, error } = await apiClient.DELETE("/api/auth/sessions/{handle}", { params: { path: { handle } } });
    setBusy(null);
    if (!response.ok) {
      toast({ variant: "destructive", title: "Erreur", description: errorMessage(error) });
      return;
    }
    toast({ title: "Appareil déconnecté" });
    void load();
  };

  const revokeAll = async () => {
    setBusy("all");
    try {
      const { response, error } = await apiClient.POST("/api/auth/logout-all");
      if (!response.ok) {
        toast({ variant: "destructive", title: "Appareils non déconnectés", description: errorMessage(error) });
        return;
      }
      // This session ended with the others: nothing left to sign out on the server
      signedOut();
    } catch {
      toast({ variant: "destructive", title: "Appareils non déconnectés", description: "Le serveur est injoignable." });
    } finally {
      setBusy(null);
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Laptop className="h-5 w-5 text-primary" />
          Appareils connectés
        </CardTitle>
        <CardDescription>Déconnectez un appareil que vous ne reconnaissez pas ou que vous n'utilisez plus.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-3">
        {sessions === null ? (
          <div className="flex justify-center py-4"><Loader2 className="h-6 w-6 animate-spin text-primary" /></div>
        ) : sessions.length === 0 ? (
          <p className="text-sm text-muted-foreground">Aucune session ouverte.</p>
        ) : (
          sessions.map((session) => {
            const device = describe(session.userAgent);
            const Icon = device.mobile ? Smartphone : Laptop;
            return (
              <div key={session.handle} className="flex items-center justify-between gap-3 p-3 border rounded-lg">
                <div className="flex items-center gap-3 min-w-0">
                  <Icon className="h-5 w-5 text-muted-foreground shrink-0" />
                  <div className="min-w-0">
                    <div className="text-sm font-medium flex items-center gap-2">
                      {device.label}
                      {session.current && <Badge variant="secondary">Cet appareil</Badge>}
                    </div>
                    <p className="text-xs text-muted-foreground truncate">
                      {session.ipAddress ?? "IP inconnue"} · actif{" "}
                      {formatDistanceToNow(new Date(session.lastAccessedAt), { addSuffix: true, locale: fr })}
                    </p>
                  </div>
                </div>
                {!session.current && (
                  <Button size="sm" variant="outline" disabled={busy !== null} onClick={() => revoke(session.handle)}>
                    {busy === session.handle ? <Loader2 className="h-4 w-4 animate-spin" /> : "Déconnecter"}
                  </Button>
                )}
              </div>
            );
          })
        )}
        <Button variant="ghost" className="w-full text-destructive hover:text-destructive hover:bg-destructive/5"
                disabled={busy !== null} onClick={revokeAll}>
          {busy === "all" ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <LogOut className="mr-2 h-4 w-4" />}
          Déconnecter tous les appareils, y compris celui-ci
        </Button>
      </CardContent>
    </Card>
  );
}
