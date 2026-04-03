import { StatCards } from "@/components/dashboard/StatCards";
import { ProductionChart } from "@/components/dashboard/ProductionChart";
import { AdvisorPanel } from "@/components/ai/AdvisorPanel";
import { Navbar } from "@/components/layout/Navbar";
import { 
  ArrowUpRight, 
  ArrowDownRight, 
  History,
  TrendingUp,
  Package
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { MOCK_TRANSACTIONS } from "@/lib/mock-data";

export default function Home() {
  const recentTransactions = MOCK_TRANSACTIONS.slice(0, 5);

  return (
    <div className="min-h-screen bg-background pb-12">
      <Navbar />
      <main className="container mx-auto px-4 py-8">
        <header className="mb-8 flex flex-col gap-2">
          <h1 className="text-3xl font-bold text-primary">Tableau de Bord</h1>
          <p className="text-muted-foreground">Bienvenue sur AgriShare. Voici l'état actuel de la ferme.</p>
        </header>

        <section className="space-y-8">
          {/* KPI Section */}
          <StatCards />

          {/* AI Advisor Section */}
          <AdvisorPanel />

          {/* Charts & Lists Section */}
          <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
            <ProductionChart />

            {/* Recent Transactions */}
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

          {/* Additional Quick Info */}
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
                  <div className="flex justify-between items-center text-sm">
                    <span>Vaccins Gumboro</span>
                    <span className="font-bold">3 doses</span>
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
                  <div className="pt-2 border-t text-xs text-muted-foreground">
                    Basé sur 10,000,000 FCFA investis.
                  </div>
                </CardContent>
             </Card>
          </div>
        </section>
      </main>
    </div>
  );
}
