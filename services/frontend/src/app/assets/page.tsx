"use client";

import { useEffect, useState } from "react";
import { Navbar } from "@/components/layout/Navbar";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
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
import { PlusCircle, Landmark, HandCoins, Ban, RefreshCcw, Lock, Calculator, Tag } from "lucide-react";
import { api } from "@/api";
import type { components } from "@/api";
import { toast } from "@/hooks/use-toast";

type ProjectDto = components["schemas"]["ProjectDto"];
type InvestorDto = components["schemas"]["InvestorDto"];
type AssetDto = components["schemas"]["AssetDto"];
type LeaseDto = components["schemas"]["LeaseDto"];
type ProjectUsageSimulationDto = components["schemas"]["ProjectUsageSimulationDto"];
type DisposeAssetRequest = components["schemas"]["DisposeAssetRequest"];

const ASSET_TYPE_LABELS: Record<string, string> = {
  LAND: "Terrain",
  BUILDING: "Bâtiment",
  MACHINE: "Machine",
  VEHICLE: "Véhicule",
  ANIMAL: "Animal",
  OTHER: "Autre",
};

export default function AssetsPage() {
  const [projects, setProjects] = useState<ProjectDto[]>([]);
  const [investors, setInvestors] = useState<InvestorDto[]>([]);
  const [selectedProjectId, setSelectedProjectId] = useState<string>("");
  const [assets, setAssets] = useState<AssetDto[]>([]);
  const [leasesByAsset, setLeasesByAsset] = useState<Record<number, LeaseDto[]>>({});
  const [loading, setLoading] = useState(false);

  const [assetDialogOpen, setAssetDialogOpen] = useState(false);
  const [assetSaving, setAssetSaving] = useState(false);
  const [assetForm, setAssetForm] = useState({ ownerInvestorId: "", type: "BUILDING", description: "", initialValue: "" });

  const [leaseDialogAssetId, setLeaseDialogAssetId] = useState<number | null>(null);
  const [leaseSaving, setLeaseSaving] = useState(false);
  const [leaseForm, setLeaseForm] = useState({ monthlyRent: "", startDate: new Date().toISOString().slice(0, 10) });

  const [actingOnLeaseId, setActingOnLeaseId] = useState<number | null>(null);

  const [role, setRole] = useState<string | null>(null);
  const [closing, setClosing] = useState(false);
  const [simulation, setSimulation] = useState<ProjectUsageSimulationDto | null>(null);

  const [disposeAssetId, setDisposeAssetId] = useState<number | null>(null);
  const [disposeSaving, setDisposeSaving] = useState(false);
  const [disposeForm, setDisposeForm] = useState({ disposition: "SOLD", amount: "", buyerInvestorId: "" });

  const canManage = role === "ADMIN" || role === "MANAGER";
  const selectedProject = projects.find((p) => p.id === Number(selectedProjectId));
  const isProjectClosed = selectedProject?.status === "CLOSED";

  const investorName = (investorId: number) =>
    investors.find((i) => i.id === investorId)?.fullName ?? `Investisseur #${investorId}`;

  const fetchRole = async () => {
    const { data } = await api.auth.me();
    if (data && "role" in data) setRole(String(data.role));
  };

  const fetchSimulation = async (projectId: string) => {
    if (!projectId) {
      setSimulation(null);
      return;
    }
    const { data } = await api.projects.simulate(Number(projectId));
    setSimulation(data ?? null);
  };

  const fetchProjects = async () => {
    const { data } = await api.projects.getAll();
    if (data) setProjects(data);
  };

  const fetchInvestors = async () => {
    const { data } = await api.investors.getAll();
    if (data) setInvestors(data);
  };

  const fetchAssets = async (projectId: string) => {
    if (!projectId) {
      setAssets([]);
      setLeasesByAsset({});
      return;
    }
    setLoading(true);
    const { data, error } = await api.assets.getByProject(Number(projectId));
    setLoading(false);
    if (error) {
      toast({ variant: "destructive", title: "Erreur", description: "Impossible de charger les actifs." });
      return;
    }
    setAssets(data ?? []);

    const leasesEntries = await Promise.all(
      (data ?? []).map(async (a) => {
        if (!a.id) return [a.id, []] as const;
        const res = await api.leases.getByAsset(a.id);
        return [a.id, res.data ?? []] as const;
      })
    );
    setLeasesByAsset(Object.fromEntries(leasesEntries.filter(([id]) => id != null)) as Record<number, LeaseDto[]>);
  };

  useEffect(() => {
    fetchProjects();
    fetchInvestors();
    fetchRole();
  }, []);

  useEffect(() => {
    fetchAssets(selectedProjectId);
    fetchSimulation(selectedProjectId);
  }, [selectedProjectId]);

  const activeLeaseOf = (assetId?: number): LeaseDto | undefined =>
    assetId != null ? leasesByAsset[assetId]?.find((l) => l.status === "ACTIVE") : undefined;

  const handleCreateAsset = async () => {
    if (!selectedProjectId || !assetForm.ownerInvestorId || !assetForm.initialValue) return;
    setAssetSaving(true);
    const { error } = await api.assets.create(Number(selectedProjectId), {
      ownerInvestorId: Number(assetForm.ownerInvestorId),
      type: assetForm.type as AssetDto["type"],
      description: assetForm.description,
      initialValue: Number(assetForm.initialValue),
    });
    setAssetSaving(false);
    if (error) {
      toast({ variant: "destructive", title: "Erreur", description: "Impossible d'enregistrer cet actif." });
      return;
    }
    toast({ title: "Actif enregistré" });
    setAssetForm({ ownerInvestorId: "", type: "BUILDING", description: "", initialValue: "" });
    setAssetDialogOpen(false);
    fetchAssets(selectedProjectId);
  };

  const handleCreateLease = async () => {
    if (!leaseDialogAssetId || !leaseForm.monthlyRent) return;
    setLeaseSaving(true);
    const { error } = await api.leases.create(leaseDialogAssetId, {
      monthlyRent: Number(leaseForm.monthlyRent),
      startDate: leaseForm.startDate,
    });
    setLeaseSaving(false);
    if (error) {
      toast({ variant: "destructive", title: "Erreur", description: "Impossible de créer ce bail." });
      return;
    }
    toast({ title: "Bail créé" });
    setLeaseDialogAssetId(null);
    setLeaseForm({ monthlyRent: "", startDate: new Date().toISOString().slice(0, 10) });
    fetchAssets(selectedProjectId);
  };

  const handlePostRent = async (leaseId: number) => {
    setActingOnLeaseId(leaseId);
    const { error } = await api.leases.postRent(leaseId);
    setActingOnLeaseId(null);
    if (error) {
      toast({ variant: "destructive", title: "Erreur", description: "Impossible d'encaisser ce loyer." });
      return;
    }
    toast({ title: "Loyer encaissé", description: "Enregistré comme charge du projet." });
  };

  const handleEndLease = async (leaseId: number) => {
    setActingOnLeaseId(leaseId);
    const { error } = await api.leases.end(leaseId);
    setActingOnLeaseId(null);
    if (error) {
      toast({ variant: "destructive", title: "Erreur", description: "Impossible de terminer ce bail." });
      return;
    }
    toast({ title: "Bail terminé" });
    fetchAssets(selectedProjectId);
  };

  const handleCloseProject = async () => {
    if (!selectedProjectId) return;
    setClosing(true);
    const { error } = await api.projects.close(Number(selectedProjectId));
    setClosing(false);
    if (error) {
      toast({ variant: "destructive", title: "Erreur", description: "Impossible de clôturer ce projet." });
      return;
    }
    toast({ title: "Projet clôturé", description: "Les actifs apportés au capital doivent maintenant être réglés." });
    fetchProjects();
  };

  const handleDisposeAsset = async () => {
    if (!disposeAssetId || !disposeForm.amount) return;
    if (disposeForm.disposition === "RETAINED_BY_INVESTOR" && !disposeForm.buyerInvestorId) return;
    setDisposeSaving(true);
    const { error } = await api.assets.dispose(disposeAssetId, {
      disposition: disposeForm.disposition as DisposeAssetRequest["disposition"],
      amount: Number(disposeForm.amount),
      buyerInvestorId: disposeForm.buyerInvestorId ? Number(disposeForm.buyerInvestorId) : undefined,
    });
    setDisposeSaving(false);
    if (error) {
      toast({ variant: "destructive", title: "Erreur", description: "Impossible de régler cet actif." });
      return;
    }
    toast({ title: "Actif réglé", description: "Le montant a été enregistré comme revenu du projet." });
    setDisposeAssetId(null);
    setDisposeForm({ disposition: "SOLD", amount: "", buyerInvestorId: "" });
    fetchAssets(selectedProjectId);
  };

  return (
    <div className="min-h-screen bg-background pb-12">
      <Navbar />
      <main className="container mx-auto px-4 py-8">
        <header className="mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="space-y-1">
            <h1 className="text-3xl font-bold text-primary">Actifs &amp; Baux</h1>
            <p className="text-muted-foreground">
              Biens apportés ou loués au projet (ferme, machines...). Un actif est soit loué
              (son propriétaire garde la propriété et perçoit un loyer), soit apporté au
              capital via un apport en nature — jamais les deux à la fois.
            </p>
          </div>
          <div className="flex gap-2 w-full md:w-auto">
            <div className="w-full md:w-56">
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
            <Button className="bg-primary" disabled={!selectedProjectId || isProjectClosed} onClick={() => setAssetDialogOpen(true)}>
              <PlusCircle className="mr-2 h-4 w-4" />
              Nouvel actif
            </Button>
            {canManage && selectedProjectId && !isProjectClosed && (
              <Button variant="outline" disabled={closing} onClick={handleCloseProject}>
                {closing ? <RefreshCcw className="mr-2 h-4 w-4 animate-spin" /> : <Lock className="mr-2 h-4 w-4" />}
                Clôturer
              </Button>
            )}
          </div>
        </header>

        {isProjectClosed && (
          <div className="mb-6 rounded-lg border border-accent bg-accent/10 px-4 py-3 text-sm text-accent-foreground flex items-center gap-2">
            <Lock className="h-4 w-4" />
            Ce projet est clôturé. Les actifs apportés au capital doivent être réglés (vendus ou rachetés par un investisseur).
          </div>
        )}

        {simulation && simulation.perAsset && simulation.perAsset.length > 0 && (
          <Card className="mb-6">
            <CardHeader>
              <CardTitle className="text-base flex items-center gap-2">
                <Calculator className="h-4 w-4 text-primary" />
                Simulation "tout le monde paie un loyer" ({simulation.monthlyRatePercent}%/mois)
              </CardTitle>
              <CardDescription>
                Coût d'usage hypothétique si chaque actif était loué, comparé aux loyers réellement payés aujourd'hui.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="flex gap-6 text-sm">
                <div>
                  <span className="text-muted-foreground">Coût hypothétique total : </span>
                  <span className="font-bold">{simulation.totalHypotheticalMonthlyCost?.toLocaleString()} FCFA/mois</span>
                </div>
                <div>
                  <span className="text-muted-foreground">Loyers réellement payés : </span>
                  <span className="font-bold">{simulation.actualCurrentMonthlyRent?.toLocaleString()} FCFA/mois</span>
                </div>
              </div>
            </CardContent>
          </Card>
        )}

        {!selectedProjectId ? (
          <Card>
            <CardContent className="py-12 text-center text-muted-foreground">
              Choisis un projet pour voir ses actifs.
            </CardContent>
          </Card>
        ) : loading ? (
          <p className="text-center text-muted-foreground py-8">Chargement...</p>
        ) : assets.length === 0 ? (
          <Card>
            <CardContent className="py-12 text-center text-muted-foreground">
              Aucun actif enregistré pour ce projet.
            </CardContent>
          </Card>
        ) : (
          <div className="grid gap-4 md:grid-cols-2">
            {assets.map((asset) => {
              const lease = activeLeaseOf(asset.id);
              return (
                <Card key={asset.id}>
                  <CardHeader>
                    <CardTitle className="text-lg flex items-center gap-2">
                      <Landmark className="h-5 w-5 text-primary" />
                      {ASSET_TYPE_LABELS[asset.type ?? ""] ?? asset.type}
                    </CardTitle>
                    <CardDescription>{asset.description || "Sans description"}</CardDescription>
                  </CardHeader>
                  <CardContent className="space-y-3">
                    <div className="flex justify-between text-sm">
                      <span className="text-muted-foreground">Propriétaire</span>
                      <span className="font-medium">{investorName(asset.ownerInvestorId)}</span>
                    </div>
                    <div className="flex justify-between text-sm">
                      <span className="text-muted-foreground">Valeur initiale</span>
                      <span className="font-medium">{(asset.initialValue ?? 0).toLocaleString()} FCFA</span>
                    </div>
                    <div className="flex justify-between text-sm items-center">
                      <span className="text-muted-foreground">Statut</span>
                      <Badge
                        variant="outline"
                        className={
                          asset.ownershipStatus === "TRANSFERRED_TO_PROJECT"
                            ? "bg-primary/10 text-primary"
                            : "bg-accent/10 text-accent-foreground"
                        }
                      >
                        {asset.ownershipStatus === "TRANSFERRED_TO_PROJECT"
                          ? "Apporté au capital"
                          : "Propriété du contributeur"}
                      </Badge>
                    </div>

                    {asset.ownershipStatus === "TRANSFERRED_TO_PROJECT" ? (
                      asset.disposition && asset.disposition !== "PENDING" ? (
                        <p className="text-xs text-muted-foreground italic">
                          Réglé : {asset.disposition === "SOLD" ? "vendu" : "racheté par un investisseur"} pour{" "}
                          {(asset.disposalAmount ?? 0).toLocaleString()} FCFA.
                        </p>
                      ) : isProjectClosed && canManage ? (
                        <Button size="sm" variant="outline" onClick={() => asset.id && setDisposeAssetId(asset.id)}>
                          <Tag className="mr-1 h-4 w-4" />
                          Régler cet actif
                        </Button>
                      ) : (
                        <p className="text-xs text-muted-foreground italic">
                          Cet actif fait partie du capital du projet (apport en nature).
                        </p>
                      )
                    ) : lease ? (
                      <div className="rounded-md border p-3 space-y-2">
                        <div className="flex justify-between text-sm">
                          <span>Loyer mensuel</span>
                          <span className="font-bold">{lease.monthlyRent?.toLocaleString()} FCFA</span>
                        </div>
                        <div className="flex gap-2">
                          <Button
                            size="sm"
                            className="bg-primary"
                            disabled={actingOnLeaseId === lease.id}
                            onClick={() => lease.id && handlePostRent(lease.id)}
                          >
                            <HandCoins className="mr-1 h-4 w-4" />
                            Encaisser le loyer
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            disabled={actingOnLeaseId === lease.id}
                            onClick={() => lease.id && handleEndLease(lease.id)}
                          >
                            <Ban className="mr-1 h-4 w-4" />
                            Terminer
                          </Button>
                        </div>
                      </div>
                    ) : (
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => asset.id && setLeaseDialogAssetId(asset.id)}
                      >
                        Créer un bail
                      </Button>
                    )}
                  </CardContent>
                </Card>
              );
            })}
          </div>
        )}
      </main>

      <Dialog open={assetDialogOpen} onOpenChange={setAssetDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Nouvel actif</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-2">
            <div className="space-y-2">
              <Label>Propriétaire</Label>
              <Select
                value={assetForm.ownerInvestorId}
                onValueChange={(v) => setAssetForm({ ...assetForm, ownerInvestorId: v })}
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
            <div className="space-y-2">
              <Label>Type</Label>
              <Select value={assetForm.type} onValueChange={(v) => setAssetForm({ ...assetForm, type: v })}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {Object.entries(ASSET_TYPE_LABELS).map(([value, label]) => (
                    <SelectItem key={value} value={value}>
                      {label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Description</Label>
              <Input
                placeholder="Ex: Ferme + bâtiments + équipements"
                value={assetForm.description}
                onChange={(e) => setAssetForm({ ...assetForm, description: e.target.value })}
              />
            </div>
            <div className="space-y-2">
              <Label>Valeur initiale (FCFA)</Label>
              <Input
                type="number"
                placeholder="Ex: 8000000"
                value={assetForm.initialValue}
                onChange={(e) => setAssetForm({ ...assetForm, initialValue: e.target.value })}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setAssetDialogOpen(false)}>
              Annuler
            </Button>
            <Button
              className="bg-primary"
              disabled={!assetForm.ownerInvestorId || !assetForm.initialValue || assetSaving}
              onClick={handleCreateAsset}
            >
              {assetSaving && <RefreshCcw className="mr-2 h-4 w-4 animate-spin" />}
              Enregistrer
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={leaseDialogAssetId !== null} onOpenChange={(open) => !open && setLeaseDialogAssetId(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Créer un bail</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-2">
            <div className="space-y-2">
              <Label>Loyer mensuel (FCFA)</Label>
              <Input
                type="number"
                placeholder="Ex: 80000"
                value={leaseForm.monthlyRent}
                onChange={(e) => setLeaseForm({ ...leaseForm, monthlyRent: e.target.value })}
              />
            </div>
            <div className="space-y-2">
              <Label>Date de début</Label>
              <Input
                type="date"
                value={leaseForm.startDate}
                onChange={(e) => setLeaseForm({ ...leaseForm, startDate: e.target.value })}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setLeaseDialogAssetId(null)}>
              Annuler
            </Button>
            <Button className="bg-primary" disabled={!leaseForm.monthlyRent || leaseSaving} onClick={handleCreateLease}>
              {leaseSaving && <RefreshCcw className="mr-2 h-4 w-4 animate-spin" />}
              Créer le bail
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={disposeAssetId !== null} onOpenChange={(open) => !open && setDisposeAssetId(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Régler cet actif</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-2">
            <div className="space-y-2">
              <Label>Mode de règlement</Label>
              <Select
                value={disposeForm.disposition}
                onValueChange={(v) => setDisposeForm({ ...disposeForm, disposition: v })}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="SOLD">Vendu à un tiers</SelectItem>
                  <SelectItem value="RETAINED_BY_INVESTOR">Racheté par un investisseur</SelectItem>
                </SelectContent>
              </Select>
            </div>
            {disposeForm.disposition === "RETAINED_BY_INVESTOR" && (
              <div className="space-y-2">
                <Label>Investisseur acheteur</Label>
                <Select
                  value={disposeForm.buyerInvestorId}
                  onValueChange={(v) => setDisposeForm({ ...disposeForm, buyerInvestorId: v })}
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
            )}
            <div className="space-y-2">
              <Label>Montant (FCFA)</Label>
              <Input
                type="number"
                placeholder="Ex: 8500000"
                value={disposeForm.amount}
                onChange={(e) => setDisposeForm({ ...disposeForm, amount: e.target.value })}
              />
              <p className="text-xs text-muted-foreground">
                Enregistré comme revenu du projet, à répartir via une distribution de clôture.
              </p>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDisposeAssetId(null)}>
              Annuler
            </Button>
            <Button
              className="bg-primary"
              disabled={
                !disposeForm.amount ||
                (disposeForm.disposition === "RETAINED_BY_INVESTOR" && !disposeForm.buyerInvestorId) ||
                disposeSaving
              }
              onClick={handleDisposeAsset}
            >
              {disposeSaving && <RefreshCcw className="mr-2 h-4 w-4 animate-spin" />}
              Confirmer
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
