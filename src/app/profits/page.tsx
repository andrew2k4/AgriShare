
"use client";

import { useState, useMemo } from "react";
import { Navbar } from "@/components/layout/Navbar";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { 
  PiggyBank, 
  RefreshCw, 
  HandCoins, 
  History, 
  TrendingUp, 
  Clock, 
  Info,
  Calendar,
  Calculator,
  ArrowRight
} from "lucide-react";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { MOCK_INVESTMENTS, MOCK_TRANSACTIONS } from "@/lib/mock-data";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export default function ProfitsPage() {
  const [simulationMonths, setSimulationMonths] = useState("12");
  
  const totalIncome = MOCK_TRANSACTIONS
    .filter(t => t.type === 'INCOME')
    .reduce((sum, t) => sum + t.amount, 0);
  
  const totalExpense = MOCK_TRANSACTIONS
    .filter(t => t.type === 'EXPENSE')
    .reduce((sum, t) => sum + t.amount, 0);

  const netProfit = totalIncome - totalExpense;
  const totalInvested = MOCK_INVESTMENTS.reduce((sum, inv) => sum + inv.amount, 0);

  // Group stats by user with time factors
  const userStats = useMemo(() => {
    return MOCK_INVESTMENTS.reduce((acc, inv) => {
      if (!acc[inv.userId]) {
        acc[inv.userId] = { 
          name: inv.userName, 
          total: 0, 
          firstDate: inv.date,
          investments: [] 
        };
      }
      acc[inv.userId].total += inv.amount;
      acc[inv.userId].investments.push(inv);
      if (new Date(inv.date) < new Date(acc[inv.userId].firstDate)) {
        acc[inv.userId].firstDate = inv.date;
      }
      return acc;
    }, {} as Record<string, { name: string, total: number, firstDate: string, investments: any[] }>);
  }, []);

  return (
    <div className="min-h-screen bg-background pb-12">
      <Navbar />
      <main className="container mx-auto px-4 py-8">
        <header className="mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="space-y-1">
            <h1 className="text-3xl font-bold text-primary">Distribution & Simulation</h1>
            <p className="text-muted-foreground">Calcul des gains tenant compte de l'apport et de la durée d'immobilisation.</p>
          </div>
          <div className="flex gap-2">
            <Button variant="outline" className="border-primary text-primary">
              <RefreshCw className="mr-2 h-4 w-4" />
              Réinvestir
            </Button>
            <Button className="bg-primary">
              <HandCoins className="mr-2 h-4 w-4" />
              Distribuer
            </Button>
          </div>
        </header>

        <div className="grid gap-6 md:grid-cols-3 mb-8">
          <Card className="bg-primary text-primary-foreground">
            <CardHeader className="pb-2">
              <CardDescription className="text-primary-foreground/80">Bénéfice Net Actuel</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="text-3xl font-bold">{netProfit.toLocaleString()} FCFA</div>
              <p className="text-xs mt-1 opacity-80 italic">Cumulé depuis le lancement de l'activité.</p>
            </CardContent>
          </Card>

          <Card className="md:col-span-2">
            <CardHeader className="pb-3">
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle className="text-lg flex items-center gap-2">
                    <TrendingUp className="h-5 w-5 text-primary" />
                    Répartition en Temps Réel
                  </CardTitle>
                  <CardDescription>Parts de dividendes basées sur le capital et l'ancienneté.</CardDescription>
                </div>
                <Badge variant="outline" className="bg-primary/5">Cycle: Poules (24 mois)</Badge>
              </div>
            </CardHeader>
            <CardContent>
               <div className="grid sm:grid-cols-2 gap-4">
                 {Object.entries(userStats).map(([id, data]) => {
                   const share = data.total / totalInvested;
                   const earnings = netProfit * share;
                   const monthsActive = Math.floor((new Date().getTime() - new Date(data.firstDate).getTime()) / (1000 * 60 * 60 * 24 * 30.44));
                   
                   return (
                     <div key={id} className="p-4 rounded-xl border bg-white flex flex-col gap-2 hover:shadow-sm transition-shadow">
                        <div className="flex justify-between items-start">
                           <div>
                             <span className="text-sm font-bold block">{data.name}</span>
                             <span className="text-[10px] text-muted-foreground flex items-center gap-1">
                               <Clock className="h-3 w-3" /> Actif depuis {monthsActive} mois
                             </span>
                           </div>
                           <Badge className="bg-primary">{(share * 100).toFixed(1)}%</Badge>
                        </div>
                        
                        <div className="mt-2">
                           <div className="flex justify-between text-[11px] mb-1">
                             <span className="text-muted-foreground">Gain accumulé</span>
                             <span className="font-bold">{earnings.toLocaleString()} F</span>
                           </div>
                           <Progress value={share * 100} className="h-1.5" />
                        </div>
                        
                        <div className="grid grid-cols-2 gap-2 mt-2 pt-2 border-t">
                           <div className="text-center">
                             <p className="text-[9px] uppercase text-muted-foreground font-bold">Investi</p>
                             <p className="text-xs font-bold">{data.total.toLocaleString()} F</p>
                           </div>
                           <div className="text-center border-l">
                             <p className="text-[9px] uppercase text-muted-foreground font-bold">ROI Estimé</p>
                             <p className="text-xs font-bold text-primary">{((earnings/data.total)*100).toFixed(1)}%</p>
                           </div>
                        </div>
                     </div>
                   );
                 })}
               </div>
            </CardContent>
          </Card>
        </div>

        <div className="grid gap-6 lg:grid-cols-3 mb-8">
           <Card className="lg:col-span-1 border-accent/20 bg-accent/5">
              <CardHeader>
                <CardTitle className="text-md flex items-center gap-2">
                  <Calculator className="h-5 w-5 text-accent-foreground" />
                  Simulateur de Projection
                </CardTitle>
                <CardDescription>Estimez les retours futurs selon le cycle de ponte.</CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <Label className="text-xs">Durée totale visée (mois)</Label>
                  <Input 
                    type="number" 
                    value={simulationMonths} 
                    onChange={(e) => setSimulationMonths(e.target.value)}
                    className="h-8"
                  />
                </div>
                
                <div className="p-3 bg-white rounded-lg border text-xs space-y-2">
                  <div className="flex items-start gap-2 text-muted-foreground">
                    <Info className="h-4 w-4 shrink-0 text-accent" />
                    <p>Le cycle standard des poules pondeuses au Cameroun prévoit une ponte optimale dès le 6ème mois.</p>
                  </div>
                  <div className="pt-2 border-t space-y-1">
                    <div className="flex justify-between">
                      <span>Mois 0-5</span>
                      <span className="text-destructive font-bold">Croissance (Charges)</span>
                    </div>
                    <div className="flex justify-between">
                      <span>Mois 6-8</span>
                      <span className="text-accent-foreground font-bold">Début Ponte</span>
                    </div>
                    <div className="flex justify-between">
                      <span>Mois 9+</span>
                      <span className="text-primary font-bold">Pleine Production</span>
                    </div>
                  </div>
                </div>

                <div className="pt-2">
                   <p className="text-xs font-medium mb-2">Projection à {simulationMonths} mois :</p>
                   <div className="p-3 bg-primary text-primary-foreground rounded-lg text-center">
                      <p className="text-lg font-bold">~ 4,500,000 FCFA</p>
                      <p className="text-[10px] opacity-80">Bénéfice total estimé du projet</p>
                   </div>
                </div>
              </CardContent>
           </Card>

           <Card className="lg:col-span-2">
              <CardHeader>
                <CardTitle className="text-lg flex items-center gap-2">
                  <History className="h-5 w-5 text-primary" />
                  Historique & Détails des Apports
                </CardTitle>
                <CardDescription>Détail chronologique des investissements par partenaire.</CardDescription>
              </CardHeader>
              <CardContent>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Investisseur</TableHead>
                      <TableHead>Date d'entrée</TableHead>
                      <TableHead>Type</TableHead>
                      <TableHead className="text-right">Montant</TableHead>
                      <TableHead className="text-center">Impact Gains</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {MOCK_INVESTMENTS.sort((a,b) => new Date(b.date).getTime() - new Date(a.date).getTime()).map((inv) => {
                      const ageInMonths = Math.floor((new Date().getTime() - new Date(inv.date).getTime()) / (1000 * 60 * 60 * 24 * 30.44));
                      const isProductive = ageInMonths >= 6;
                      
                      return (
                        <TableRow key={inv.id}>
                          <TableCell className="font-medium">{inv.userName}</TableCell>
                          <TableCell className="text-xs">
                            {new Date(inv.date).toLocaleDateString('fr-FR')}
                          </TableCell>
                          <TableCell>
                            <Badge variant="outline" className="text-[10px]">FINANCIER</Badge>
                          </TableCell>
                          <TableCell className="text-right font-bold">{inv.amount.toLocaleString()} F</TableCell>
                          <TableCell className="text-center">
                            {isProductive ? (
                              <Badge className="bg-primary/10 text-primary border-primary/20 text-[10px]">En production</Badge>
                            ) : (
                              <Badge variant="secondary" className="text-[10px]">Phase croissance</Badge>
                            )}
                          </TableCell>
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              </CardContent>
           </Card>
        </div>
      </main>
    </div>
  );
}
