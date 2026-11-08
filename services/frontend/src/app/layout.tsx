import type {Metadata} from 'next';
import './globals.css';
import { AuthProvider } from '@/auth/AuthProvider';
import { Toaster } from '@/components/ui/toaster';

export const metadata: Metadata = {
  title: 'AgriShare – Investissement agro-pastoral transparent',
  description: "Investissez dans des projets agricoles et d'élevage et suivez leurs résultats en toute transparence.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="fr">
      <head>
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous" />
        <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet" />
      </head>
      <body className="font-body antialiased bg-background text-foreground">
        <AuthProvider>
          {children}
          <Toaster />
        </AuthProvider>
      </body>
    </html>
  );
}
