export function Skeleton({ className = '' }: { className?: string }) {
  return <span aria-hidden="true" className={`sc-skeleton block rounded-lg ${className}`} />;
}
