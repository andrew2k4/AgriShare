"use client";

import { useEffect, useRef } from "react";
import { useRouter } from "next/navigation";
import { Loader2 } from "lucide-react";
import { afterLoginPath, useSession } from "@/auth/AuthProvider";

/** Where the API sends the browser after a successful Google sign-in: the session cookie is set. */
export default function AuthCallbackPage() {
  const { refresh } = useSession();
  const router = useRouter();
  const done = useRef(false);

  useEffect(() => {
    if (done.current) {
      return;
    }
    done.current = true;
    refresh().then((me) => {
      if (!me) {
        router.replace("/login?error=GOOGLE_LOGIN_FAILED");
      } else {
        router.replace(afterLoginPath(me));
      }
    });
  }, [refresh, router]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-background">
      <Loader2 className="h-10 w-10 animate-spin text-primary" />
    </div>
  );
}
