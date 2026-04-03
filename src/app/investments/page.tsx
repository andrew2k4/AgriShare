import { Navbar } from "@/components/layout/Navbar";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { PlusCircle, Wallet, Users, History } from "lucide-react";
import { MOCK_INVESTMENTS } from "@/lib/mock-data";
import { Badge } from "@/components/ui/badge";

export default function InvestmentsPage() {
  const totalInvested = MOCK_INVESTMENTS.reduce((sum, inv) => sum + inv.amount, 0);

  // Calculate shares per user
  const sharesByUser = MOCK_INVESTMENTS.reduce((acc, inv) => {
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
            <h1 className="text-3xl font-bold text-primary">Gestion des Investissements</h1>
            <p className="text-muted-foreground">Suivez les capitaux et l'évolution des parts dynamiques.</p>
          </div>
          <Button className="w-full md:w-auto bg-primary">
            <PlusCircle className="mr-2 h-4 w-4" />
            Nouvel Investissement
          </Button>
        </header>

        <div className="grid gap-6 lg:grid-cols-3">
          {/* Summary Section */}
          <div className="lg:col-span-1 space-y-6">
            <Card className="bg-primary text-primary-foreground">
              <CardHeader className="pb-2">
                <CardTitle className="text-lg flex items-center gap-2">
                  <Wallet className="h-5 w-5" />
                  Total Investi
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-3xl font-bold">{totalInvested.toLocaleString()} FCFA</div>
                <p className="text-sm opacity-80 mt-1">Capital social de l'activité</p>
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
                <div className="space-y-4">
                  {Object.entries(sharesByUser).map(([id, data]) => {
                    const sharePercent = ((data.total / totalInvested) * 100).toFixed(1);
                    return (
                      <div key={id} className="space-y-2">
                        <div className="flex justify-between text-sm">
                          <span className="font-medium">{data.name}</span>
                          <span className="font-bold">{sharePercent}%</span>
                        </div>
                        <div className="w-full bg-secondary h-2 rounded-full overflow-hidden">
                          <div 
                            className="bg-primary h-full transition-all duration-500" 
                            style={{ width: `${sharePercent}%` }}
                          />
                        </div>
                        <div className="text-xs text-muted-foreground text-right">
                          {data.total.toLocaleString()} FCFA
                        </div>
                      </div>
                    );
                  })}
                </div>
              </CardContent>
            </Card>
          </div>

          {/* History Section */}
          <div className="lg:col-span-2">
            <Card>
              <CardHeader className="flex flex-row items-center justify-between">
                <div>
                  <CardTitle className="text-lg flex items-center gap-2">
                    <History className="h-5 w-5 text-primary" />
                    Historique des apports
                  </CardTitle>
                </div>
              </CardHeader>
              <CardContent>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Date</TableHead>
                      <TableHead>Investisseur</TableHead>
                      <TableHead className="text-right">Montant</TableHead>
                      <TableHead className="text-right">Part ajoutée</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {MOCK_INVESTMENTS.sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime()).map((inv) => (
                      <TableRow key={inv.id}>
                        <TableCell className="text-sm">
                          {new Date(inv.date).toLocaleDateString('fr-FR')}
                        </TableCell>
                        <TableCell className="font-medium">{inv.userName}</TableCell>
                        <TableCell className="text-right font-bold">
                          {inv.amount.toLocaleString()} F
                        </TableCell>
                        <TableCell className="text-right">
                           <Badge variant="outline" className="bg-primary/5 text-primary">
                             +{(inv.amount / totalInvested * 100).toFixed(1)}%
                           </Badge>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          </div>
        </div>
      </main>
    </div>
  );
}
