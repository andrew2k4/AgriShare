"use client";

import Link from "next/link";
import Image from "next/image";
import {
  Sprout,
  Home,
  LogIn,
  ArrowRight,
  ShieldCheck,
  Stethoscope,
  PieChart,
  ChevronRight,
} from "lucide-react";
import { Button } from "@/components/ui/button";

const features = [
  {
    icon: ShieldCheck,
    iconClass: "bg-primary/10 text-primary",
    title: "Transparence Totale",
    description:
      "Consultez chaque transaction, chaque vente d'œufs et chaque dépense en temps réel depuis votre tableau de bord.",
  },
  {
    icon: Stethoscope,
    iconClass: "bg-accent/15 text-accent",
    title: "Monitoring Expert",
    description:
      "Tous les projets sont suivis par des vétérinaires vérifiés qui enregistrent les prescriptions et alertes sanitaires.",
  },
  {
    icon: PieChart,
    iconClass: "bg-primary/10 text-primary",
    title: "Gains Dynamiques",
    description:
      "Répartition équitable des bénéfices basée sur votre part de capital et l'ancienneté de votre apport.",
  },
];

const stats = [
  { value: "10M+", label: "FCFA Investis" },
  { value: "91%", label: "Taux de Ponte Moyen" },
  { value: "15+", label: "Experts Vétérinaires" },
  { value: "100%", label: "Transparence" },
];

export function LandingPage() {
  return (
    <div className="flex min-h-screen flex-col bg-background">
      {/* Header */}
      <header className="sticky top-0 z-50 w-full border-b border-border/60 bg-background/80 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="container mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex h-20 items-center justify-between">
            <Link href="/" className="flex items-center gap-2.5">
              <div className="rounded-lg bg-primary p-2">
                <Sprout className="h-6 w-6 text-primary-foreground" />
              </div>
              <span className="text-2xl font-bold tracking-tight text-primary">
                AgriShare
              </span>
            </Link>

            <div className="flex items-center gap-6">
              <Link
                href="/"
                className="hidden items-center gap-2 text-base font-semibold text-primary transition-colors hover:text-primary/80 sm:flex"
              >
                <Home className="h-5 w-5" />
                Accueil
              </Link>
              <Link href="/login">
                <Button
                  size="lg"
                  className="gap-2 bg-primary px-6 text-base font-semibold shadow-sm hover:bg-primary/90"
                >
                  <LogIn className="h-5 w-5" />
                  Connexion
                </Button>
              </Link>
            </div>
          </div>
        </div>
      </header>

      {/* Hero */}
      <section className="bg-background">
        <div className="container mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid items-center gap-12 py-16 lg:grid-cols-2 lg:gap-16 lg:py-24">
            <div className="space-y-8">
              <div className="inline-flex items-center gap-2 rounded-full bg-primary/10 px-4 py-2">
                <Sprout className="h-4 w-4 text-primary" />
                <span className="text-sm font-semibold text-primary">
                  L'Agro-pastoral Moderne au Cameroun
                </span>
              </div>

              <h1 className="text-5xl font-extrabold leading-[1.05] tracking-tight text-foreground sm:text-6xl lg:text-7xl">
                Investissez dans la Terre avec{" "}
                <span className="text-primary">Transparence</span>
                <span className="text-foreground">.</span>
              </h1>

              <p className="max-w-xl text-lg leading-relaxed text-muted-foreground">
                AgriShare connecte les investisseurs aux projets agricoles et
                d'élevage, sous le monitoring constant d'experts vétérinaires.
                Suivez vos gains en temps réel.
              </p>

              <div className="flex flex-col gap-4 sm:flex-row">
                <Link href="/login">
                  <Button
                    size="lg"
                    className="h-14 w-full gap-2 bg-primary px-8 text-base font-semibold shadow-md hover:bg-primary/90 sm:w-auto"
                  >
                    Commencer maintenant
                    <ArrowRight className="h-5 w-5" />
                  </Button>
                </Link>
                <Link href="#features">
                  <Button
                    size="lg"
                    variant="outline"
                    className="h-14 w-full border-primary px-8 text-base font-semibold text-primary hover:bg-primary/5 hover:text-primary sm:w-auto"
                  >
                    En savoir plus
                  </Button>
                </Link>
              </div>
            </div>

            <div className="relative">
              <Image
                // TODO: placeholder — à remplacer par la photo finale du hero
                src="https://images.unsplash.com/photo-1470770841072-f978cf4d019e?auto=format&fit=crop&w=1400&q=80"
                alt="Investisseur contemplant un lac de montagne"
                width={1400}
                height={900}
                priority
                className="aspect-[3/2] w-full rounded-2xl object-cover shadow-2xl"
              />
            </div>
          </div>
        </div>
      </section>

      {/* Features */}
      <section id="features" className="bg-muted/40 py-20 lg:py-24">
        <div className="container mx-auto px-4 sm:px-6 lg:px-8">
          <div className="mx-auto max-w-2xl text-center">
            <h2 className="text-3xl font-bold tracking-tight text-primary sm:text-4xl">
              Pourquoi choisir AgriShare ?
            </h2>
            <p className="mt-5 text-lg leading-relaxed text-muted-foreground">
              Une plateforme conçue pour sécuriser vos investissements et
              maximiser les rendements grâce à l'expertise.
            </p>
          </div>

          <div className="mt-16 grid gap-8 md:grid-cols-3">
            {features.map((feature) => {
              const Icon = feature.icon;
              return (
                <div
                  key={feature.title}
                  className="rounded-2xl border border-border/60 bg-card p-8 shadow-sm"
                >
                  <div
                    className={`flex h-14 w-14 items-center justify-center rounded-xl ${feature.iconClass}`}
                  >
                    <Icon className="h-7 w-7" />
                  </div>
                  <h3 className="mt-8 text-xl font-bold text-foreground">
                    {feature.title}
                  </h3>
                  <p className="mt-4 leading-relaxed text-muted-foreground">
                    {feature.description}
                  </p>
                </div>
              );
            })}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="bg-background py-20 lg:py-24">
        <div className="container mx-auto px-4 sm:px-6 lg:px-8">
          <div className="rounded-3xl bg-primary px-6 py-16 text-center sm:px-12 lg:px-16">
            <h2 className="text-3xl font-bold tracking-tight text-primary-foreground sm:text-4xl lg:text-5xl">
              Prêt à transformer l'agriculture camerounaise ?
            </h2>

            <div className="mx-auto mt-14 grid max-w-4xl grid-cols-2 gap-y-10 md:grid-cols-4">
              {stats.map((stat) => (
                <div key={stat.label} className="text-center">
                  <div className="text-4xl font-extrabold text-primary-foreground lg:text-5xl">
                    {stat.value}
                  </div>
                  <div className="mt-2 text-sm text-primary-foreground/80 lg:text-base">
                    {stat.label}
                  </div>
                </div>
              ))}
            </div>

            <div className="mt-14 flex justify-center">
              <Link href="/login">
                <Button
                  size="lg"
                  className="h-14 gap-2 bg-secondary px-8 text-base font-bold text-secondary-foreground shadow-md hover:bg-secondary/90"
                >
                  Créer mon compte
                  <ChevronRight className="h-5 w-5" />
                </Button>
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-border/60 bg-background">
        <div className="container mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex flex-col items-center justify-between gap-6 py-8 md:flex-row">
            <Link href="/" className="flex items-center gap-2.5">
              <div className="rounded-lg bg-primary p-2">
                <Sprout className="h-6 w-6 text-primary-foreground" />
              </div>
              <span className="text-xl font-bold tracking-tight text-primary">
                AgriShare
              </span>
            </Link>

            <p className="text-center text-sm text-muted-foreground">
              © 2024 AgriShare. Tous droits réservés. Développé pour
              l'agro-pastoral au Cameroun.
            </p>

            <div className="flex items-center gap-8">
              <Link
                href="#"
                className="text-sm font-medium text-foreground transition-colors hover:text-primary"
              >
                Contact
              </Link>
              <Link
                href="#"
                className="text-sm font-medium text-foreground transition-colors hover:text-primary"
              >
                Mentions légales
              </Link>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}
