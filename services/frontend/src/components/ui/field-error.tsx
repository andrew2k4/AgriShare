/** Field error shown under an input. */
export function FieldError({ message }: { message: string | null | undefined }) {
  if (!message) {
    return null;
  }
  return <p className="text-xs text-destructive mt-1">{message}</p>;
}
