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
import { Badge } from "@/components/ui/badge";
import { api } from "@/api";
import type { components } from "@/api";
import { useFirestore, useCollection, useMemoFirebase, useUser } from "@/firebase";
import { collection } from "firebase/firestore";

type OperationDto = components["schemas"]["OperationDto"];
type InvestorDto = components["schemas"]["InvestorDto"];
type ProductionDayDto = components["schemas"]["ProductionDayDto"];
type StockItemDto = components["schemas"]["StockItemDto"];
type CapitalShareDto = components["schemas"]["CapitalShareDto"];

interface FirestoreProject {
  id: string;
  name: string;
  domain: string;
  type: string;
  status: string;
  createdAt: string;
}

export default function Home() {
  const db = useFirestore();
  const { user, isUserLoading } = useUser();
  const projectsRef = useMemoFirebase(() => collection(db, "projects"), [db]);
  const { data: projects } = useCollection<FirestoreProject>(
    isUserLoading || !user ? null : projectsRef
  );
  const project = projects?.[0] ?? null;

  const [operations, setOperations] = useState<OperationDto[]>([]);
  const [investors, setInvestors] = useState<InvestorDto[]>([]);
  const [productions, setProductions] = useState<ProductionDayDto[]>([]);
  const [stockItems, setStockItems] = useState<StockItemDto[]>([]);
  const [capitalShares, setCapitalShares] = useState<CapitalShareDto[]>([]);

  useEffect(() => {
    Promise.all([
      api.operations.getAll(),
      api.investors.getAll(),
      api.productionDays.getAll(),
      api.stockInventories.getAll(),
      api.capitalDistributions.getAll(),
    ]).then(([opsRes, invRes, prodRes, stockRes, capitalRes]) => {
      if (opsRes.data) setOperations(opsRes.data);
      if (invRes.data) setInvestors(invRes.data);
      if (prodRes.data) setProductions(prodRes.data);
      if (stockRes.data?.length)
        setStockItems(stockRes.data.flatMap((s) => s.items ?? []));
      if (capitalRes.data?.length)
        setCapitalShares(capitalRes.data.flatMap((d) => d.shares ?? []));
    });
  }, []);

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
          {project && (
            <div className="flex items-center gap-2 bg-white p-2 rounded-lg border">
              <Layers className="h-4 w-4 text-muted-foreground" />
              <span className="text-sm font-medium">Projet:</span>
              <Badge variant="outline" className="bg-primary/5 text-primary">
                {project.name}
              </Badge>
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
              </CardHeader>
              <CardContent className="space-y-4">
                {capitalShares.length === 0 ? (
                  <p className="text-sm text-muted-foreground text-center py-2">
                    Aucune distribution enregistrée.
                  </p>
                ) : (
                  capitalShares.map((share, i) => (
                    <div key={i} className="flex justify-between items-center text-sm">
                      <span>{share.investorName}</span>
                      <span className="font-bold text-primary">
                        {share.percentage}%
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
