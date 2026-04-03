import { Navbar } from "@/components/layout/Navbar";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { 
  Activity, 
  Egg, 
  Skull, 
  Bird, 
  PlusCircle,
  TrendingUp,
  AlertCircle
} from "lucide-react";
import { MOCK_PRODUCTION } from "@/lib/mock-data";
import { ProductionChart } from "@/components/dashboard/ProductionChart";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";

export default function ProductionPage() {
  const latest = MOCK_PRODUCTION[MOCK_PRODUCTION.length - 1];
  const avgPonte = ((latest.eggsProduced / latest.chickenCount) * 100).toFixed(1);

  return (
    <div className="min-h-screen bg-background pb-12">
      <Navbar />
      <main className="container mx-auto px-4 py-8">
        <header className="mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="space-y-1">
            <h1 className="text-3xl font-bold text-primary">Suivi de Production</h1>
            <p className="text-muted-foreground">Données journalières de ponte et mortalité.</p>
          </div>
          <Button className="bg-primary">
            <PlusCircle className="mr-2 h-4 w-4" />
            Saisie du Jour
          </Button>
        </header>

        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-4 mb-8">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardDescription>Effectif Total</CardDescription>
              <Bird className="h-4 w-4 text-primary" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{latest.chickenCount} Poules</div>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardDescription>Dernière Ponte</CardDescription>
              <Egg className="h-4 w-4 text-accent" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{latest.eggsProduced} Œufs</div>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardDescription>Taux de Ponte</CardDescription>
              <TrendingUp className="h-4 w-4 text-primary" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{avgPonte}%</div>
            </CardContent>
          </Card>
          <Card className={latest.mortality > 2 ? "border-destructive bg-destructive/5" : ""}>
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardDescription>Mortalité</CardDescription>
              <Skull className={latest.mortality > 2 ? "h-4 w-4 text-destructive" : "h-4 w-4 text-muted-foreground"} />
            </CardHeader>
            <CardContent>
              <div className={`text-2xl font-bold ${latest.mortality > 2 ? "text-destructive" : ""}`}>
                {latest.mortality} Décès
              </div>
            </CardContent>
          </Card>
        </div>

        <div className="grid gap-6 lg:grid-cols-3">
          <div className="lg:col-span-2">
            <ProductionChart />
          </div>
          
          <Card>
            <CardHeader>
              <CardTitle className="text-lg flex items-center gap-2">
                <AlertCircle className="h-5 w-5 text-accent" />
                Alertes & Seuils
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="p-3 rounded-lg bg-accent/10 border border-accent/20">
                <p className="text-sm font-medium">Baisse de production légère</p>
                <p className="text-xs text-muted-foreground mt-1">-5% sur les 3 derniers jours. Vérifier la qualité de l'aliment.</p>
              </div>
              <div className="p-3 rounded-lg bg-primary/10 border border-primary/20">
                <p className="text-sm font-medium">Mortalité stable</p>
                <p className="text-xs text-muted-foreground mt-1">Inférieure à 0.5% par jour. Routine sanitaire OK.</p>
              </div>
            </CardContent>
          </Card>
        </div>

        <Card className="mt-8">
          <CardHeader>
            <CardTitle>Registre Historique</CardTitle>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Date</TableHead>
                  <TableHead className="text-right">Effectif</TableHead>
                  <TableHead className="text-right">Œufs Produits</TableHead>
                  <TableHead className="text-right">Taux</TableHead>
                  <TableHead className="text-right">Mortalité</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {MOCK_PRODUCTION.sort((a,b) => new Date(b.date).getTime() - new Date(a.date).getTime()).map((record) => (
                  <TableRow key={record.id}>
                    <TableCell>{new Date(record.date).toLocaleDateString('fr-FR')}</TableCell>
                    <TableCell className="text-right">{record.chickenCount}</TableCell>
                    <TableCell className="text-right font-medium">{record.eggsProduced}</TableCell>
                    <TableCell className="text-right">
                      {((record.eggsProduced / record.chickenCount) * 100).toFixed(1)}%
                    </TableCell>
                    <TableCell className={`text-right ${record.mortality > 2 ? "text-destructive font-bold" : ""}`}>
                      {record.mortality}
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
