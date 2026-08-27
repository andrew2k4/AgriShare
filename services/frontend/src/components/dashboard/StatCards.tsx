"use client";

import {
  TrendingUp,
  TrendingDown,
  Users,
  Wallet,
  Activity,
  CircleDollarSign,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import type { components } from "@/api";

type OperationDto = components["schemas"]["OperationDto"];
type InvestorDto = components["schemas"]["InvestorDto"];
type ProductionDayDto = components["schemas"]["ProductionDayDto"];

interface StatCardProps {
  title: string;
  value: string;
  icon: any;
  description?: string;
  trend?: { value: string; positive: boolean };
}

function StatCard({ title, value, icon: Icon, description, trend }: StatCardProps) {
  return (
    <Card className="overflow-hidden">
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">
          {title}
        </CardTitle>
        <div className="rounded-full bg-primary/10 p-2">
          <Icon className="h-4 w-4 text-primary" />
        </div>
      </CardHeader>
      <CardContent>
        <div className="text-2xl font-bold">{value}</div>
        {(description || trend) && (
          <p className="mt-1 text-xs text-muted-foreground flex items-center gap-1">
            {trend && (
              <span className={trend.positive ? "text-primary font-bold" : "text-destructive font-bold"}>
                {trend.positive ? "+" : "-"}{trend.value}
              </span>
            )}
            {description}
          </p>
        )}
      </CardContent>
    </Card>
  );
}

interface Props {
  operations: OperationDto[];
  investors: InvestorDto[];
  productions: ProductionDayDto[];
}

export function StatCards({ operations, investors, productions }: Props) {
  const totalInvested = investors.reduce((s, i) => s + i.contribution, 0);
  const totalRevenue = operations
    .filter((o) => o.operationType === "INCOME")
    .reduce((s, o) => s + o.amount, 0);
  const totalExpenses = operations
    .filter((o) => o.operationType === "EXPENSE")
    .reduce((s, o) => s + o.amount, 0);
  const netProfit = totalRevenue - totalExpenses;
  const lastProduction = productions.at(-1);

  return (
    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
      <StatCard
        title="Total Investi"
        value={`${totalInvested.toLocaleString()} FCFA`}
        icon={Wallet}
        description="depuis le début"
      />
      <StatCard
        title="Revenus"
        value={`${totalRevenue.toLocaleString()} FCFA`}
        icon={CircleDollarSign}
        description="toutes périodes"
      />
      <StatCard
        title="Dépenses"
        value={`${totalExpenses.toLocaleString()} FCFA`}
        icon={TrendingDown}
        description="toutes périodes"
      />
      <StatCard
        title="Bénéfice Net"
        value={`${netProfit.toLocaleString()} FCFA`}
        icon={TrendingUp}
        description="disponible"
      />
      <StatCard
        title="Production (dernier jour)"
        value={lastProduction ? String(lastProduction.quantity) : "—"}
        icon={Activity}
        description={lastProduction ? new Date(lastProduction.date).toLocaleDateString("fr-FR") : "aucune donnée"}
      />
      <StatCard
        title="Investisseurs"
        value={String(investors.length)}
        icon={Users}
        description="Actifs"
      />
    </div>
  );
}
