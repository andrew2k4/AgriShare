import { 
  TrendingUp, 
  TrendingDown, 
  Users, 
  Wallet, 
  Activity, 
  CircleDollarSign 
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

interface StatCardProps {
  title: string;
  value: string;
  icon: any;
  description?: string;
  trend?: {
    value: string;
    positive: boolean;
  };
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

export function StatCards() {
  return (
    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
      <StatCard
        title="Total Investi"
        value="10,000,000 FCFA"
        icon={Wallet}
        description="depuis le début"
      />
      <StatCard
        title="Revenus"
        value="1,350,000 FCFA"
        icon={CircleDollarSign}
        trend={{ value: "12%", positive: true }}
        description="ce mois"
      />
      <StatCard
        title="Dépenses"
        value="450,000 FCFA"
        icon={TrendingDown}
        trend={{ value: "5%", positive: false }}
        description="ce mois"
      />
      <StatCard
        title="Bénéfice Net"
        value="900,000 FCFA"
        icon={TrendingUp}
        description="disponible"
      />
      <StatCard
        title="Production Jour"
        value="450 Œufs"
        icon={Activity}
        trend={{ value: "2%", positive: true }}
        description="vs hier"
      />
      <StatCard
        title="Taux de Ponte"
        value="91%"
        icon={Activity}
        description="Moyenne troupeau"
      />
      <StatCard
        title="Mortalité Jour"
        value="2"
        icon={TrendingDown}
        description="Poules"
      />
      <StatCard
        title="Investisseurs"
        value="3"
        icon={Users}
        description="Actifs"
      />
    </div>
  );
}
