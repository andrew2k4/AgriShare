
"use client";

import Link from "next/link";
import Image from "next/image";
import { Navbar } from "@/components/layout/Navbar";
import { Button } from "@/components/ui/button";
import { 
  Sprout, 
  ShieldCheck, 
  TrendingUp, 
  Stethoscope, 
  ArrowRight,
  ChevronRight,
  PieChart,
  Users
} from "lucide-react";
import { PlaceHolderImages } from "@/lib/placeholder-images";
import { useUser } from "@/firebase";

export default function LandingPage() {
  const { user } = useUser();
  const heroImage = PlaceHolderImages.find(img => img.id === "hero-farm");

  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      
      {/* Hero Section */}
      <section className="relative py-20 overflow-hidden border-b">
        <div className="container mx-auto px-4 grid lg:grid-cols-2 gap-12 items-center">
          <div className="space-y-6 text-center lg:text-left">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-primary/10 text-primary text-sm font-bold border border-primary/20">
              <Sprout className="h-4 w-4" />
              <span>L'Agro-pastoral Moderne au Cameroun</span>
            </div>
            <h1 className="text-4xl md:text-6xl font-extrabold text-foreground leading-tight">
              Investissez dans la Terre avec <span className="text-primary">Transparence</span>.
            </h1>
            <p className="text-lg text-muted-foreground max-w-xl mx-auto lg:mx-0">
              AgriShare connecte les investisseurs aux projets agricoles et d'élevage, sous le monitoring constant d'experts vétérinaires. Suivez vos gains en temps réel.
            </p>
            <div className="flex flex-col sm:flex-row gap-4 justify-center lg:justify-start">
              {user ? (
                <Link href="/dashboard">
                  <Button size="lg" className="bg-primary hover:bg-primary/90 px-8 h-12 text-lg">
                    Accéder à mon Dashboard <ArrowRight className="ml-2 h-5 w-5" />
                  </Button>
                </Link>
              ) : (
                <>
                  <Link href="/login">
                    <Button size="lg" className="bg-primary hover:bg-primary/90 px-8 h-12 text-lg">
                      Commencer maintenant <ArrowRight className="ml-2 h-5 w-5" />
                    </Button>
                  </Link>
                  <Button size="lg" variant="outline" className="border-primary text-primary px-8 h-12 text-lg">
                    En savoir plus
                  </Button>
                </>
              )}
            </div>
          </div>
          
          <div className="relative group">
            <div className="absolute -inset-1 bg-gradient-to-r from-primary to-accent rounded-2xl blur opacity-25 group-hover:opacity-50 transition duration-1000"></div>
            <div className="relative rounded-2xl overflow-hidden shadow-2xl border bg-white aspect-video">
              {heroImage && (
                <Image 
                  src={heroImage.imageUrl} 
                  alt={heroImage.description} 
                  fill
                  className="object-cover"
                  data-ai-hint={heroImage.imageHint}
                />
              )}
            </div>
          </div>
        </div>
      </section>

      {/* Features Grid */}
      <section className="py-20 bg-muted/30">
        <div className="container mx-auto px-4">
          <div className="text-center mb-16 space-y-4">
            <h2 className="text-3xl font-bold text-primary">Pourquoi choisir AgriShare ?</h2>
            <p className="text-muted-foreground max-w-2xl mx-auto">
              Une plateforme conçue pour sécuriser vos investissements et maximiser les rendements grâce à l'expertise.
            </p>
          </div>
          
          <div className="grid md:grid-cols-3 gap-8">
            <div className="bg-white p-8 rounded-2xl border hover:shadow-lg transition-shadow">
              <div className="h-12 w-12 rounded-xl bg-primary/10 flex items-center justify-center text-primary mb-6">
                <ShieldCheck className="h-6 w-6" />
              </div>
              <h3 className="text-xl font-bold mb-3">Transparence Totale</h3>
              <p className="text-sm text-muted-foreground">
                Consultez chaque transaction, chaque vente d'œufs et chaque dépense en temps réel depuis votre tableau de bord.
              </p>
            </div>
            
            <div className="bg-white p-8 rounded-2xl border hover:shadow-lg transition-shadow">
              <div className="h-12 w-12 rounded-xl bg-accent/10 flex items-center justify-center text-accent">
                <Stethoscope className="h-6 w-6" />
              </div>
              <h3 className="text-xl font-bold mb-3">Monitoring Expert</h3>
              <p className="text-sm text-muted-foreground">
                Tous les projets sont suivis par des vétérinaires vérifiés qui enregistrent les prescriptions et alertes sanitaires.
              </p>
            </div>
            
            <div className="bg-white p-8 rounded-2xl border hover:shadow-lg transition-shadow">
              <div className="h-12 w-12 rounded-xl bg-primary/10 flex items-center justify-center text-primary mb-6">
                <PieChart className="h-6 w-6" />
              </div>
              <h3 className="text-xl font-bold mb-3">Gains Dynamiques</h3>
              <p className="text-sm text-muted-foreground">
                Répartition équitable des bénéfices basée sur votre part de capital et l'ancienneté de votre apport.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Stats / Call to action */}
      <section className="py-20">
        <div className="container mx-auto px-4 bg-primary rounded-[2rem] p-12 text-primary-foreground text-center space-y-8">
          <h2 className="text-3xl md:text-4xl font-bold">Prêt à transformer l'agriculture camerounaise ?</h2>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
            <div>
              <p className="text-4xl font-bold">10M+</p>
              <p className="text-sm opacity-80">FCFA Investis</p>
            </div>
            <div>
              <p className="text-4xl font-bold">91%</p>
              <p className="text-sm opacity-80">Taux de Ponte Moyen</p>
            </div>
            <div>
              <p className="text-4xl font-bold">15+</p>
              <p className="text-sm opacity-80">Experts Vétérinaires</p>
            </div>
            <div>
              <p className="text-4xl font-bold">100%</p>
              <p className="text-sm opacity-80">Transparence</p>
            </div>
          </div>
          <div className="pt-4">
            <Link href="/login">
              <Button size="lg" variant="secondary" className="px-10 h-12 text-lg font-bold">
                Créer mon compte <ChevronRight className="ml-1 h-5 w-5" />
              </Button>
            </Link>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="py-12 border-t">
        <div className="container mx-auto px-4 flex flex-col md:flex-row justify-between items-center gap-6">
          <div className="flex items-center gap-2">
            <div className="rounded-lg bg-primary p-1.5">
              <Sprout className="h-5 w-5 text-white" />
            </div>
            <span className="text-lg font-bold text-primary">AgriShare</span>
          </div>
          <p className="text-sm text-muted-foreground text-center">
            &copy; 2024 AgriShare. Tous droits réservés. Développé pour l'agro-pastoral au Cameroun.
          </p>
          <div className="flex gap-6 text-sm font-medium">
            <Link href="#" className="hover:text-primary">Contact</Link>
            <Link href="#" className="hover:text-primary">Mentions légales</Link>
          </div>
        </div>
      </footer>
    </div>
  );
}
