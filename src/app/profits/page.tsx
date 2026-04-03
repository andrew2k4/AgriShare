import { Navbar } from "@/components/layout/Navbar";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { PiggyBank, RefreshCw, HandCoins, History, TrendingUp } from "lucide-react";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { MOCK_INVESTMENTS, MOCK_TRANSACTIONS } from "@/lib/mock-data";

export default function ProfitsPage() {
  const totalIncome = MOCK_TRANSACTIONS
    .filter(t => t.type === 'INCOME')
    .reduce((sum, t) => sum + t.amount, 0);
  
  const totalExpense = MOCK_TRANSACTIONS
    .filter(t => t.type === 'EXPENSE')
    .reduce((sum, t) => sum + t.amount, 0);

  const netProfit = totalIncome - totalExpense;
  const totalInvested = MOCK_INVESTMENTS.reduce((sum, inv) => sum + inv.amount, 0);

  // Group by user
  const userStats = MOCK_INVESTMENTS.reduce((acc, inv) => {
    if (!acc[inv.userId]) {
      acc[inv.userId] = { name: inv.userName, total: 0 };
    }
    acc[inv.userId].total += inv.amount;
    return acc;
  }, {} as Record<string, { name: string, total: number }>);

  return (
    <div className="min-h-screen bg-background pb-12">
      <Navbar />
      <main className="container mx-auto px-4 py-8">
        <header className="mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="space-y-1">
            <h1 className="text-3xl font-bold text-primary">Distribution des Bénéfices</h1>
            <p className="text-muted-foreground">Calcul automatique basé sur vos parts de capital.</p>
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
              <CardDescription className="text-primary-foreground/80">Bénéfice à distribuer</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="text-3xl font-bold">{netProfit.toLocaleString()} FCFA</div>
              <p className="text-xs mt-1 opacity-80 italic">Calculé après déduction des charges opérationnelles.</p>
            </CardContent>
          </Card>

          <Card className="md:col-span-2">
            <CardHeader>
              <CardTitle className="text-lg flex items-center gap-2">
                <TrendingUp className="h-5 w-5 text-primary" />
                Performance des Parts
              </CardTitle>
            </CardHeader>
            <CardContent>
               <div className="grid sm:grid-cols-2 gap-6">
                 {Object.entries(userStats).map(([id, data]) => {
                   const share = data.total / totalInvested;
                   const earnings = netProfit * share;
                   return (
                     <div key={id} className="p-4 rounded-xl border bg-primary/5 flex flex-col gap-1">
                        <span className="text-sm font-medium text-muted-foreground">{data.name}</span>
                        <div className="flex justify-between items-end">
                           <span className="text-xl font-bold text-primary">{(share * 100).toFixed(1)}%</span>
                           <span className="text-lg font-bold text-accent-foreground">{earnings.toLocaleString()} F</span>
                        </div>
                        <div className="text-[10px] text-muted-foreground mt-2 uppercase tracking-wider font-bold">Dividende estimé</div>
                     </div>
                   );
                 })}
               </div>
            </CardContent>
          </Card>
        </div>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <History className="h-5 w-5 text-primary" />
              Historique des distributions
            </CardTitle>
            <CardDescription>Archive des paiements effectués aux investisseurs.</CardDescription>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Période</TableHead>
                  <TableHead>Investisseur</TableHead>
                  <TableHead className="text-right">Montant</TableHead>
                  <TableHead className="text-right">Mode</TableHead>
                  <TableHead className="text-right">Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <TableRow>
                  <TableCell>Avril 2024</TableCell>
                  <TableCell>Jean-Pierre Ndongo</TableCell>
                  <TableCell className="text-right font-bold">210,000 F</TableCell>
                  <TableCell className="text-right">Mobile Money</TableCell>
                  <TableCell className="text-right">
                    <span className="text-xs font-bold text-primary bg-primary/10 px-2 py-1 rounded">Payé</span>
                  </TableCell>
                </TableRow>
                <TableRow>
                  <TableCell>Avril 2024</TableCell>
                  <TableCell>Marie Tchakoute</TableCell>
                  <TableCell className="text-right font-bold">90,000 F</TableCell>
                  <TableCell className="text-right">Espèces</TableCell>
                  <TableCell className="text-right">
                    <span className="text-xs font-bold text-primary bg-primary/10 px-2 py-1 rounded">Payé</span>
                  </TableCell>
                </TableRow>
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      </main>
    </div>
  );
}
