export function Loading() {
  return (
    <div role="status" className="flex items-center justify-center py-10">
      <span
        aria-hidden="true"
        className="size-8 animate-spin rounded-full border-4 border-line-100 border-t-brand-800 motion-reduce:animate-none"
      />
      <span className="sr-only">불러오는 중이에요</span>
    </div>
  );
}
