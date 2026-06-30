"use client";

import { Loader2 } from "lucide-react";
import { useUser } from "@/firebase";
import { LandingPage } from "@/components/landing/LandingPage";
import { DashboardHome } from "@/components/dashboard/DashboardHome";

export default function Home() {
  const { user, isUserLoading } = useUser();

  if (isUserLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  return user ? <DashboardHome /> : <LandingPage />;
}
