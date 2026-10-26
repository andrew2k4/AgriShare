"use client";

import { useState, useEffect } from "react";
import { StatCards } from "@/components/dashboard/StatCards";
import { ProductionChart } from "@/components/dashboard/ProductionChart";
import { AdvisorPanel } from "@/components/ai/AdvisorPanel";
import { Navbar } from "@/components/layout/Navbar";
import {
  ArrowUpRight,
  ArrowDownRight,
  History,
  TrendingUp,
  Package,
  Layers,
} from "lucide-react";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription,
} from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { api } from "@/api";
import type { components } from "@/api";

type OperationDto = components["schemas"]["OperationDto"];
type InvestorDto = components["schemas"]["InvestorDto"];
type ProductionDayDto = components["schemas"]["ProductionDayDto"];
type StockItemDto = components["schemas"]["StockItemDto"];
type ProjectDto = components["schemas"]["ProjectDto"];
type InvestorShareDto = components["schemas"]["InvestorShareDto"];

export function DashboardHome() {
  const [projects, setProjects] = useState<ProjectDto[]>([]);
  const [selectedProjectId, setSelectedProjectId] = useState<string>("");

  const [operations, setOperations] = useState<OperationDto[]>([]);
  const [investors, setInvestors] = useState<InvestorDto[]>([]);
  const [productions, setProductions] = useState<ProductionDayDto[]>([]);
  const [stockItems, setStockItems] = useState<StockItemDto[]>([]);
  const [currentShares, setCurrentShares] = useState<InvestorShareDto[]>([]);

  useEffect(() => {
    Promise.all([
      api.projects.getAll(),
      api.investors.getAll(),
      api.productionDays.getAll(),
      api.stockInventories.getAll(),
    ]).then(([projRes, invRes, prodRes, stockRes]) => {
      if (projRes.data) {
        setProjects(projRes.data);
        const firstId = projRes.data[0]?.id;
        if (firstId != null) setSelectedProjectId(String(firstId));
      }
      if (invRes.data) setInvestors(invRes.data);
      if (prodRes.data) setProductions(prodRes.data);
      if (stockRes.data?.length)
        setStockItems(stockRes.data.flatMap((s) => s.items ?? []));
    });
  }, []);

  useEffect(() => {
    if (!selectedProjectId) {
      setOperations([]);
      setCurrentShares([]);
      return;
    }
    api.operations.getByProject(Number(selectedProjectId)).then(({ data }) => {
      if (data) setOperations(data);
    });
    api.projects.financialSummary(Number(selectedProjectId)).then(({ data }) => {
      setCurrentShares(data?.currentShares ?? []);
    });
  }, [selectedProjectId]);

  const investorName = (investorId: number) =>
    investors.find((i) => i.id === investorId)?.fullName ?? `Investisseur #${investorId}`;

  const recentOperations = [...operations].reverse().slice(0, 5);

  return (
    <div className="min-h-screen bg-background pb-12">
      <Navbar />
      <main className="container mx-auto px-4 py-8">
        <header className="mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="space-y-1">
            <h1 className="text-3xl font-bold text-primary">Tableau de Bord</h1>
            <p className="text-muted-foreground">
              Bienvenue sur AgriShare. Gérer vos projets agro-pastoraux.
            </p>
          </div>
          {projects.length > 0 && (
            <div className="flex items-center gap-2 bg-white p-2 rounded-lg border">
              <Layers className="h-4 w-4 text-muted-foreground" />
              <span className="text-sm font-medium">Projet:</span>
              <Select value={selectedProjectId} onValueChange={setSelectedProjectId}>
                <SelectTrigger className="h-8 w-48 border-none bg-transparent">
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
          )}
        </header>

        <section className="space-y-8">
          <StatCards
            operations={operations}
            investors={investors}
            productions={productions}
          />
          <AdvisorPanel />

          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            <ProductionChart data={productions} />

            <Card className="col-span-1">
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <div className="space-y-1">
                  <CardTitle className="text-lg">Dernières Opérations</CardTitle>
                  <CardDescription>Comptabilité récente</CardDescription>
                </div>
                <History className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                {recentOperations.length === 0 ? (
                  <p className="text-sm text-muted-foreground text-center py-4">
                    Aucune opération enregistrée.
                  </p>
                ) : (
                  <div className="space-y-4">
                    {recentOperations.map((op) => (
                      <div
                        key={op.id}
                        className="flex items-center justify-between"
                      >
                        <div className="flex items-center gap-3">
                          <div
                            className={`rounded-full p-2 ${op.operationType === "INCOME" ? "bg-primary/10 text-primary" : "bg-destructive/10 text-destructive"}`}
                          >
                            {op.operationType === "INCOME" ? (
                              <ArrowUpRight className="h-4 w-4" />
                            ) : (
                              <ArrowDownRight className="h-4 w-4" />
                            )}
                          </div>
                          <div className="flex flex-col">
                            <span className="text-sm font-medium leading-none">
                              {op.label}
                            </span>
                            <span className="text-xs text-muted-foreground">
                              {new Date(op.date).toLocaleDateString("fr-FR")}
                            </span>
                          </div>
                        </div>
                        <span
                          className={`text-sm font-bold ${op.operationType === "INCOME" ? "text-primary" : "text-destructive"}`}
                        >
                          {op.operationType === "INCOME" ? "+" : "-"}
                          {op.amount.toLocaleString()} F
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </CardContent>
            </Card>
          </div>

          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            <Card>
              <CardHeader>
                <CardTitle className="text-md flex items-center gap-2">
                  <Package className="h-5 w-5 text-primary" />
                  Inventaire Stock
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                {stockItems.length === 0 ? (
                  <p className="text-sm text-muted-foreground text-center py-2">
                    Aucun article en stock.
                  </p>
                ) : (
                  stockItems.map((item, i) => (
                    <div key={i} className="flex justify-between items-center text-sm">
                      <span>{item.name}</span>
                      <span className="font-bold">
                        {item.quantity} {item.unit}
                      </span>
                    </div>
                  ))
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-md flex items-center gap-2">
                  <TrendingUp className="h-5 w-5 text-accent" />
                  Parts de Capital
                </CardTitle>
                <CardDescription>Projet sélectionné, basé sur les unités de contribution</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                {currentShares.length === 0 ? (
                  <p className="text-sm text-muted-foreground text-center py-2">
                    Aucun apport en capital enregistré pour ce projet.
                  </p>
                ) : (
                  currentShares.map((share) => (
                    <div key={share.investorId} className="flex justify-between items-center text-sm">
                      <span>{investorName(share.investorId ?? 0)}</span>
                      <span className="font-bold text-primary">
                        {share.sharePercentage}%
                      </span>
                    </div>
                  ))
                )}
              </CardContent>
            </Card>
          </div>
        </section>
      </main>
    </div>
  );
}
