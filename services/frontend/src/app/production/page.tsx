"use client";

import { useState, useEffect } from "react";
import { Navbar } from "@/components/layout/Navbar";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
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
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Activity,
  PlusCircle,
  TrendingUp,
  RefreshCcw,
  CalendarDays,
  BarChart3,
} from "lucide-react";
import { ProductionChart } from "@/components/dashboard/ProductionChart";
import { api } from "@/api/api";
import type { components } from "@/api/schema";
import { toast } from "@/hooks/use-toast";
import { FieldError } from "@/components/ui/field-error";
import { dateError, numberError, todayIso, useFormErrors } from "@/lib/validation";

type ProductionDayDto = components["schemas"]["ProductionDayDto"];

export default function ProductionPage() {
  const [productions, setProductions] = useState<ProductionDayDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({
    date: todayIso(),
    quantity: "",
  });
  const { errors, check, reset } = useFormErrors({
    date: dateError(form.date, { notInFuture: true }),
    // A day without production is a valid entry; INTEGER column
    quantity: numberError(form.quantity, { allowZero: true, integer: true, max: 1_000_000, empty: "Saisissez une quantité." }),
  });

  async function fetchProductions() {
    setLoading(true);
    const { data, error } = await api.productionDays.getAll();
    if (error || !data) {
      toast({ variant: "destructive", title: "Erreur", description: "Impossible de charger les données de production." });
    } else {
      setProductions(data);
    }
    setLoading(false);
  }

  useEffect(() => {
    fetchProductions();
  }, []);

  const sorted = [...productions].sort(
    (a, b) => new Date(b.date).getTime() - new Date(a.date).getTime()
  );
  const latest = sorted[0];
  const totalQuantity = productions.reduce((sum, p) => sum + p.quantity, 0);
  const avgQuantity = productions.length > 0
    ? Math.round(totalQuantity / productions.length)
    : 0;

  async function handleCreate() {
    if (!check()) return;
    setSaving(true);
    const { error } = await api.productionDays.create({
      date: form.date,
      quantity: Number(form.quantity),
    });
    setSaving(false);
    if (error) {
      toast({ variant: "destructive", title: "Erreur", description: "Impossible d'enregistrer la saisie." });
      return;
    }
    toast({ title: "Saisie enregistrée", description: `${form.quantity} unités le ${new Date(form.date).toLocaleDateString("fr-FR")}.` });
    setDialogOpen(false);
    setForm({ date: todayIso(), quantity: "" });
    reset();
    fetchProductions();
  }

  return (
    <div className="min-h-screen bg-background pb-12">
      <Navbar />
      <main className="container mx-auto px-4 py-8">
        <header className="mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="space-y-1">
            <h1 className="text-3xl font-bold text-primary">Suivi de Production</h1>
            <p className="text-muted-foreground">Données journalières de production.</p>
          </div>
          <Button className="bg-primary" onClick={() => setDialogOpen(true)}>
            <PlusCircle className="mr-2 h-4 w-4" />
            Saisie du Jour
          </Button>
        </header>

        <div className="grid gap-6 md:grid-cols-3 mb-8">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardDescription>Dernière Production</CardDescription>
              <Activity className="h-4 w-4 text-primary" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">
                {latest ? `${latest.quantity} unités` : "—"}
              </div>
              {latest && (
                <p className="text-xs text-muted-foreground mt-1">
                  {new Date(latest.date).toLocaleDateString("fr-FR")}
                </p>
              )}
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardDescription>Moyenne Journalière</CardDescription>
              <TrendingUp className="h-4 w-4 text-primary" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{avgQuantity} unités</div>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardDescription>Total Enregistrements</CardDescription>
              <CalendarDays className="h-4 w-4 text-primary" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{productions.length} jours</div>
            </CardContent>
          </Card>
        </div>

        {loading ? (
          <div className="flex items-center justify-center py-20 text-muted-foreground">
            <RefreshCcw className="mr-2 h-4 w-4 animate-spin" />
            Chargement...
          </div>
        ) : productions.length === 0 ? (
          <div className="text-center py-20 text-muted-foreground">
            <BarChart3 className="mx-auto h-10 w-10 mb-3 opacity-40" />
            <p>Aucune donnée de production enregistrée.</p>
            <Button variant="outline" className="mt-4" onClick={() => setDialogOpen(true)}>
              Faire la première saisie
            </Button>
          </div>
        ) : (
          <div className="space-y-6">
            <ProductionChart data={sorted} />

            <Card>
              <CardHeader>
                <CardTitle>Registre Historique</CardTitle>
              </CardHeader>
              <CardContent>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Date</TableHead>
                      <TableHead className="text-right">Quantité produite</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {sorted.map((record) => (
                      <TableRow key={record.id}>
                        <TableCell>
                          {new Date(record.date).toLocaleDateString("fr-FR")}
                        </TableCell>
                        <TableCell className="text-right font-medium">
                          {record.quantity}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          </div>
        )}
      </main>

      <Dialog open={dialogOpen} onOpenChange={(open) => { setDialogOpen(open); if (!open) reset(); }}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Saisie Journalière</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-2">
            <div className="space-y-2">
              <Label>Date</Label>
              <Input
                type="date"
                max={todayIso()}
                value={form.date}
                aria-invalid={!!errors.date}
                onChange={(e) => setForm({ ...form, date: e.target.value })}
              />
              <FieldError message={errors.date} />
            </div>
            <div className="space-y-2">
              <Label>Quantité produite</Label>
              <Input
                type="number"
                min={0}
                step={1}
                placeholder="Ex: 450"
                value={form.quantity}
                aria-invalid={!!errors.quantity}
                onChange={(e) => setForm({ ...form, quantity: e.target.value })}
              />
              <FieldError message={errors.quantity} />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDialogOpen(false)}>
              Annuler
            </Button>
            <Button
              className="bg-primary"
              disabled={saving}
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
