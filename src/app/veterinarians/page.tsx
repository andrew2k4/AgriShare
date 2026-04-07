
"use client";

import { useState } from "react";
import { Navbar } from "@/components/layout/Navbar";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { 
  Stethoscope, 
  MapPin, 
  Phone, 
  Search, 
  CheckCircle2, 
  MessageSquare,
  Filter
} from "lucide-react";
import { MOCK_VETERINARIANS } from "@/lib/mock-data";

export default function VeterinariansPage() {
  const [searchTerm, setSearchTerm] = useState("");
  const [filterSpecialty, setFilterSpecialty] = useState<string | null>(null);

  const filteredVets = MOCK_VETERINARIANS.filter(vet => {
    const matchesSearch = vet.name.toLowerCase().includes(searchTerm.toLowerCase()) || 
                          vet.location.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesSpecialty = !filterSpecialty || vet.specialties.includes(filterSpecialty);
    return matchesSearch && matchesSpecialty;
  });

  const allSpecialties = Array.from(new Set(MOCK_VETERINARIANS.flatMap(v => v.specialties)));

  return (
    <div className="min-h-screen bg-background pb-12">
      <Navbar />
      <main className="container mx-auto px-4 py-8">
        <header className="mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="space-y-1">
            <h1 className="text-3xl font-bold text-primary">Experts Vétérinaires</h1>
            <p className="text-muted-foreground">Trouvez des spécialistes vérifiés pour votre élevage au Cameroun.</p>
          </div>
        </header>

        <div className="grid gap-6 md:grid-cols-4">
          {/* Sidebar Filters */}
          <div className="md:col-span-1 space-y-6">
            <Card>
              <CardHeader className="pb-3">
                <CardTitle className="text-sm flex items-center gap-2">
                  <Filter className="h-4 w-4" />
                  Filtres
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <p className="text-xs font-bold uppercase text-muted-foreground">Recherche</p>
                  <div className="relative">
                    <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
                    <Input 
                      placeholder="Nom ou ville..." 
                      className="pl-8" 
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <p className="text-xs font-bold uppercase text-muted-foreground">Spécialités</p>
                  <div className="flex flex-wrap gap-2">
                    <Badge 
                      variant={filterSpecialty === null ? "default" : "outline"}
                      className="cursor-pointer"
                      onClick={() => setFilterSpecialty(null)}
                    >
                      Tout
                    </Badge>
                    {allSpecialties.map(spec => (
                      <Badge 
                        key={spec}
                        variant={filterSpecialty === spec ? "default" : "outline"}
                        className="cursor-pointer"
                        onClick={() => setFilterSpecialty(spec)}
                      >
                        {spec}
                      </Badge>
                    ))}
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card className="bg-primary/5 border-primary/20">
              <CardContent className="pt-6">
                <div className="flex flex-col items-center text-center space-y-3">
                  <div className="h-12 w-12 rounded-full bg-primary/10 flex items-center justify-center text-primary">
                    <CheckCircle2 className="h-6 w-6" />
                  </div>
                  <h3 className="font-bold text-sm text-primary">Vérification AgriShare</h3>
                  <p className="text-xs text-muted-foreground">
                    Tous nos vétérinaires sont certifiés par notre équipe après vérification de leurs diplômes.
                  </p>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Veterinarians Grid */}
          <div className="md:col-span-3 grid gap-6 sm:grid-cols-2 lg:grid-cols-2">
            {filteredVets.map((vet) => (
              <Card key={vet.id} className="group hover:shadow-md transition-shadow duration-300">
                <CardContent className="pt-6">
                  <div className="flex justify-between items-start mb-4">
                    <div className="flex gap-4">
                      <div className="h-16 w-16 rounded-xl bg-secondary flex items-center justify-center text-primary">
                        <Stethoscope className="h-8 w-8" />
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <h3 className="font-bold text-lg">{vet.name}</h3>
                          {vet.verified && <CheckCircle2 className="h-4 w-4 text-primary fill-primary/10" />}
                        </div>
                        <div className="flex items-center text-muted-foreground text-sm gap-1 mt-1">
                          <MapPin className="h-3 w-3" />
                          {vet.location}
                        </div>
                      </div>
                    </div>
                  </div>

                  <div className="space-y-4">
                    <div className="flex flex-wrap gap-1.5">
                      {vet.specialties.map(spec => (
                        <Badge key={spec} variant="secondary" className="bg-primary/5 text-primary text-[10px]">
                          {spec}
                        </Badge>
                      ))}
                    </div>

                    <div className="pt-4 border-t flex items-center justify-between">
                      <div className="flex flex-col">
                        <span className="text-[10px] text-muted-foreground uppercase font-bold">Expérience</span>
                        <span className="text-sm font-bold">{vet.experienceYears} ans</span>
                      </div>
                      <div className="flex gap-2">
                        <Button size="sm" variant="outline" className="h-9 w-9 p-0 rounded-full border-primary text-primary">
                          <Phone className="h-4 w-4" />
                        </Button>
                        <Button size="sm" className="h-9 bg-primary">
                          <MessageSquare className="h-4 w-4 mr-2" />
                          Contacter
                        </Button>
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
            {filteredVets.length === 0 && (
              <div className="col-span-full py-12 text-center text-muted-foreground italic">
                Aucun vétérinaire trouvé correspondant à vos critères.
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  );
}
