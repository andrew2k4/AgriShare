
"use client";

import { useState, useMemo } from "react";
import { Navbar } from "@/components/layout/Navbar";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { simulateProject, type ProjectSimulationOutput } from "@/ai/flows/project-simulator";
import { Sparkles, ArrowRight, CheckCircle2, AlertCircle, TrendingUp, Info, Plus, Trash2, Save } from "lucide-react";
import { Progress } from "@/components/ui/progress";
import { useFirestore, useUser } from "@/firebase";
import { collection, doc } from "firebase/firestore";
import { addDocumentNonBlocking, setDocumentNonBlocking } from "@/firebase/non-blocking-updates";
import { useRouter } from "next/navigation";
import { toast } from "@/hooks/use-toast";

interface InitialInvestor {
  name: string;
  amount: string;
}

export default function NewProjectPage() {
  const router = useRouter();
  const { db } = useFirestore() as any;
  const { user } = useUser();
  const [step, setStep] = useState(1);
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    projectName: "",
    domain: "",
    type: "",
    budget: "",
    landSize: "",
    hasStructures: false,
    description: "",
  });
  const [simulation, setSimulation] = useState<ProjectSimulationOutput | null>(null);
  const [investors, setInvestors] = useState<InitialInvestor[]>([{ name: "", amount: "" }]);

  const totalInitialCapital = useMemo(() => {
    return investors.reduce((sum, inv) => sum + (Number(inv.amount) || 0), 0);
  }, [investors]);

  const handleSimulate = async () => {
    setLoading(true);
    try {
      const result = await simulateProject({
        domain: formData.domain as 'LIVESTOCK' | 'AGRICULTURE',
        type: formData.type,
        budget: Number(formData.budget),
        landSize: Number(formData.landSize),
        hasStructures: formData.hasStructures,
        description: formData.description,
      });
      setSimulation(result);
      setStep(3);
    } catch (error) {
      console.error("Simulation failed", error);
    } finally {
      setLoading(false);
    }
  };

  const addInvestor = () => setInvestors([...investors, { name: "", amount: "" }]);
  
  const removeInvestor = (index: number) => {
    if (investors.length > 1) {
      setInvestors(investors.filter((_, i) => i !== index));
    }
  };

  const updateInvestor = (index: number, field: keyof InitialInvestor, value: string) => {
    const newInvestors = [...investors];
    newInvestors[index][field] = value;
    setInvestors(newInvestors);
  };

  const handleInitializeProject = () => {
    if (!user || !db) return;
    setLoading(true);

    const projectRef = doc(collection(db, "projects"));
    const projectId = projectRef.id;

    // Create Project Document
    setDocumentNonBlocking(projectRef, {
      id: projectId,
      name: formData.projectName,
      domain: formData.domain,
      type: formData.type,
      status: "ACTIVE",
      resources: {
        budget: Number(formData.budget),
        landSize: Number(formData.landSize),
        hasStructures: formData.hasStructures,
        description: formData.description,
      },
      createdBy: user.uid,
      createdAt: new Date().toISOString(),
    }, { merge: true });

    // Create Initial Investments
    investors.forEach((inv) => {
      const invRef = collection(db, "projects", projectId, "investments");
      addDocumentNonBlocking(invRef, {
        projectId,
        investorName: inv.name,
        amount: Number(inv.amount),
        date: new Date().toISOString(),
      });
    });

    toast({
      title: "Projet Initialisé !",
      description: `Le projet ${formData.projectName} a été créé avec succès.`,
    });

    router.push("/");
  };

  return (
    <div className="min-h-screen bg-background pb-12">
      <Navbar />
      <main className="container mx-auto px-4 py-8 max-w-3xl">
        <header className="mb-8 text-center">
          <h1 className="text-3xl font-bold text-primary">Nouveau Projet AgriShare</h1>
          <p className="text-muted-foreground mt-2">Initialisez votre activité avec une simulation IA complète.</p>
          <div className="mt-6 flex justify-center">
            <Progress value={(step / 4) * 100} className="w-64 h-2" />
          </div>
        </header>

        {step === 1 && (
          <Card className="animate-in fade-in slide-in-from-bottom-4 duration-500">
            <CardHeader>
              <CardTitle>Domaine & Type</CardTitle>
              <CardDescription>Choisissez ce que vous souhaitez produire.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="space-y-2">
                <Label>Domaine d'activité</Label>
                <Select onValueChange={(v) => setFormData({ ...formData, domain: v })}>
                  <SelectTrigger>
                    <SelectValue placeholder="Sélectionner un domaine" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="LIVESTOCK">Élevage (Livestock)</SelectItem>
                    <SelectItem value="AGRICULTURE">Agriculture</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              {formData.domain && (
                <div className="space-y-2 animate-in fade-in">
                  <Label>Type de culture ou élevage</Label>
                  <Select onValueChange={(v) => setFormData({ ...formData, type: v })}>
                    <SelectTrigger>
                      <SelectValue placeholder="Sélectionner le type" />
                    </SelectTrigger>
                    <SelectContent>
                      {formData.domain === "LIVESTOCK" ? (
                        <>
                          <SelectItem value="Poules Pondeuses">Poules Pondeuses</SelectItem>
                          <SelectItem value="Porcs">Porcs</SelectItem>
                          <SelectItem value="Lapins">Lapins</SelectItem>
                        </>
                      ) : (
                        <>
                          <SelectItem value="Maïs">Maïs</SelectItem>
                          <SelectItem value="Haricots">Haricots</SelectItem>
                          <SelectItem value="Soja">Soja</SelectItem>
                        </>
                      )}
                    </SelectContent>
                  </Select>
                </div>
              )}

              <Button 
                disabled={!formData.type} 
                onClick={() => setStep(2)} 
                className="w-full bg-primary"
              >
                Suivant <ArrowRight className="ml-2 h-4 w-4" />
              </Button>
            </CardContent>
          </Card>
        )}

        {step === 2 && (
          <Card className="animate-in fade-in slide-in-from-bottom-4 duration-500">
            <CardHeader>
              <CardTitle>Ressources & Moyens</CardTitle>
              <CardDescription>Dites-nous ce que vous avez à disposition.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>Budget disponible (FCFA)</Label>
                  <Input 
                    type="number" 
                    placeholder="Ex: 5000000" 
                    value={formData.budget}
                    onChange={(e) => setFormData({ ...formData, budget: e.target.value })}
                  />
                </div>
                <div className="space-y-2">
                  <Label>Taille du terrain (m²)</Label>
                  <Input 
                    type="number" 
                    placeholder="Ex: 1000" 
                    value={formData.landSize}
                    onChange={(e) => setFormData({ ...formData, landSize: e.target.value })}
                  />
                </div>
              </div>

              <div className="flex items-center justify-between p-4 border rounded-lg">
                <div className="space-y-0.5">
                  <Label>Structures existantes</Label>
                  <p className="text-xs text-muted-foreground">Bâtiments, fermes ou clôtures déjà présents.</p>
                </div>
                <Switch 
                  checked={formData.hasStructures}
                  onCheckedChange={(v) => setFormData({ ...formData, hasStructures: v })}
                />
              </div>

              <div className="space-y-2">
                <Label>Notes additionnelles (Optionnel)</Label>
                <Input 
                  placeholder="Ex: Terrain avec accès eau, proche de Douala..." 
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                />
              </div>

              <div className="flex gap-4">
                <Button variant="outline" onClick={() => setStep(1)} className="flex-1">Retour</Button>
                <Button 
                  disabled={!formData.budget || !formData.landSize || loading} 
                  onClick={handleSimulate} 
                  className="flex-1 bg-primary"
                >
                  {loading ? "Simulation..." : "Lancer la Simulation"}
                  <Sparkles className="ml-2 h-4 w-4" />
                </Button>
              </div>
            </CardContent>
          </Card>
        )}

        {step === 3 && simulation && (
          <div className="space-y-6 animate-in zoom-in duration-500">
            <Card className={simulation.isFeasible ? "border-primary bg-primary/5" : "border-destructive bg-destructive/5"}>
              <CardHeader className="text-center">
                <div className="mx-auto h-12 w-12 rounded-full flex items-center justify-center mb-4">
                  {simulation.isFeasible ? (
                    <CheckCircle2 className="h-10 w-10 text-primary" />
                  ) : (
                    <AlertCircle className="h-10 w-10 text-destructive" />
                  )}
                </div>
                <CardTitle className="text-2xl">
                  {simulation.isFeasible ? "Projet Viable !" : "Projet non réalisable"}
                </CardTitle>
                <CardDescription>Analyse de faisabilité AgriShare IA</CardDescription>
              </CardHeader>
              <CardContent className="space-y-6">
                <div className="grid grid-cols-3 gap-4 text-center">
                  <div className="p-3 bg-white rounded-lg shadow-sm">
                    <p className="text-[10px] text-muted-foreground uppercase font-bold">Investissement</p>
                    <p className="font-bold text-primary">{simulation.estimatedStartupCost.toLocaleString()} F</p>
                  </div>
                  <div className="p-3 bg-white rounded-lg shadow-sm">
                    <p className="text-[10px] text-muted-foreground uppercase font-bold">Revenu Mensuel</p>
                    <p className="font-bold text-accent-foreground">{simulation.estimatedMonthlyRevenue.toLocaleString()} F</p>
                  </div>
                  <div className="p-3 bg-white rounded-lg shadow-sm">
                    <p className="text-[10px] text-muted-foreground uppercase font-bold">Rentabilité (ROI)</p>
                    <p className="font-bold text-primary">{simulation.roiPeriodMonths} Mois</p>
                  </div>
                </div>

                <div className="space-y-4">
                  <div className="flex gap-2">
                    <Info className="h-5 w-5 text-primary shrink-0" />
                    <p className="text-sm leading-relaxed italic">{simulation.feasibilityReport}</p>
                  </div>

                  <div className="space-y-2">
                    <h4 className="text-sm font-bold flex items-center gap-2">
                      <TrendingUp className="h-4 w-4 text-primary" />
                      Conseils de l'expert
                    </h4>
                    <ul className="grid grid-cols-1 gap-2">
                      {simulation.recommendations.map((rec, i) => (
                        <li key={i} className="text-xs bg-white p-2 rounded border border-primary/10 flex items-start gap-2">
                          <div className="h-1.5 w-1.5 rounded-full bg-primary mt-1 shrink-0" />
                          {rec}
                        </li>
                      ))}
                    </ul>
                  </div>
                </div>

                <div className="flex gap-4 pt-4">
                  <Button variant="outline" onClick={() => setStep(2)} className="flex-1">Ajuster mes moyens</Button>
                  {simulation.isFeasible && (
                    <Button onClick={() => setStep(4)} className="flex-1 bg-primary">
                      Ouvrir mon Projet
                    </Button>
                  )}
                </div>
              </CardContent>
            </Card>
          </div>
        )}

        {step === 4 && (
          <Card className="animate-in fade-in slide-in-from-bottom-4 duration-500">
            <CardHeader>
              <CardTitle>Configuration du Capital</CardTitle>
              <CardDescription>Nommez votre projet et ajoutez vos partenaires.</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="space-y-2">
                <Label>Nom du Projet</Label>
                <Input 
                  placeholder="Ex: Ferme Avicole du Sud" 
                  value={formData.projectName}
                  onChange={(e) => setFormData({ ...formData, projectName: e.target.value })}
                />
              </div>

              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <Label>Liste des Investisseurs</Label>
                  <Button variant="ghost" size="sm" onClick={addInvestor} className="text-primary h-8 px-2">
                    <Plus className="h-4 w-4 mr-1" /> Ajouter
                  </Button>
                </div>
                
                {investors.map((inv, idx) => (
                  <div key={idx} className="flex gap-2 items-end animate-in fade-in slide-in-from-right-2">
                    <div className="flex-1 space-y-1">
                      <Label className="text-[10px] text-muted-foreground">Nom complet</Label>
                      <Input 
                        placeholder="Nom" 
                        value={inv.name}
                        onChange={(e) => updateInvestor(idx, "name", e.target.value)}
                      />
                    </div>
                    <div className="flex-1 space-y-1">
                      <Label className="text-[10px] text-muted-foreground">Apport (FCFA)</Label>
                      <Input 
                        type="number" 
                        placeholder="Montant" 
                        value={inv.amount}
                        onChange={(e) => updateInvestor(idx, "amount", e.target.value)}
                      />
                    </div>
                    {investors.length > 1 && (
                      <Button variant="ghost" size="icon" onClick={() => removeInvestor(idx)} className="text-destructive h-10 w-10">
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    )}
                  </div>
                ))}

                <div className="p-4 bg-primary/5 rounded-lg border border-primary/20">
                  <div className="flex justify-between items-center text-sm">
                    <span className="font-medium">Capital Total Initial :</span>
                    <span className="font-bold text-primary">{totalInitialCapital.toLocaleString()} FCFA</span>
                  </div>
                  <p className="text-[10px] text-muted-foreground mt-1">
                    Les parts de chacun seront calculées automatiquement sur cette base.
                  </p>
                </div>
              </div>

              <div className="flex gap-4">
                <Button variant="outline" onClick={() => setStep(3)} className="flex-1">Retour Simulation</Button>
                <Button 
                  disabled={!formData.projectName || totalInitialCapital === 0 || loading} 
                  onClick={handleInitializeProject} 
                  className="flex-1 bg-primary"
                >
                  {loading ? "Initialisation..." : "Créer le Projet"}
                  <Save className="ml-2 h-4 w-4" />
                </Button>
              </div>
            </CardContent>
          </Card>
        )}
      </main>
    </div>
  );
}
