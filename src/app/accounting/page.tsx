"use client";

import { useState, useEffect } from "react";
import { Navbar } from "@/components/layout/Navbar";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import {
  PlusCircle,
  ArrowUpRight,
  ArrowDownRight,
  Layers,
  RefreshCcw,
} from "lucide-react";
import { api } from "@/api/api";
import type { components } from "@/api/schema";
import { toast } from "@/hooks/use-toast";

type OperationDto = components["schemas"]["OperationDto"];

export default function AccountingPage() {
  const [operations, setOperations] = useState<OperationDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({
    label: "",
    date: new Date().toISOString().split("T")[0],
    amount: "",
    operationType: "" as "INCOME" | "EXPENSE" | "",
  });

  async function fetchOperations() {
    setLoading(true);
    const { data, error } = await api.operations.getAll();
    if (error || !data) {
      toast({ variant: "destructive", title: "Erreur", description: "Impossible de charger les opérations." });
    } else {
      setOperations(data);
    }
    setLoading(false);
  }

  useEffect(() => {
    fetchOperations();
  }, []);

  const totalIncome = operations
    .filter((op) => op.operationType === "INCOME")
    .reduce((sum, op) => sum + op.amount, 0);

  const totalExpense = operations
    .filter((op) => op.operationType === "EXPENSE")
    .reduce((sum, op) => sum + op.amount, 0);

  const netProfit = totalIncome - totalExpense;

  async function handleCreate() {
    if (!form.label || !form.date || !form.amount || !form.operationType) return;
    setSaving(true);
    const { error } = await api.operations.create({
      label: form.label,
      date: form.date,
      amount: Number(form.amount),
      operationType: form.operationType,
    });
    setSaving(false);
    if (error) {
      toast({ variant: "destructive", title: "Erreur", description: "Impossible de créer l'opération." });
      return;
    }
    toast({ title: "Opération ajoutée", description: `"${form.label}" enregistrée.` });
    setDialogOpen(false);
    setForm({ label: "", date: new Date().toISOString().split("T")[0], amount: "", operationType: "" });
    fetchOperations();
  }

  return (
    <div className="min-h-screen bg-background pb-12">
      <Navbar />
      <main className="container mx-auto px-4 py-8">
        <header className="mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="space-y-1">
            <h1 className="text-3xl font-bold text-primary">Comptabilité</h1>
            <p className="text-muted-foreground">Suivi des entrées et sorties de fonds.</p>
          </div>
          <Button className="bg-primary" onClick={() => setDialogOpen(true)}>
            <PlusCircle className="mr-2 h-4 w-4" />
            Opération
          </Button>
        </header>

        <div className="grid gap-6 mb-8 md:grid-cols-3">
          <Card className="bg-white">
            <CardHeader className="pb-2">
              <CardDescription className="flex items-center gap-1">
                <ArrowUpRight className="h-4 w-4 text-primary" />
                Total Revenus
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-primary">{totalIncome.toLocaleString()} FCFA</div>
            </CardContent>
          </Card>
          <Card className="bg-white">
            <CardHeader className="pb-2">
              <CardDescription className="flex items-center gap-1">
                <ArrowDownRight className="h-4 w-4 text-destructive" />
                Total Dépenses
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-destructive">{totalExpense.toLocaleString()} FCFA</div>
            </CardContent>
          </Card>
          <Card className="bg-primary text-primary-foreground">
            <CardHeader className="pb-2">
              <CardDescription className="text-primary-foreground/80">Bénéfice Net</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{netProfit.toLocaleString()} FCFA</div>
            </CardContent>
          </Card>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>Grand Livre</CardTitle>
            <CardDescription>Toutes les transactions financières</CardDescription>
          </CardHeader>
          <CardContent>
            {loading ? (
              <div className="flex items-center justify-center py-12 text-muted-foreground">
                <RefreshCcw className="mr-2 h-4 w-4 animate-spin" />
                Chargement...
              </div>
            ) : operations.length === 0 ? (
              <div className="text-center py-12 text-muted-foreground">
                <Layers className="mx-auto h-8 w-8 mb-2 opacity-40" />
                <p>Aucune opération enregistrée.</p>
                <Button variant="outline" className="mt-4" onClick={() => setDialogOpen(true)}>
                  Ajouter une opération
                </Button>
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Date</TableHead>
                    <TableHead>Libellé</TableHead>
                    <TableHead className="text-right">Montant</TableHead>
                    <TableHead className="text-right">Type</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {operations.map((op) => (
                    <TableRow key={op.id}>
                      <TableCell className="text-sm">
                        {new Date(op.date).toLocaleDateString("fr-FR")}
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <Layers className="h-3 w-3 text-muted-foreground" />
                          <span className="font-medium">{op.label}</span>
                        </div>
                      </TableCell>
                      <TableCell className="text-right font-bold">
                        {op.amount.toLocaleString()} F
                      </TableCell>
                      <TableCell className="text-right">
                        <Badge
                          variant={op.operationType === "INCOME" ? "default" : "destructive"}
                          className={op.operationType === "INCOME" ? "bg-primary" : ""}
                        >
                          {op.operationType === "INCOME" ? "Entrée" : "Sortie"}
                        </Badge>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>
      </main>

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Nouvelle Opération</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-2">
            <div className="space-y-2">
              <Label>Libellé</Label>
              <Input
                placeholder="Ex: Vente d'œufs, Achat aliments..."
                value={form.label}
                onChange={(e) => setForm({ ...form, label: e.target.value })}
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Date</Label>
                <Input
                  type="date"
                  value={form.date}
                  onChange={(e) => setForm({ ...form, date: e.target.value })}
                />
              </div>
              <div className="space-y-2">
                <Label>Montant (FCFA)</Label>
                <Input
                  type="number"
                  placeholder="Ex: 50000"
                  value={form.amount}
                  onChange={(e) => setForm({ ...form, amount: e.target.value })}
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label>Type</Label>
              <Select
                value={form.operationType}
                onValueChange={(v) => setForm({ ...form, operationType: v as "INCOME" | "EXPENSE" })}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Sélectionner le type" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="INCOME">Entrée (Revenu)</SelectItem>
                  <SelectItem value="EXPENSE">Sortie (Dépense)</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDialogOpen(false)}>
              Annuler
            </Button>
            <Button
              className="bg-primary"
              disabled={!form.label || !form.amount || !form.operationType || saving}
              onClick={handleCreate}
            >
              {saving ? <RefreshCcw className="mr-2 h-4 w-4 animate-spin" /> : null}
              Enregistrer
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
