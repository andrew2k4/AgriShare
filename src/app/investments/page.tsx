"use client";

import { useState, useEffect } from "react";
import { Navbar } from "@/components/layout/Navbar";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription,
} from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { PlusCircle, Wallet, Users, History, RefreshCcw } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { api } from "@/api";
import type { components } from "@/api";
import { toast } from "@/hooks/use-toast";

type InvestorDto = components["schemas"]["InvestorDto"];

export default function InvestmentsPage() {
  const [investors, setInvestors] = useState<InvestorDto[]>([]);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({ fullName: "", contribution: "" });

  const fetchInvestors = async () => {
    const { data } = await api.investors.getAll();
    if (data) setInvestors(data);
  };

  useEffect(() => {
    fetchInvestors();
  }, []);

  const totalInvested = investors.reduce((s, i) => s + i.contribution, 0);

  const handleCreate = async () => {
    if (!form.fullName || !form.contribution) return;
    setSaving(true);
    const { error } = await api.investors.create({
      fullName: form.fullName,
      contribution: Number(form.contribution),
    });
    setSaving(false);
    if (error) {
      toast({
        variant: "destructive",
        title: "Erreur",
        description: "Impossible d'enregistrer l'investissement.",
      });
      return;
    }
    toast({
      title: "Investissement ajouté",
      description: `L'apport de ${form.fullName} a été enregistré.`,
    });
    setForm({ fullName: "", contribution: "" });
    setDialogOpen(false);
    fetchInvestors();
  };

  return (
    <div className="min-h-screen bg-background pb-12">
      <Navbar />
      <main className="container mx-auto px-4 py-8">
        <header className="mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="space-y-1">
            <h1 className="text-3xl font-bold text-primary">
              Gestion des Investissements
            </h1>
            <p className="text-muted-foreground">
              Suivez les capitaux et l'évolution des parts dynamiques.
            </p>
          </div>
          <Button
            className="w-full md:w-auto bg-primary"
            onClick={() => setDialogOpen(true)}
          >
            <PlusCircle className="mr-2 h-4 w-4" />
            Nouvel Investissement
          </Button>
        </header>

        <div className="grid gap-6 lg:grid-cols-3">
          <div className="lg:col-span-1 space-y-6">
            <Card className="bg-primary text-primary-foreground">
              <CardHeader className="pb-2">
                <CardTitle className="text-lg flex items-center gap-2">
                  <Wallet className="h-5 w-5" />
                  Total Investi
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-3xl font-bold">
                  {totalInvested.toLocaleString()} FCFA
                </div>
                <p className="text-sm opacity-80 mt-1">
                  Capital social de l'activité
                </p>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-lg flex items-center gap-2">
                  <Users className="h-5 w-5 text-primary" />
                  Répartition des Parts
                </CardTitle>
                <CardDescription>Basé sur le total investi</CardDescription>
              </CardHeader>
              <CardContent>
                {investors.length === 0 ? (
                  <p className="text-sm text-muted-foreground text-center py-2">
                    Aucun investisseur.
                  </p>
                ) : (
                  <div className="space-y-4">
                    {investors.map((inv) => {
                      const pct =
                        totalInvested > 0
                          ? ((inv.contribution / totalInvested) * 100).toFixed(1)
                          : "0.0";
                      return (
                        <div key={inv.id} className="space-y-2">
                          <div className="flex justify-between text-sm">
                            <span className="font-medium">{inv.fullName}</span>
                            <span className="font-bold">{pct}%</span>
                          </div>
                          <div className="w-full bg-secondary h-2 rounded-full overflow-hidden">
                            <div
                              className="bg-primary h-full transition-all duration-500"
                              style={{ width: `${pct}%` }}
                            />
                          </div>
                          <div className="text-xs text-muted-foreground text-right">
                            {inv.contribution.toLocaleString()} FCFA
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </CardContent>
            </Card>
          </div>

          <div className="lg:col-span-2">
            <Card>
              <CardHeader className="flex flex-row items-center justify-between">
                <div>
                  <CardTitle className="text-lg flex items-center gap-2">
                    <History className="h-5 w-5 text-primary" />
                    Liste des investisseurs
                  </CardTitle>
                </div>
              </CardHeader>
              <CardContent>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Investisseur</TableHead>
                      <TableHead className="text-right">Apport (FCFA)</TableHead>
                      <TableHead className="text-right">Part</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {investors.length === 0 ? (
                      <TableRow>
                        <TableCell
                          colSpan={3}
                          className="text-center text-muted-foreground py-8"
                        >
                          Aucun investissement enregistré.
                        </TableCell>
                      </TableRow>
                    ) : (
                      investors.map((inv) => {
                        const pct =
                          totalInvested > 0
                            ? ((inv.contribution / totalInvested) * 100).toFixed(1)
                            : "0.0";
                        return (
                          <TableRow key={inv.id}>
                            <TableCell className="font-medium">
                              {inv.fullName}
                            </TableCell>
                            <TableCell className="text-right font-bold">
                              {inv.contribution.toLocaleString()} F
                            </TableCell>
                            <TableCell className="text-right">
                              <Badge
                                variant="outline"
                                className="bg-primary/5 text-primary"
                              >
                                {pct}%
                              </Badge>
                            </TableCell>
                          </TableRow>
                        );
                      })
                    )}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          </div>
        </div>
      </main>

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Nouvel Investissement</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-2">
            <div className="space-y-2">
              <Label>Nom de l'investisseur</Label>
              <Input
                placeholder="Ex: Jean-Pierre Ndongo"
                value={form.fullName}
                onChange={(e) => setForm({ ...form, fullName: e.target.value })}
              />
            </div>
            <div className="space-y-2">
              <Label>Montant (FCFA)</Label>
              <Input
                type="number"
                placeholder="Ex: 500000"
                value={form.contribution}
                onChange={(e) =>
                  setForm({ ...form, contribution: e.target.value })
                }
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDialogOpen(false)}>
              Annuler
            </Button>
            <Button
              className="bg-primary"
              disabled={!form.fullName || !form.contribution || saving}
              onClick={handleCreate}
            >
              {saving && <RefreshCcw className="mr-2 h-4 w-4 animate-spin" />}
              Enregistrer
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
