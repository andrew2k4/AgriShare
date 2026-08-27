"use client";

import { useState } from "react";
import { Sparkles, BrainCircuit, RefreshCw, AlertTriangle, Lightbulb } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { farmPerformanceAdvisor, type FarmPerformanceAdvisorOutput } from "@/ai/flows/farm-performance-advisor";
import { MOCK_PRODUCTION, MOCK_TRANSACTIONS } from "@/lib/mock-data";
import { Skeleton } from "@/components/ui/skeleton";

export function AdvisorPanel() {
  const [advice, setAdvice] = useState<FarmPerformanceAdvisorOutput | null>(null);
  const [loading, setLoading] = useState(false);

  const getAdvice = async () => {
    setLoading(true);
    try {
      // Prepare data for the flow
      const eggProductionData = MOCK_PRODUCTION.map(p => ({
        date: new Date(p.date).toISOString(),
        eggsProduced: p.eggsProduced
      }));
      
      const mortalityData = MOCK_PRODUCTION.map(p => ({
        date: new Date(p.date).toISOString(),
        mortalityCount: p.mortality
      }));

      const expenseData = MOCK_TRANSACTIONS
        .filter(t => t.type === 'EXPENSE')
        .map(t => ({
          date: new Date(t.date).toISOString(),
          category: t.category,
          amount: t.amount
        }));

      const revenueData = MOCK_TRANSACTIONS
        .filter(t => t.type === 'INCOME')
        .map(t => ({
          date: new Date(t.date).toISOString(),
          category: t.category,
          amount: t.amount
        }));

      const result = await farmPerformanceAdvisor({
        startDate: new Date('2024-05-15').toISOString(),
        endDate: new Date().toISOString(),
        currentChickenCount: 500,
        eggProductionData,
        mortalityData,
        expenseData,
        revenueData
      });
      setAdvice(result);
    } catch (error) {
      console.error("Failed to fetch advice", error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card className="border-primary/20 bg-primary/5">
      <CardHeader className="flex flex-row items-center justify-between pb-2">
        <div className="space-y-1">
          <CardTitle className="text-primary flex items-center gap-2">
            <BrainCircuit className="h-5 w-5" />
            Conseiller Intelligent AgriShare
          </CardTitle>
          <CardDescription>
            Analyse de vos données de production et finances par IA.
          </CardDescription>
        </div>
        <Button 
          onClick={getAdvice} 
          disabled={loading}
          variant="outline"
          className="bg-white hover:bg-primary/10 border-primary/20 text-primary"
        >
          {loading ? (
            <RefreshCw className="mr-2 h-4 w-4 animate-spin" />
          ) : (
            <Sparkles className="mr-2 h-4 w-4" />
          )}
          {advice ? "Recalculer" : "Analyser les données"}
        </Button>
      </CardHeader>
      <CardContent>
        {!advice && !loading && (
          <div className="py-6 text-center text-muted-foreground italic">
            Cliquez sur "Analyser les données" pour recevoir des conseils personnalisés.
          </div>
        )}

        {loading && (
          <div className="space-y-4 py-4">
            <Skeleton className="h-4 w-3/4" />
            <Skeleton className="h-20 w-full" />
            <Skeleton className="h-20 w-full" />
          </div>
        )}

        {advice && !loading && (
          <div className="space-y-6">
            <div>
              <h4 className="font-bold text-sm text-primary uppercase mb-2">Résumé de Performance</h4>
              <p className="text-sm leading-relaxed">{advice.summary}</p>
            </div>
            
            <div className="grid md:grid-cols-2 gap-4">
              <div className="space-y-3">
                <h4 className="font-bold text-sm flex items-center gap-2 text-accent-foreground">
                  <AlertTriangle className="h-4 w-4 text-accent" />
                  Observations clés
                </h4>
                <ul className="space-y-2">
                  {advice.insights.map((insight, idx) => (
                    <li key={idx} className="text-sm bg-white/50 p-2 rounded border border-primary/10">
                      {insight}
                    </li>
                  ))}
                </ul>
              </div>

              <div className="space-y-3">
                <h4 className="font-bold text-sm flex items-center gap-2 text-primary">
                  <Lightbulb className="h-4 w-4" />
                  Recommandations Actionnables
                </h4>
                <ul className="space-y-2">
                  {advice.recommendations.map((rec, idx) => (
                    <li key={idx} className="text-sm bg-primary/10 p-2 rounded border border-primary/20">
                      {rec}
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
