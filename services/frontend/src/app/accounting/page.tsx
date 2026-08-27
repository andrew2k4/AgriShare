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
  Calendar,
  Download,
} from "lucide-react";
import { api } from "@/api/api";
import type { components } from "@/api/schema";
import { toast } from "@/hooks/use-toast";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";

type OperationDto = components["schemas"]["OperationDto"];

const CATEGORIES = {
  INCOME: ["Ventes œufs", "Ventes poules", "Autres revenus"],
  EXPENSE: ["Aliments", "Salaires", "Vaccins", "Matériel", "Transport", "Autres charges"]
};

export default function AccountingPage() {
  const [operations, setOperations] = useState<OperationDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({
    label: "",
    category: "",
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
    if (!form.category || !form.date || !form.amount || !form.operationType) return;
    setSaving(true);
    // On concatène la catégorie et le label pour le stockage API
    const fullLabel = form.label ? `${form.category}: ${form.label}` : form.category;
    const { error } = await api.operations.create({
      label: fullLabel,
      date: form.date,
      amount: Number(form.amount),
      operationType: form.operationType,
    });
    setSaving(false);
    if (error) {
      toast({ variant: "destructive", title: "Erreur", description: "Impossible de créer l'opération." });
      return;
    }
    toast({ title: "Opération ajoutée", description: `"${fullLabel}" enregistrée.` });
    setDialogOpen(false);
    setForm({ label: "", category: "", date: new Date().toISOString().split("T")[0], amount: "", operationType: "" });
    fetchOperations();
  }

  return (
    <div className="min-h-screen bg-background pb-12">
      <Navbar />
      <main className="container mx-auto px-4 py-8">
        <header className="mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="space-y-1">
            <h1 className="text-3xl font-bold text-primary">Comptabilité & Rubriques</h1>
            <p className="text-muted-foreground">Suivi financier organisé par catégories d'activité.</p>
          </div>
          <div className="flex gap-2">
            <Button variant="outline" className="border-primary text-primary hover:bg-primary/5">
              <Download className="mr-2 h-4 w-4" />
              Rapport
            </Button>
            <Button className="bg-primary" onClick={() => setDialogOpen(true)}>
              <PlusCircle className="mr-2 h-4 w-4" />
              Nouvelle opération
            </Button>
          </div>
        </header>

        <div className="grid gap-6 mb-8 md:grid-cols-3">
          <Card className="bg-white border-l-4 border-l-primary">
            <CardHeader className="pb-2">
              <CardDescription className="flex items-center gap-1">
                <ArrowUpRight className="h-4 w-4 text-primary" />
                Total Entrées
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-primary">{totalIncome.toLocaleString()} FCFA</div>
            </CardContent>
          </Card>
          <Card className="bg-white border-l-4 border-l-destructive">
            <CardHeader className="pb-2">
              <CardDescription className="flex items-center gap-1">
                <ArrowDownRight className="h-4 w-4 text-destructive" />
                Total Sorties
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-destructive">{totalExpense.toLocaleString()} FCFA</div>
            </CardContent>
          </Card>
          <Card className="bg-primary text-primary-foreground">
            <CardHeader className="pb-2">
              <CardDescription className="text-primary-foreground/80">Solde Net</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{netProfit.toLocaleString()} FCFA</div>
            </CardContent>
          </Card>
        </div>

        <Tabs defaultValue="all" className="space-y-6">
          <div className="flex items-center justify-between">
            <TabsList>
              <TabsTrigger value="all">Tout</TabsTrigger>
              <TabsTrigger value="income">Entrées</TabsTrigger>
              <TabsTrigger value="expense">Sorties</TabsTrigger>
            </TabsList>
          </div>

          <TabsContent value="all">
            <Card>
              <CardHeader>
                <CardTitle className="text-lg">Journal des Opérations</CardTitle>
                <CardDescription>Historique complet rubriqué</CardDescription>
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
                  </div>
                ) : (
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Date</TableHead>
                        <TableHead>Libellé</TableHead>
                        <TableHead className="text-right">Montant</TableHead>
                        <TableHead className="text-right">Status</TableHead>
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
                              <Badge variant="secondary" className="font-medium">
                                {op.label}
                              </Badge>
                            </div>
                          </TableCell>
                          <TableCell className={`text-right font-bold ${op.operationType === 'INCOME' ? 'text-primary' : 'text-destructive'}`}>
                            {op.operationType === 'INCOME' ? '+' : '-'}{op.amount.toLocaleString()} F
                          </TableCell>
                          <TableCell className="text-right">
                            <div className={`h-2 w-2 rounded-full ml-auto ${op.operationType === 'INCOME' ? 'bg-primary' : 'bg-destructive'}`} />
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                )}
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="income">
            <div className="grid gap-6 md:grid-cols-2">
              {CATEGORIES.INCOME.map(cat => {
                const total = operations
                  .filter(op => op.operationType === 'INCOME' && op.label.startsWith(cat))
                  .reduce((s, op) => s + op.amount, 0);
                return (
                  <Card key={cat}>
                    <CardHeader className="pb-2">
                      <CardTitle className="text-sm font-medium">{cat}</CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="text-xl font-bold text-primary">{total.toLocaleString()} FCFA</div>
                    </CardContent>
                  </Card>
                );
              })}
            </div>
          </TabsContent>

          <TabsContent value="expense">
             <div className="grid gap-6 md:grid-cols-3">
              {CATEGORIES.EXPENSE.map(cat => {
                const total = operations
                  .filter(op => op.operationType === 'EXPENSE' && op.label.startsWith(cat))
                  .reduce((s, op) => s + op.amount, 0);
                return (
                  <Card key={cat}>
                    <CardHeader className="pb-2">
                      <CardTitle className="text-sm font-medium">{cat}</CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="text-xl font-bold text-destructive">{total.toLocaleString()} FCFA</div>
                    </CardContent>
                  </Card>
                );
              })}
            </div>
          </TabsContent>
        </Tabs>
      </main>

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Enregistrer une opération</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label>Type d'opération</Label>
              <Select 
                value={form.operationType} 
                onValueChange={(v) => setForm({...form, operationType: v as any, category: ""})}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Choisir le type" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="INCOME">Entrée (Revenu)</SelectItem>
                  <SelectItem value="EXPENSE">Sortie (Charge)</SelectItem>
                </SelectContent>
              </Select>
            </div>
            {form.operationType && (
              <div className="space-y-2">
                <Label>Rubrique / Catégorie</Label>
                <Select 
                  value={form.category} 
                  onValueChange={(v) => setForm({...form, category: v})}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Choisir une rubrique" />
                  </SelectTrigger>
                  <SelectContent>
                    {CATEGORIES[form.operationType as keyof typeof CATEGORIES].map(cat => (
                      <SelectItem key={cat} value={cat}>{cat}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}
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
              <Label>Description / Détails</Label>
              <Input
                placeholder="Ex: Facture #123..."
                value={form.label}
                onChange={(e) => setForm({ ...form, label: e.target.value })}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDialogOpen(false)}>
              Annuler
            </Button>
            <Button
              className="bg-primary"
              disabled={!form.category || !form.amount || !form.operationType || saving}
              onClick={handleCreate}
            >
              {saving ? <RefreshCcw className="mr-2 h-4 w-4 animate-spin" /> : null}
              Valider l'opération
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
