"use client";

import { useState } from "react";
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
  Layers,
  ArrowRightLeft,
  ChevronRight
} from "lucide-react";
import { MOCK_TRANSACTIONS } from "@/lib/mock-data";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger, DialogFooter } from "@/components/ui/dialog";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

const CATEGORIES = {
  INCOME: ["Ventes œufs", "Ventes poules", "Autres revenus"],
  EXPENSE: ["Aliments", "Salaires", "Vaccins", "Matériel", "Transport", "Autres charges"]
};

export default function AccountingPage() {
  const [transactions, setTransactions] = useState(MOCK_TRANSACTIONS);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [newTx, setNewTx] = useState({
    type: "EXPENSE",
    category: "",
    amount: "",
    description: "",
    date: new Date().toISOString().split('T')[0]
  });

  const totalIncome = transactions
    .filter(t => t.type === 'INCOME')
    .reduce((sum, t) => sum + t.amount, 0);
  
  const totalExpense = transactions
    .filter(t => t.type === 'EXPENSE')
    .reduce((sum, t) => sum + t.amount, 0);

  const netProfit = totalIncome - totalExpense;

  const handleAddTransaction = () => {
    const tx = {
      id: Math.random().toString(36).substr(2, 9),
      ...newTx,
      amount: Number(newTx.amount),
      date: newTx.date
    };
    setTransactions([tx as any, ...transactions]);
    setIsDialogOpen(false);
  };

  return (
    <div className="min-h-screen bg-background pb-12">
      <Navbar />
      <main className="container mx-auto px-4 py-8">
        <header className="mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="space-y-1">
            <h1 className="text-3xl font-bold text-primary">Comptabilité & Rubriques</h1>
            <p className="text-muted-foreground">Suivi financier organisé par catégories d'activité.</p>
          </div>
          <div className="flex gap-2">
            <Button variant="outline" className="border-primary text-primary hover:bg-primary/5">
              <Download className="mr-2 h-4 w-4" />
              Rapport
            </Button>
            <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
              <DialogTrigger asChild>
                <Button className="bg-primary">
                  <PlusCircle className="mr-2 h-4 w-4" />
                  Nouvelle opération
                </Button>
              </DialogTrigger>
              <DialogContent>
                <DialogHeader>
                  <DialogTitle>Enregistrer une opération</DialogTitle>
                </DialogHeader>
                <div className="space-y-4 py-4">
                  <div className="space-y-2">
                    <Label>Type d'opération</Label>
                    <Select onValueChange={(v) => setNewTx({...newTx, type: v, category: ""})}>
                      <SelectTrigger>
                        <SelectValue placeholder="Choisir le type" />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="INCOME">Entrée (Revenu)</SelectItem>
                        <SelectItem value="EXPENSE">Sortie (Charge)</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="space-y-2">
                    <Label>Rubrique / Catégorie</Label>
                    <Select onValueChange={(v) => setNewTx({...newTx, category: v})}>
                      <SelectTrigger>
                        <SelectValue placeholder="Choisir une rubrique" />
                      </SelectTrigger>
                      <SelectContent>
                        {CATEGORIES[newTx.type as keyof typeof CATEGORIES].map(cat => (
                          <SelectItem key={cat} value={cat}>{cat}</SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="space-y-2">
                    <Label>Montant (FCFA)</Label>
                    <Input 
                      type="number" 
                      placeholder="0" 
                      value={newTx.amount}
                      onChange={(e) => setNewTx({...newTx, amount: e.target.value})}
                    />
                  </div>
                  <div className="space-y-2">
                    <Label>Description</Label>
                    <Input 
                      placeholder="Détails de l'opération..." 
                      value={newTx.description}
                      onChange={(e) => setNewTx({...newTx, description: e.target.value})}
                    />
                  </div>
                </div>
                <DialogFooter>
                  <Button className="bg-primary w-full" onClick={handleAddTransaction}>Valider l'opération</Button>
                </DialogFooter>
              </DialogContent>
            </Dialog>
          </div>
        </header>

        <div className="grid gap-6 mb-8 md:grid-cols-3">
          <Card className="bg-white border-l-4 border-l-primary">
            <CardHeader className="pb-2">
              <CardDescription className="flex items-center gap-1">
                <ArrowUpRight className="h-4 w-4 text-primary" />
                Total Entrées
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-primary">{totalIncome.toLocaleString()} FCFA</div>
            </CardContent>
          </Card>
          <Card className="bg-white border-l-4 border-l-destructive">
            <CardHeader className="pb-2">
              <CardDescription className="flex items-center gap-1">
                <ArrowDownRight className="h-4 w-4 text-destructive" />
                Total Sorties
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-destructive">{totalExpense.toLocaleString()} FCFA</div>
            </CardContent>
          </Card>
          <Card className="bg-primary text-primary-foreground">
            <CardHeader className="pb-2">
              <CardDescription className="text-primary-foreground/80">Solde Net</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{netProfit.toLocaleString()} FCFA</div>
            </CardContent>
          </Card>
        </div>

        <Tabs defaultValue="all" className="space-y-6">
          <div className="flex items-center justify-between">
            <TabsList>
              <TabsTrigger value="all">Tout</TabsTrigger>
              <TabsTrigger value="income">Entrées</TabsTrigger>
              <TabsTrigger value="expense">Sorties</TabsTrigger>
            </TabsList>
            <Button variant="ghost" size="sm">
              <Calendar className="mr-2 h-4 w-4" />
              Filtrer par date
            </Button>
          </div>

          <TabsContent value="all">
            <Card>
              <CardHeader>
                <CardTitle className="text-lg">Journal des Opérations</CardTitle>
                <CardDescription>Historique complet rubriqué</CardDescription>
              </CardHeader>
              <CardContent>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Date</TableHead>
                      <TableHead>Rubrique</TableHead>
                      <TableHead>Description</TableHead>
                      <TableHead className="text-right">Montant</TableHead>
                      <TableHead className="text-right">Status</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {transactions.map((tx) => (
                      <TableRow key={tx.id}>
                        <TableCell className="text-sm">
                          {new Date(tx.date).toLocaleDateString('fr-FR')}
                        </TableCell>
                        <TableCell>
                          <Badge variant="secondary" className="font-medium">
                            {tx.category}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-muted-foreground text-sm max-w-[200px] truncate">
                          {tx.description}
                        </TableCell>
                        <TableCell className={`text-right font-bold ${tx.type === 'INCOME' ? 'text-primary' : 'text-destructive'}`}>
                          {tx.type === 'INCOME' ? '+' : '-'}{tx.amount.toLocaleString()} F
                        </TableCell>
                        <TableCell className="text-right">
                          <div className={`h-2 w-2 rounded-full ml-auto ${tx.type === 'INCOME' ? 'bg-primary' : 'bg-destructive'}`} />
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="income">
            <div className="grid gap-6 md:grid-cols-2">
              {CATEGORIES.INCOME.map(cat => {
                const total = transactions.filter(t => t.category === cat).reduce((s, t) => s + t.amount, 0);
                return (
                  <Card key={cat}>
                    <CardHeader className="pb-2">
                      <CardTitle className="text-sm font-medium">{cat}</CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="text-xl font-bold text-primary">{total.toLocaleString()} FCFA</div>
                    </CardContent>
                  </Card>
                );
              })}
            </div>
          </TabsContent>

          <TabsContent value="expense">
             <div className="grid gap-6 md:grid-cols-3">
              {CATEGORIES.EXPENSE.map(cat => {
                const total = transactions.filter(t => t.category === cat).reduce((s, t) => s + t.amount, 0);
                return (
                  <Card key={cat}>
                    <CardHeader className="pb-2">
                      <CardTitle className="text-sm font-medium">{cat}</CardTitle>
                    </CardHeader>
                    <CardContent>
                      <div className="text-xl font-bold text-destructive">{total.toLocaleString()} FCFA</div>
                    </CardContent>
                  </Card>
                );
              })}
            </div>
          </TabsContent>
        </Tabs>
      </main>
    </div>
  );
}
