
"use client";

import { useState } from "react";
import { Navbar } from "@/components/layout/Navbar";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { PlusCircle, Wallet, Users, History, Edit2, Save, X } from "lucide-react";
import { MOCK_INVESTMENTS } from "@/lib/mock-data";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger, DialogFooter } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { toast } from "@/hooks/use-toast";

export default function InvestmentsPage() {
  const [investments, setInvestments] = useState(MOCK_INVESTMENTS);
  const [editingInvestment, setEditingInvestment] = useState<any>(null);

  const totalInvested = investments.reduce((sum, inv) => sum + inv.amount, 0);

  const handleUpdateInvestment = () => {
    if (!editingInvestment) return;
    
    setInvestments(prev => 
      prev.map(inv => inv.id === editingInvestment.id ? editingInvestment : inv)
    );
    
    toast({
      title: "Investissement mis à jour",
      description: `L'apport de ${editingInvestment.userName} a été modifié.`,
    });
    setEditingInvestment(null);
  };

  // Calculate shares per user
  const sharesByUser = investments.reduce((acc, inv) => {
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
            <p className="text-muted-foreground">Suivez les capitaux et modifiez les parts si nécessaire.</p>
          </div>
          <Button className="w-full md:w-auto bg-primary">
            <PlusCircle className="mr-2 h-4 w-4" />
            Nouvel Investissement
          </Button>
        </header>

        <div className="grid gap-6 lg:grid-cols-3">
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
                      </div>
                    );
                  })}
                </div>
              </CardContent>
            </Card>
          </div>

          <div className="lg:col-span-2">
            <Card>
              <CardHeader>
                <CardTitle className="text-lg flex items-center gap-2">
                  <History className="h-5 w-5 text-primary" />
                  Historique des apports
                </CardTitle>
              </CardHeader>
              <CardContent>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Date</TableHead>
                      <TableHead>Investisseur</TableHead>
                      <TableHead className="text-right">Montant</TableHead>
                      <TableHead className="text-right">Action</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {[...investments].sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime()).map((inv) => (
                      <TableRow key={inv.id}>
                        <TableCell className="text-sm">
                          {new Date(inv.date).toLocaleDateString('fr-FR')}
                        </TableCell>
                        <TableCell className="font-medium">{inv.userName}</TableCell>
                        <TableCell className="text-right font-bold">
                          {inv.amount.toLocaleString()} F
                        </TableCell>
                        <TableCell className="text-right">
                          <Dialog>
                            <DialogTrigger asChild>
                              <Button variant="ghost" size="sm" onClick={() => setEditingInvestment(inv)}>
                                <Edit2 className="h-4 w-4" />
                              </Button>
                            </DialogTrigger>
                            <DialogContent>
                              <DialogHeader>
                                <DialogTitle>Modifier l'investissement</DialogTitle>
                              </DialogHeader>
                              <div className="space-y-4 py-4">
                                <div className="space-y-2">
                                  <Label>Nom de l'investisseur</Label>
                                  <Input 
                                    value={editingInvestment?.userName || ""} 
                                    onChange={(e) => setEditingInvestment({...editingInvestment, userName: e.target.value})}
                                  />
                                </div>
                                <div className="space-y-2">
                                  <Label>Montant (FCFA)</Label>
                                  <Input 
                                    type="number"
                                    value={editingInvestment?.amount || 0} 
                                    onChange={(e) => setEditingInvestment({...editingInvestment, amount: Number(e.target.value)})}
                                  />
                                </div>
                              </div>
                              <DialogFooter>
                                <Button className="bg-primary" onClick={handleUpdateInvestment}>Enregistrer</Button>
                              </DialogFooter>
                            </DialogContent>
                          </Dialog>
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
