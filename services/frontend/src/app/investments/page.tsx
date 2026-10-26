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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { PlusCircle, Wallet, Users, History, RefreshCcw, Edit2, Landmark, ArrowDownCircle, Check, X, Info } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { api } from "@/api";
import type { components } from "@/api";
import { toast } from "@/hooks/use-toast";

type InvestorDto = components["schemas"]["InvestorDto"];
type ProjectDto = components["schemas"]["ProjectDto"];
type ContributionDto = components["schemas"]["ContributionDto"];
type WithdrawalDto = components["schemas"]["WithdrawalDto"];
type AssetDto = components["schemas"]["AssetDto"];
type InvestorShareDto = components["schemas"]["InvestorShareDto"];

const WITHDRAWAL_STATUS_LABELS: Record<string, string> = {
  REQUESTED: "En attente de validation",
  VALIDATED: "Validé",
  REJECTED: "Rejeté",
  BLOCKED: "Bloqué",
};

const CONTRIBUTION_TYPE_LABELS: Record<string, string> = {
  EQUITY: "Associé (numéraire)",
  IN_KIND: "Apport en nature",
  LOAN: "Prêt",
  TEMPORARY_INVESTMENT: "Financement temporaire",
  LABOR: "Travail",
};

export default function InvestmentsPage() {
  const [investors, setInvestors] = useState<InvestorDto[]>([]);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingInvestor, setEditingInvestor] = useState<InvestorDto | null>(null);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({ fullName: "", contribution: "" });

  const [projects, setProjects] = useState<ProjectDto[]>([]);
  const [selectedProjectId, setSelectedProjectId] = useState<string>("");
  const [contributions, setContributions] = useState<ContributionDto[]>([]);
  const [loadingContributions, setLoadingContributions] = useState(false);
  const [contributionSaving, setContributionSaving] = useState(false);
  const [contributionForm, setContributionForm] = useState({
    investorId: "",
    type: "EQUITY",
    amount: "",
    entryDate: new Date().toISOString().slice(0, 10),
    assetId: "",
  });
  const [leasableAssets, setLeasableAssets] = useState<AssetDto[]>([]);
  const [currentShares, setCurrentShares] = useState<InvestorShareDto[]>([]);

  const [role, setRole] = useState<string | null>(null);
  const [withdrawals, setWithdrawals] = useState<WithdrawalDto[]>([]);
  const [withdrawalDialogOpen, setWithdrawalDialogOpen] = useState(false);
  const [withdrawalContributionId, setWithdrawalContributionId] = useState<number | null>(null);
  const [withdrawalAmount, setWithdrawalAmount] = useState("");
  const [withdrawalSaving, setWithdrawalSaving] = useState(false);
  const [actingOnWithdrawalId, setActingOnWithdrawalId] = useState<number | null>(null);

  const canManageWithdrawals = role === "ADMIN" || role === "MANAGER";

  const fetchInvestors = async () => {
    const { data } = await api.investors.getAll();
    if (data) setInvestors(data);
  };

  const fetchProjects = async () => {
    const { data } = await api.projects.getAll();
    if (data) setProjects(data);
  };

  const fetchContributions = async (projectId: string) => {
    if (!projectId) {
      setContributions([]);
      return;
    }
    setLoadingContributions(true);
    const { data, error } = await api.contributions.getByProject(Number(projectId));
    setLoadingContributions(false);
    if (error) {
      toast({
        variant: "destructive",
        title: "Erreur",
        description: "Impossible de charger les apports de ce projet.",
      });
      return;
    }
    setContributions(data ?? []);
  };

  const fetchCurrentShares = async (projectId: string) => {
    if (!projectId) {
      setCurrentShares([]);
      return;
    }
    const { data } = await api.projects.financialSummary(Number(projectId));
    setCurrentShares(data?.currentShares ?? []);
  };

  const fetchRole = async () => {
    const { data } = await api.auth.me();
    if (data && "role" in data) setRole(String(data.role));
  };

  const fetchWithdrawals = async (projectId: string) => {
    if (!projectId) {
      setWithdrawals([]);
      return;
    }
    const { data, error } = await api.withdrawals.getByProject(Number(projectId));
    if (error) {
      toast({ variant: "destructive", title: "Erreur", description: "Impossible de charger les retraits." });
      return;
    }
    setWithdrawals(data ?? []);
  };

  useEffect(() => {
    fetchInvestors();
    fetchProjects();
    fetchRole();
  }, []);

  useEffect(() => {
    fetchContributions(selectedProjectId);
    fetchWithdrawals(selectedProjectId);
    fetchCurrentShares(selectedProjectId);
  }, [selectedProjectId]);

  useEffect(() => {
    const loadLeasableAssets = async () => {
      if (!selectedProjectId || contributionForm.type !== "IN_KIND" || !contributionForm.investorId) {
        setLeasableAssets([]);
        return;
      }
      const { data } = await api.assets.getByProject(Number(selectedProjectId));
      setLeasableAssets(
        (data ?? []).filter(
          (a) => a.ownerInvestorId === Number(contributionForm.investorId) && a.ownershipStatus === "OWNED_BY_CONTRIBUTOR"
        )
      );
    };
    loadLeasableAssets();
  }, [selectedProjectId, contributionForm.type, contributionForm.investorId]);

  const handleCreateContribution = async () => {
    if (!selectedProjectId || !contributionForm.investorId || !contributionForm.amount) return;
    if (contributionForm.type === "IN_KIND" && !contributionForm.assetId) return;
    setContributionSaving(true);
    const { data, error } = await api.contributions.create(Number(selectedProjectId), {
      investorId: Number(contributionForm.investorId),
      type: contributionForm.type as ContributionDto["type"],
      amount: Number(contributionForm.amount),
      entryDate: contributionForm.entryDate,
      assetId: contributionForm.type === "IN_KIND" ? Number(contributionForm.assetId) : undefined,
    });
    setContributionSaving(false);
    if (error) {
      toast({
        variant: "destructive",
        title: "Erreur",
        description: "Impossible d'enregistrer l'apport.",
      });
      return;
    }
    const isEquityLike = contributionForm.type === "EQUITY" || contributionForm.type === "IN_KIND";
    toast({
      title: "Apport enregistré",
      description: isEquityLike
        ? `Part obtenue à l'entrée : ${data?.sharePercentageAtEntry}%.`
        : "Enregistré — à suivre via le remboursement (prêt/financement temporaire).",
    });
    setContributionForm({
      investorId: "", type: "EQUITY", amount: "", entryDate: new Date().toISOString().slice(0, 10), assetId: "",
    });
    fetchContributions(selectedProjectId);
    fetchCurrentShares(selectedProjectId);
  };

  const investorName = (investorId: number) =>
    investors.find((i) => i.id === investorId)?.fullName ?? `Investisseur #${investorId}`;

  const currentShareFor = (investorId: number): string | null => {
    const share = currentShares.find((s) => s.investorId === investorId);
    return share?.sharePercentage != null ? String(share.sharePercentage) : null;
  };

  const openWithdrawalDialog = (contributionId: number) => {
    setWithdrawalContributionId(contributionId);
    setWithdrawalAmount("");
    setWithdrawalDialogOpen(true);
  };

  const handleRequestWithdrawal = async () => {
    if (!withdrawalContributionId || !withdrawalAmount) return;
    setWithdrawalSaving(true);
    const { data, error } = await api.withdrawals.request(withdrawalContributionId, {
      amount: Number(withdrawalAmount),
    });
    setWithdrawalSaving(false);
    if (error) {
      toast({ variant: "destructive", title: "Erreur", description: "Impossible de demander ce retrait." });
      return;
    }
    if (data?.status === "BLOCKED") {
      toast({
        variant: "destructive",
        title: "Retrait bloqué",
        description: data.blockReason ?? "Le projet ne peut pas absorber ce retrait pour le moment.",
      });
    } else {
      toast({ title: "Retrait demandé", description: "En attente de validation par un second responsable." });
    }
    setWithdrawalDialogOpen(false);
    fetchWithdrawals(selectedProjectId);
  };

  const handleValidateWithdrawal = async (id: number) => {
    setActingOnWithdrawalId(id);
    const { error } = await api.withdrawals.validate(id);
    setActingOnWithdrawalId(null);
    if (error) {
      toast({
        variant: "destructive",
        title: "Validation impossible",
        description: "Un responsable différent de celui qui a demandé le retrait doit le valider.",
      });
      return;
    }
    toast({ title: "Retrait validé" });
    fetchWithdrawals(selectedProjectId);
    fetchContributions(selectedProjectId);
    fetchCurrentShares(selectedProjectId);
  };

  const handleRejectWithdrawal = async (id: number) => {
    setActingOnWithdrawalId(id);
    const { error } = await api.withdrawals.reject(id);
    setActingOnWithdrawalId(null);
    if (error) {
      toast({ variant: "destructive", title: "Erreur", description: "Impossible de rejeter ce retrait." });
      return;
    }
    toast({ title: "Retrait rejeté" });
    fetchWithdrawals(selectedProjectId);
  };

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

  const handleUpdate = async () => {
    if (!editingInvestor || !form.fullName || !form.contribution) return;
    setSaving(true);
    const { error } = await api.investors.update(editingInvestor.id!, {
      fullName: form.fullName,
      contribution: Number(form.contribution),
    });
    setSaving(false);
    if (error) {
      toast({
        variant: "destructive",
        title: "Erreur",
        description: "Impossible de modifier l'investissement.",
      });
      return;
    }
    toast({
      title: "Investissement mis à jour",
      description: `L'apport de ${form.fullName} a été modifié.`,
    });
    setEditingInvestor(null);
    setForm({ fullName: "", contribution: "" });
    fetchInvestors();
  };

  const openEdit = (inv: InvestorDto) => {
    setEditingInvestor(inv);
    setForm({ fullName: inv.fullName, contribution: inv.contribution.toString() });
  };

  return (
    <div className="min-h-screen bg-background pb-12">
      <Navbar />
      <main className="container mx-auto px-4 py-8">
        <header className="mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="space-y-1">
            <h1 className="text-3xl font-bold text-primary">Gestion des Investissements</h1>
            <p className="text-muted-foreground">Suivez les capitaux et gérez les parts dynamiques.</p>
          </div>
          <Button
            className="w-full md:w-auto bg-primary"
            onClick={() => { setForm({fullName: "", contribution: ""}); setDialogOpen(true); }}
          >
            <PlusCircle className="mr-2 h-4 w-4" />
            Nouvel Investissement
          </Button>
        </header>

        <Card className="mb-8">
          <CardHeader>
            <CardTitle className="text-lg flex items-center gap-2">
              <Landmark className="h-5 w-5 text-primary" />
              Apports au capital par projet
            </CardTitle>
            <CardDescription>
              Enregistre un apport en numéraire et calcule la part obtenue selon la valorisation du projet à l'entrée.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid gap-4 md:grid-cols-5">
              <div className="space-y-2 md:col-span-1">
                <Label>Projet</Label>
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
              <div className="space-y-2 md:col-span-1">
                <Label>Investisseur</Label>
                <Select
                  value={contributionForm.investorId}
                  onValueChange={(v) => setContributionForm({ ...contributionForm, investorId: v })}
                >
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
              <div className="space-y-2 md:col-span-1">
                <Label>Type</Label>
                <Select
                  value={contributionForm.type}
                  onValueChange={(v) => setContributionForm({ ...contributionForm, type: v, assetId: "" })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {Object.entries(CONTRIBUTION_TYPE_LABELS).map(([value, label]) => (
                      <SelectItem key={value} value={value}>
                        {label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2 md:col-span-1">
                <Label>Montant (FCFA)</Label>
                <Input
                  type="number"
                  placeholder="Ex: 2000000"
                  value={contributionForm.amount}
                  onChange={(e) => setContributionForm({ ...contributionForm, amount: e.target.value })}
                />
              </div>
              <div className="space-y-2 md:col-span-1">
                <Label>Date</Label>
                <Input
                  type="date"
                  value={contributionForm.entryDate}
                  onChange={(e) => setContributionForm({ ...contributionForm, entryDate: e.target.value })}
                />
              </div>
            </div>

            {contributionForm.type === "IN_KIND" && (
              <div className="space-y-2 md:w-1/3">
                <Label>Actif à apporter</Label>
                <Select
                  value={contributionForm.assetId}
                  onValueChange={(v) => setContributionForm({ ...contributionForm, assetId: v })}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Choisir un actif de cet investisseur" />
                  </SelectTrigger>
                  <SelectContent>
                    {leasableAssets.length === 0 ? (
                      <div className="px-2 py-1.5 text-sm text-muted-foreground">
                        Aucun actif disponible pour cet investisseur sur ce projet.
                      </div>
                    ) : (
                      leasableAssets.map((a) => (
                        <SelectItem key={a.id} value={String(a.id)}>
                          {a.description || a.type} ({(a.initialValue ?? 0).toLocaleString()} FCFA)
                        </SelectItem>
                      ))
                    )}
                  </SelectContent>
                </Select>
                <p className="text-xs text-muted-foreground">
                  Créez d'abord l'actif depuis la page{" "}
                  <a href="/assets" className="underline">
                    Actifs &amp; Baux
                  </a>
                  .
                </p>
              </div>
            )}
            <Button
              className="bg-primary"
              disabled={
                !selectedProjectId ||
                !contributionForm.investorId ||
                !contributionForm.amount ||
                (contributionForm.type === "IN_KIND" && !contributionForm.assetId) ||
                contributionSaving
              }
              onClick={handleCreateContribution}
            >
              {contributionSaving && <RefreshCcw className="mr-2 h-4 w-4 animate-spin" />}
              Enregistrer l'apport
            </Button>

            {selectedProjectId && (
              <div className="pt-2">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Investisseur</TableHead>
                      <TableHead className="text-right">Montant</TableHead>
                      <TableHead>Date</TableHead>
                      <TableHead className="text-right">Valorisation avant entrée</TableHead>
                      <TableHead className="text-right">
                        <span className="inline-flex items-center justify-end gap-1">
                          Part obtenue à l&apos;entrée
                          <span
                            title="Pourcentage du capital que cet apport a acheté au moment où il a été versé. C'est un instantané historique : la somme des lignes ne fait pas 100 % dès qu'il y a plusieurs apports à des dates différentes. Pour la répartition actuelle, voir la colonne « Part actuelle »."
                          >
                            <Info className="h-3.5 w-3.5 text-muted-foreground" />
                          </span>
                        </span>
                      </TableHead>
                      <TableHead className="text-right">Part actuelle</TableHead>
                      {canManageWithdrawals && <TableHead className="text-right">Action</TableHead>}
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {loadingContributions ? (
                      <TableRow>
                        <TableCell colSpan={7} className="text-center text-muted-foreground py-6">
                          Chargement...
                        </TableCell>
                      </TableRow>
                    ) : contributions.length === 0 ? (
                      <TableRow>
                        <TableCell colSpan={7} className="text-center text-muted-foreground py-6">
                          Aucun apport enregistré pour ce projet.
                        </TableCell>
                      </TableRow>
                    ) : (
                      contributions.map((c) => (
                        <TableRow key={c.id}>
                          <TableCell className="font-medium">{investorName(c.investorId)}</TableCell>
                          <TableCell className="text-right">{c.amount.toLocaleString()} FCFA</TableCell>
                          <TableCell>{c.entryDate}</TableCell>
                          <TableCell className="text-right">{(c.valuationBeforeEntry ?? 0).toLocaleString()} FCFA</TableCell>
                          <TableCell className="text-right">
                            <Badge variant="outline" className="bg-muted text-muted-foreground">
                              {c.sharePercentageAtEntry}%
                            </Badge>
                          </TableCell>
                          <TableCell className="text-right">
                            {currentShareFor(c.investorId) != null ? (
                              <Badge variant="outline" className="bg-primary/5 text-primary">
                                {currentShareFor(c.investorId)}%
                              </Badge>
                            ) : (
                              <span className="text-xs text-muted-foreground">—</span>
                            )}
                          </TableCell>
                          {canManageWithdrawals && (
                            <TableCell className="text-right">
                              <Button
                                size="sm"
                                variant="outline"
                                onClick={() => c.id && openWithdrawalDialog(c.id)}
                              >
                                <ArrowDownCircle className="mr-1 h-4 w-4" />
                                Retirer
                              </Button>
                            </TableCell>
                          )}
                        </TableRow>
                      ))
                    )}
                  </TableBody>
                </Table>
              </div>
            )}

            {selectedProjectId && withdrawals.length > 0 && (
              <div className="pt-4">
                <h3 className="text-sm font-semibold text-muted-foreground mb-2">Retraits de capital</h3>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Investisseur</TableHead>
                      <TableHead className="text-right">Montant demandé</TableHead>
                      <TableHead className="text-right">Montant net</TableHead>
                      <TableHead>Statut</TableHead>
                      {canManageWithdrawals && <TableHead className="text-right">Action</TableHead>}
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {withdrawals.map((w) => (
                      <TableRow key={w.id}>
                        <TableCell className="font-medium">{investorName(w.investorId ?? 0)}</TableCell>
                        <TableCell className="text-right">{(w.requestedAmount ?? 0).toLocaleString()} FCFA</TableCell>
                        <TableCell className="text-right">
                          {w.netAmountPaid != null ? `${w.netAmountPaid.toLocaleString()} FCFA` : "—"}
                        </TableCell>
                        <TableCell>
                          <Badge
                            variant="outline"
                            className={
                              w.status === "VALIDATED"
                                ? "bg-primary/10 text-primary"
                                : w.status === "REJECTED" || w.status === "BLOCKED"
                                ? "bg-destructive/10 text-destructive"
                                : "bg-accent/10 text-accent-foreground"
                            }
                            title={w.blockReason ?? undefined}
                          >
                            {WITHDRAWAL_STATUS_LABELS[w.status ?? ""] ?? w.status}
                          </Badge>
                        </TableCell>
                        {canManageWithdrawals && (
                          <TableCell className="text-right">
                            {w.status === "REQUESTED" && (
                              <div className="flex justify-end gap-2">
                                <Button
                                  size="sm"
                                  className="bg-primary"
                                  disabled={actingOnWithdrawalId === w.id}
                                  onClick={() => w.id && handleValidateWithdrawal(w.id)}
                                >
                                  <Check className="mr-1 h-4 w-4" />
                                  Valider
                                </Button>
                                <Button
                                  size="sm"
                                  variant="outline"
                                  disabled={actingOnWithdrawalId === w.id}
                                  onClick={() => w.id && handleRejectWithdrawal(w.id)}
                                >
                                  <X className="mr-1 h-4 w-4" />
                                  Rejeter
                                </Button>
                              </div>
                            )}
                          </TableCell>
                        )}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            )}
          </CardContent>
        </Card>

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
                      <TableHead className="text-right">Action</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {investors.length === 0 ? (
                      <TableRow>
                        <TableCell
                          colSpan={4}
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
                            <TableCell className="text-right">
                               <Button variant="ghost" size="sm" onClick={() => openEdit(inv)}>
                                 <Edit2 className="h-4 w-4" />
                               </Button>
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

      <Dialog open={dialogOpen || !!editingInvestor} onOpenChange={(val) => { if(!val) { setDialogOpen(false); setEditingInvestor(null); } }}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{editingInvestor ? "Modifier l'Investissement" : "Nouvel Investissement"}</DialogTitle>
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
            <Button variant="outline" onClick={() => { setDialogOpen(false); setEditingInvestor(null); }}>
              Annuler
            </Button>
            <Button
              className="bg-primary"
              disabled={!form.fullName || !form.contribution || saving}
              onClick={editingInvestor ? handleUpdate : handleCreate}
            >
              {saving && <RefreshCcw className="mr-2 h-4 w-4 animate-spin" />}
              {editingInvestor ? "Enregistrer" : "Créer"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={withdrawalDialogOpen} onOpenChange={setWithdrawalDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Demander un retrait de capital</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-2">
            <div className="space-y-2">
              <Label>Montant à retirer (FCFA)</Label>
              <Input
                type="number"
                placeholder="Ex: 500000"
                value={withdrawalAmount}
                onChange={(e) => setWithdrawalAmount(e.target.value)}
              />
            </div>
            <p className="text-xs text-muted-foreground">
              Le retrait peut être bloqué si le projet ne peut pas l'absorber, et une pénalité
              éventuelle peut réduire le montant net versé.
            </p>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setWithdrawalDialogOpen(false)}>
              Annuler
            </Button>
            <Button
              className="bg-primary"
              disabled={!withdrawalAmount || withdrawalSaving}
              onClick={handleRequestWithdrawal}
            >
              {withdrawalSaving && <RefreshCcw className="mr-2 h-4 w-4 animate-spin" />}
              Demander le retrait
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
