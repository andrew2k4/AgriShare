"use client";

import { useEffect, useState } from "react";
import { Navbar } from "@/components/layout/Navbar";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { HandCoins, History, TrendingUp, RefreshCcw, Check, X, LineChart, Gauge } from "lucide-react";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { api } from "@/api";
import type { components } from "@/api";
import { toast } from "@/hooks/use-toast";

type ProjectDto = components["schemas"]["ProjectDto"];
type InvestorDto = components["schemas"]["InvestorDto"];
type ProfitDistributionDto = components["schemas"]["ProfitDistributionDto"];
type ProjectFinancialSummaryDto = components["schemas"]["ProjectFinancialSummaryDto"];
type RoiDto = components["schemas"]["RoiDto"];

const STATUS_LABELS: Record<string, string> = {
  PROPOSED: "En attente de validation",
  VALIDATED: "Validée",
  REJECTED: "Rejetée",
};

export default function ProfitsPage() {
  const [projects, setProjects] = useState<ProjectDto[]>([]);
  const [investors, setInvestors] = useState<InvestorDto[]>([]);
  const [selectedProjectId, setSelectedProjectId] = useState<string>("");
  const [distributions, setDistributions] = useState<ProfitDistributionDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [role, setRole] = useState<string | null>(null);
  const [proposing, setProposing] = useState(false);
  const [actingOnId, setActingOnId] = useState<number | null>(null);

  const today = new Date().toISOString().slice(0, 10);
  const [form, setForm] = useState({
    periodStart: today,
    periodEnd: today,
    taxes: "0",
    reserve: "0",
    reinvestment: "0",
  });

  const canManageDistributions = role === "ADMIN" || role === "MANAGER";

  const [summary, setSummary] = useState<ProjectFinancialSummaryDto | null>(null);
  const [roiInvestorId, setRoiInvestorId] = useState<string>("");
  const [roi, setRoi] = useState<RoiDto | null>(null);
  const [roiLoading, setRoiLoading] = useState(false);

  const fetchProjects = async () => {
    const { data } = await api.projects.getAll();
    if (data) setProjects(data);
  };

  const fetchInvestors = async () => {
    const { data } = await api.investors.getAll();
    if (data) setInvestors(data);
  };

  const fetchRole = async () => {
    const { data } = await api.auth.me();
    if (data && "role" in data) setRole(String(data.role));
  };

  const fetchDistributions = async (projectId: string) => {
    if (!projectId) {
      setDistributions([]);
      return;
    }
    setLoading(true);
    const { data, error } = await api.distributions.getByProject(Number(projectId));
    setLoading(false);
    if (error) {
      toast({ variant: "destructive", title: "Erreur", description: "Impossible de charger les distributions." });
      return;
    }
    setDistributions(data ?? []);
  };

  useEffect(() => {
    fetchProjects();
    fetchInvestors();
    fetchRole();
  }, []);

  const fetchSummary = async (projectId: string) => {
    if (!projectId) {
      setSummary(null);
      return;
    }
    const { data, error } = await api.projects.financialSummary(Number(projectId));
    if (error) {
      toast({ variant: "destructive", title: "Erreur", description: "Impossible de charger la vue financière." });
      return;
    }
    setSummary(data ?? null);
  };

  useEffect(() => {
    fetchDistributions(selectedProjectId);
    fetchSummary(selectedProjectId);
    setRoi(null);
    setRoiInvestorId("");
  }, [selectedProjectId]);

  const investorName = (investorId: number) =>
    investors.find((i) => i.id === investorId)?.fullName ?? `Investisseur #${investorId}`;

  const handleLookupRoi = async () => {
    if (!selectedProjectId || !roiInvestorId) return;
    setRoiLoading(true);
    const { data, error } = await api.projects.roi(Number(selectedProjectId), Number(roiInvestorId));
    setRoiLoading(false);
    if (error) {
      toast({
        variant: "destructive",
        title: "Erreur",
        description: "Aucun apport en capital (associé/apport en nature) trouvé pour cet investisseur sur ce projet.",
      });
      setRoi(null);
      return;
    }
    setRoi(data ?? null);
  };

  const handlePropose = async () => {
    if (!selectedProjectId) return;
    setProposing(true);
    const { error } = await api.distributions.propose(Number(selectedProjectId), {
      periodStart: form.periodStart,
      periodEnd: form.periodEnd,
      taxes: Number(form.taxes),
      reserve: Number(form.reserve),
      reinvestment: Number(form.reinvestment),
    });
    setProposing(false);
    if (error) {
      toast({
        variant: "destructive",
        title: "Erreur",
        description: "Impossible de proposer cette distribution (vérifie tes droits et les dates).",
      });
      return;
    }
    toast({ title: "Distribution proposée", description: "En attente de validation par un second responsable." });
    fetchDistributions(selectedProjectId);
  };

  const handleValidate = async (id: number) => {
    setActingOnId(id);
    const { error } = await api.distributions.validate(id);
    setActingOnId(null);
    if (error) {
      toast({
        variant: "destructive",
        title: "Validation impossible",
        description: "Un responsable différent de celui qui a proposé la distribution doit la valider.",
      });
      return;
    }
    toast({ title: "Distribution validée" });
    fetchDistributions(selectedProjectId);
  };

  const handleReject = async (id: number) => {
    setActingOnId(id);
    const { error } = await api.distributions.reject(id);
    setActingOnId(null);
    if (error) {
      toast({ variant: "destructive", title: "Erreur", description: "Impossible de rejeter cette distribution." });
      return;
    }
    toast({ title: "Distribution rejetée" });
    fetchDistributions(selectedProjectId);
  };

  return (
    <div className="min-h-screen bg-background pb-12">
      <Navbar />
      <main className="container mx-auto px-4 py-8">
        <header className="mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="space-y-1">
            <h1 className="text-3xl font-bold text-primary">Distribution des Bénéfices</h1>
            <p className="text-muted-foreground">
              Calcul automatique basé sur les unités de contribution de chaque investisseur.
            </p>
          </div>
          <div className="w-full md:w-64">
            <Select value={selectedProjectId} onValueChange={setSelectedProjectId}>
              <SelectTrigger>
                <SelectValue placeholder="Choisir un projet" />
              </SelectTrigger>
              <SelectContent>
                {projects.map((p) => (
                  <SelectItem key={p.id} value={String(p.id)}>
                    {p.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </header>

        {!selectedProjectId ? (
          <Card>
            <CardContent className="py-12 text-center text-muted-foreground">
              Choisis un projet pour consulter ou proposer une distribution de bénéfices.
            </CardContent>
          </Card>
        ) : (
          <>
            {summary && (
              <Card className="mb-8">
                <CardHeader>
                  <CardTitle className="text-lg flex items-center gap-2">
                    <Gauge className="h-5 w-5 text-primary" />
                    Vue financière du projet
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                    <div className="p-3 rounded-lg border bg-primary/5">
                      <div className="text-xs text-muted-foreground uppercase tracking-wide">Valeur du projet</div>
                      <div className="text-lg font-bold">{summary.currentProjectValue?.toLocaleString()} FCFA</div>
                    </div>
                    <div className="p-3 rounded-lg border">
                      <div className="text-xs text-muted-foreground uppercase tracking-wide">Capital investi (cumulé)</div>
                      <div className="text-lg font-bold">{summary.totalCapitalInvested?.toLocaleString()} FCFA</div>
                    </div>
                    <div className="p-3 rounded-lg border">
                      <div className="text-xs text-muted-foreground uppercase tracking-wide">Financement temporaire restant</div>
                      <div className="text-lg font-bold">{summary.remainingTemporaryCapital?.toLocaleString()} FCFA</div>
                    </div>
                    <div className="p-3 rounded-lg border">
                      <div className="text-xs text-muted-foreground uppercase tracking-wide">Actifs loués (propriété tiers)</div>
                      <div className="text-lg font-bold">{summary.assetsValueOwnedByContributors?.toLocaleString()} FCFA</div>
                    </div>
                    <div className="p-3 rounded-lg border">
                      <div className="text-xs text-muted-foreground uppercase tracking-wide">Loyers mensuels actuels</div>
                      <div className="text-lg font-bold">{summary.currentMonthlyRent?.toLocaleString()} FCFA</div>
                    </div>
                    <div className="p-3 rounded-lg border">
                      <div className="text-xs text-muted-foreground uppercase tracking-wide">Chiffre d'affaires (total)</div>
                      <div className="text-lg font-bold">{summary.totalRevenue?.toLocaleString()} FCFA</div>
                    </div>
                    <div className="p-3 rounded-lg border">
                      <div className="text-xs text-muted-foreground uppercase tracking-wide">Dépenses (total)</div>
                      <div className="text-lg font-bold">{summary.totalExpenses?.toLocaleString()} FCFA</div>
                    </div>
                    <div className="p-3 rounded-lg border bg-primary/5">
                      <div className="text-xs text-muted-foreground uppercase tracking-wide">Bénéfice/perte (total)</div>
                      <div className="text-lg font-bold">{summary.netProfitAllTime?.toLocaleString()} FCFA</div>
                    </div>
                    <div className="p-3 rounded-lg border">
                      <div className="text-xs text-muted-foreground uppercase tracking-wide">Réserve cumulée</div>
                      <div className="text-lg font-bold">{summary.totalReserveAccumulated?.toLocaleString()} FCFA</div>
                    </div>
                    <div className="p-3 rounded-lg border">
                      <div className="text-xs text-muted-foreground uppercase tracking-wide">Déjà distribué</div>
                      <div className="text-lg font-bold">{summary.totalDistributedToDate?.toLocaleString()} FCFA</div>
                    </div>
                  </div>

                  {summary.currentShares && summary.currentShares.length > 0 && (
                    <div className="mt-6">
                      <h3 className="text-sm font-semibold text-muted-foreground mb-2">Parts actuelles</h3>
                      <div className="flex flex-wrap gap-2">
                        {summary.currentShares.map((s) => (
                          <Badge key={s.investorId} variant="outline" className="bg-primary/5 text-primary">
                            {investorName(s.investorId ?? 0)} — {s.sharePercentage}%
                          </Badge>
                        ))}
                      </div>
                    </div>
                  )}

                  <div className="mt-6 pt-4 border-t">
                    <h3 className="text-sm font-semibold text-muted-foreground mb-2 flex items-center gap-2">
                      <LineChart className="h-4 w-4" />
                      ROI par investisseur
                    </h3>
                    <div className="flex flex-wrap items-end gap-3">
                      <div className="w-56 space-y-2">
                        <Label>Investisseur</Label>
                        <Select value={roiInvestorId} onValueChange={setRoiInvestorId}>
                          <SelectTrigger>
                            <SelectValue placeholder="Choisir un investisseur" />
                          </SelectTrigger>
                          <SelectContent>
                            {investors.map((inv) => (
                              <SelectItem key={inv.id} value={String(inv.id)}>
                                {inv.fullName}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </div>
                      <Button variant="outline" disabled={!roiInvestorId || roiLoading} onClick={handleLookupRoi}>
                        {roiLoading && <RefreshCcw className="mr-2 h-4 w-4 animate-spin" />}
                        Calculer le ROI
                      </Button>
                      {roi && (
                        <div className="flex gap-4 text-sm">
                          <div>
                            <span className="text-muted-foreground">ROI : </span>
                            <span className="font-bold">{roi.roiPercent}%</span>
                          </div>
                          <div>
                            <span className="text-muted-foreground">ROI annualisé : </span>
                            <span className="font-bold">{roi.annualizedRoiPercent}%</span>
                          </div>
                          <div>
                            <span className="text-muted-foreground">Depuis : </span>
                            <span className="font-bold">{roi.daysInvested} jours</span>
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                </CardContent>
              </Card>
            )}

            {canManageDistributions && (
              <Card className="mb-8">
                <CardHeader>
                  <CardTitle className="text-lg flex items-center gap-2">
                    <HandCoins className="h-5 w-5 text-primary" />
                    Proposer une distribution
                  </CardTitle>
                  <CardDescription>
                    Le bénéfice distribuable est calculé automatiquement à partir des opérations de la période
                    (revenus − charges − taxes − réserve − réinvestissement), et réparti selon les unités de
                    contribution de chaque investisseur.
                  </CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="grid gap-4 md:grid-cols-5">
                    <div className="space-y-2">
                      <Label>Début de période</Label>
                      <Input
                        type="date"
                        value={form.periodStart}
                        onChange={(e) => setForm({ ...form, periodStart: e.target.value })}
                      />
                    </div>
                    <div className="space-y-2">
                      <Label>Fin de période</Label>
                      <Input
                        type="date"
                        value={form.periodEnd}
                        onChange={(e) => setForm({ ...form, periodEnd: e.target.value })}
                      />
                    </div>
                    <div className="space-y-2">
                      <Label>Taxes</Label>
                      <Input
                        type="number"
                        value={form.taxes}
                        onChange={(e) => setForm({ ...form, taxes: e.target.value })}
                      />
                    </div>
                    <div className="space-y-2">
                      <Label>Réserve</Label>
                      <Input
                        type="number"
                        value={form.reserve}
                        onChange={(e) => setForm({ ...form, reserve: e.target.value })}
                      />
                    </div>
                    <div className="space-y-2">
                      <Label>Réinvestissement</Label>
                      <Input
                        type="number"
                        value={form.reinvestment}
                        onChange={(e) => setForm({ ...form, reinvestment: e.target.value })}
                      />
                    </div>
                  </div>
                  <Button className="bg-primary" disabled={proposing} onClick={handlePropose}>
                    {proposing && <RefreshCcw className="mr-2 h-4 w-4 animate-spin" />}
                    Proposer la distribution
                  </Button>
                </CardContent>
              </Card>
            )}

            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <History className="h-5 w-5 text-primary" />
                  Historique des distributions
                </CardTitle>
                <CardDescription>
                  Chaque distribution doit être validée par un responsable différent de celui qui l'a proposée.
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-6">
                {loading ? (
                  <p className="text-center text-muted-foreground py-8">Chargement...</p>
                ) : distributions.length === 0 ? (
                  <p className="text-center text-muted-foreground py-8">
                    Aucune distribution proposée pour ce projet.
                  </p>
                ) : (
                  distributions.map((d) => (
                    <div key={d.id} className="rounded-lg border p-4 space-y-3">
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <div>
                          <div className="font-medium flex items-center gap-2">
                            <TrendingUp className="h-4 w-4 text-primary" />
                            {d.periodStart} → {d.periodEnd}
                          </div>
                          <div className="text-sm text-muted-foreground">
                            Revenus {(d.revenue ?? 0).toLocaleString()} FCFA · Charges{" "}
                            {(d.operatingCosts ?? 0).toLocaleString()} FCFA
                          </div>
                        </div>
                        <div className="flex items-center gap-3">
                          <Badge
                            variant="outline"
                            className={
                              d.status === "VALIDATED"
                                ? "bg-primary/10 text-primary"
                                : d.status === "REJECTED"
                                ? "bg-destructive/10 text-destructive"
                                : "bg-accent/10 text-accent-foreground"
                            }
                          >
                            {STATUS_LABELS[d.status ?? ""] ?? d.status}
                          </Badge>
                          <span className="text-lg font-bold text-primary">
                            {(d.distributableProfit ?? 0).toLocaleString()} FCFA
                          </span>
                        </div>
                      </div>

                      {d.status === "PROPOSED" && canManageDistributions && (
                        <div className="flex gap-2">
                          <Button
                            size="sm"
                            className="bg-primary"
                            disabled={actingOnId === d.id}
                            onClick={() => d.id && handleValidate(d.id)}
                          >
                            <Check className="mr-1 h-4 w-4" />
                            Valider
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            disabled={actingOnId === d.id}
                            onClick={() => d.id && handleReject(d.id)}
                          >
                            <X className="mr-1 h-4 w-4" />
                            Rejeter
                          </Button>
                        </div>
                      )}

                      <Table>
                        <TableHeader>
                          <TableRow>
                            <TableHead>Investisseur</TableHead>
                            <TableHead className="text-right">Part</TableHead>
                            <TableHead className="text-right">Montant</TableHead>
                          </TableRow>
                        </TableHeader>
                        <TableBody>
                          {(d.lines ?? []).length === 0 ? (
                            <TableRow>
                              <TableCell colSpan={3} className="text-center text-muted-foreground">
                                Aucun investisseur actif sur ce projet.
                              </TableCell>
                            </TableRow>
                          ) : (
                            (d.lines ?? []).map((line, idx) => (
                              <TableRow key={idx}>
                                <TableCell>{investorName(line.investorId ?? 0)}</TableCell>
                                <TableCell className="text-right">{line.sharePercentage}%</TableCell>
                                <TableCell className="text-right font-bold">
                                  {(line.amount ?? 0).toLocaleString()} FCFA
                                </TableCell>
                              </TableRow>
                            ))
                          )}
                        </TableBody>
                      </Table>
                    </div>
                  ))
                )}
              </CardContent>
            </Card>
          </>
        )}
      </main>
    </div>
  );
}
