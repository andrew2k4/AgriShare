import { Navbar } from "@/components/layout/Navbar";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { 
  PlusCircle, 
  ArrowUpRight, 
  ArrowDownRight, 
  Filter, 
  Download,
  Calendar,
  Layers
} from "lucide-react";
import { MOCK_TRANSACTIONS } from "@/lib/mock-data";
import { Badge } from "@/components/ui/badge";

export default function AccountingPage() {
  const totalIncome = MOCK_TRANSACTIONS
    .filter(t => t.type === 'INCOME')
    .reduce((sum, t) => sum + t.amount, 0);
  
  const totalExpense = MOCK_TRANSACTIONS
    .filter(t => t.type === 'EXPENSE')
    .reduce((sum, t) => sum + t.amount, 0);

  const netProfit = totalIncome - totalExpense;

  return (
    <div className="min-h-screen bg-background pb-12">
      <Navbar />
      <main className="container mx-auto px-4 py-8">
        <header className="mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="space-y-1">
            <h1 className="text-3xl font-bold text-primary">Comptabilité</h1>
            <p className="text-muted-foreground">Suivi des entrées et sorties de fonds.</p>
          </div>
          <div className="flex gap-2">
            <Button variant="outline" className="border-primary text-primary hover:bg-primary/5">
              <Download className="mr-2 h-4 w-4" />
              Exporter (PDF)
            </Button>
            <Button className="bg-primary">
              <PlusCircle className="mr-2 h-4 w-4" />
              Opération
            </Button>
          </div>
        </header>

        <div className="grid gap-6 mb-8 md:grid-cols-3">
          <Card className="bg-white">
            <CardHeader className="pb-2">
              <CardDescription className="flex items-center gap-1">
                <ArrowUpRight className="h-4 w-4 text-primary" />
                Total Revenus
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-primary">{totalIncome.toLocaleString()} FCFA</div>
            </CardContent>
          </Card>
          <Card className="bg-white">
            <CardHeader className="pb-2">
              <CardDescription className="flex items-center gap-1">
                <ArrowDownRight className="h-4 w-4 text-destructive" />
                Total Dépenses
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-destructive">{totalExpense.toLocaleString()} FCFA</div>
            </CardContent>
          </Card>
          <Card className="bg-primary text-primary-foreground">
            <CardHeader className="pb-2">
              <CardDescription className="text-primary-foreground/80">Bénéfice Net</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{netProfit.toLocaleString()} FCFA</div>
            </CardContent>
          </Card>
        </div>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between">
            <div className="space-y-1">
              <CardTitle>Grand Livre</CardTitle>
              <CardDescription>Toutes les transactions financières</CardDescription>
            </div>
            <div className="flex gap-2">
              <Button variant="ghost" size="sm" className="h-8">
                <Calendar className="mr-2 h-3 w-3" />
                Mai 2024
              </Button>
              <Button variant="ghost" size="sm" className="h-8">
                <Filter className="mr-2 h-3 w-3" />
                Filtrer
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Date</TableHead>
                  <TableHead>Catégorie</TableHead>
                  <TableHead>Description</TableHead>
                  <TableHead className="text-right">Montant</TableHead>
                  <TableHead className="text-right">Type</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {MOCK_TRANSACTIONS.map((tx) => (
                  <TableRow key={tx.id}>
                    <TableCell className="text-sm">
                      {new Date(tx.date).toLocaleDateString('fr-FR')}
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <Layers className="h-3 w-3 text-muted-foreground" />
                        <span className="font-medium">{tx.category}</span>
                      </div>
                    </TableCell>
                    <TableCell className="text-muted-foreground text-sm max-w-[200px] truncate">
                      {tx.description}
                    </TableCell>
                    <TableCell className="text-right font-bold">
                      {tx.amount.toLocaleString()} F
                    </TableCell>
                    <TableCell className="text-right">
                      <Badge variant={tx.type === 'INCOME' ? 'default' : 'destructive'} className={tx.type === 'INCOME' ? 'bg-primary' : ''}>
                        {tx.type === 'INCOME' ? 'Entrée' : 'Sortie'}
                      </Badge>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      </main>
    </div>
  );
}
