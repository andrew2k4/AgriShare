'use client';

import type { ReactNode } from 'react';
import { Sprout } from 'lucide-react';
import { Navbar } from '@/components/layout/Navbar';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';

/** Shared frame of the sign-in, sign-up and account recovery pages. */
export function AuthCard({ title, description, children, footer }: {
  title: string;
  description?: string;
  children: ReactNode;
  footer?: ReactNode;
}) {
  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <main className="container mx-auto px-4 py-12 flex flex-col items-center">
        <Card className="w-full max-w-md shadow-xl border-primary/20 bg-white">
          <CardHeader className="text-center space-y-1">
            <div className="mx-auto bg-primary w-12 h-12 rounded-xl flex items-center justify-center mb-2 shadow-lg shadow-primary/20">
              <Sprout className="text-white h-7 w-7" />
            </div>
            <CardTitle className="text-2xl font-bold text-primary">{title}</CardTitle>
            {description && <CardDescription>{description}</CardDescription>}
          </CardHeader>
          <CardContent>{children}</CardContent>
          {footer && <CardFooter className="flex flex-col gap-2 border-t pt-4">{footer}</CardFooter>}
        </Card>
      </main>
    </div>
  );
}

export { FieldError } from '@/components/ui/field-error';
