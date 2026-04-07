
"use client";

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
  Stethoscope,
  Phone,
  MessageSquare
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { MOCK_TRANSACTIONS, MOCK_VETERINARIANS } from "@/lib/mock-data";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";

export default function Home() {
  const recentTransactions = MOCK_TRANSACTIONS.slice(0, 5);
  const assignedVet = MOCK_VETERINARIANS[0]; // Simulation: Dr. Kamgaing assigned to this project

  return (
    <div className="min-h-screen bg-background pb-12">
      <Navbar />
      <main className="container mx-auto px-4 py-8">
        <header className="mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="space-y-1">
            <h1 className="text-3xl font-bold text-primary">Tableau de Bord</h1>
            <p className="text-muted-foreground">Bienvenue sur AgriShare. Gérer vos projets agro-pastoraux.</p>
          </div>
          <div className="flex flex-col md:flex-row gap-4">
            <div className="flex items-center gap-2 bg-white p-2 rounded-lg border">
              <Layers className="h-4 w-4 text-muted-foreground" />
              <span className="text-sm font-medium">Projet:</span>
              <Badge variant="outline" className="bg-primary/5 text-primary">Ferme Poules Ndongo</Badge>
            </div>
            {assignedVet && (
              <div className="flex items-center gap-3 bg-primary/5 p-2 px-4 rounded-lg border border-primary/20">
                <div className="h-8 w-8 rounded-full bg-primary/10 flex items-center justify-center">
                  <Stethoscope className="h-4 w-4 text-primary" />
                </div>
                <div>
                  <p className="text-[10px] uppercase font-bold text-muted-foreground">Expert en charge</p>
                  <p className="text-xs font-bold text-primary">{assignedVet.name}</p>
                </div>
                <div className="flex gap-1 ml-2">
                  <Button size="icon" variant="ghost" className="h-7 w-7 rounded-full text-primary hover:bg-primary/10">
                    <Phone className="h-3 w-3" />
                  </Button>
                  <Button size="icon" variant="ghost" className="h-7 w-7 rounded-full text-primary hover:bg-primary/10">
                    <MessageSquare className="h-3 w-3" />
                  </Button>
                </div>
              </div>
            )}
          </div>
        </header>

        <section className="space-y-8">
          <StatCards />
          <AdvisorPanel />

          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            <ProductionChart />

            <Card className="col-span-1">
              <CardHeader className="flex flex-row items-center justify-between pb-2">
                <div className="space-y-1">
                  <CardTitle className="text-lg">Dernières Opérations</CardTitle>
                  <CardDescription>Comptabilité récente</CardDescription>
                </div>
                <History className="h-4 w-4 text-muted-foreground" />
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  {recentTransactions.map((tx) => (
                    <div key={tx.id} className="flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <div className={`rounded-full p-2 ${tx.type === 'INCOME' ? 'bg-primary/10 text-primary' : 'bg-destructive/10 text-destructive'}`}>
                          {tx.type === 'INCOME' ? <ArrowUpRight className="h-4 w-4" /> : <ArrowDownRight className="h-4 w-4" />}
                        </div>
                        <div className="flex flex-col">
                          <span className="text-sm font-medium leading-none">{tx.category}</span>
                          <span className="text-xs text-muted-foreground">{new Date(tx.date).toLocaleDateString('fr-FR')}</span>
                        </div>
                      </div>
                      <span className={`text-sm font-bold ${tx.type === 'INCOME' ? 'text-primary' : 'text-destructive'}`}>
                        {tx.type === 'INCOME' ? '+' : '-'}{tx.amount.toLocaleString()} F
                      </span>
                    </div>
                  ))}
                </div>
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
                  <div className="flex justify-between items-center text-sm">
                    <span>Aliment Démarrage</span>
                    <span className="font-bold">12 sacs</span>
                  </div>
                  <div className="flex justify-between items-center text-sm">
                    <span>Plateaux Œufs (vides)</span>
                    <span className="font-bold">150 unités</span>
                  </div>
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
                   <div className="flex justify-between items-center text-sm">
                    <span>Jean-Pierre Ndongo</span>
                    <span className="font-bold text-primary">70%</span>
                  </div>
                  <div className="flex justify-between items-center text-sm">
                    <span>Marie Tchakoute</span>
                    <span className="font-bold text-primary">30%</span>
                  </div>
                </CardContent>
             </Card>
          </div>
        </section>
      </main>
    </div>
  );
}
