const SAMPLE_SLOTS = [1, 2, 3, 4, 5];

export function SamplePhotoStrip() {
  return (
    <div className="flex w-full gap-2">
      {SAMPLE_SLOTS.map((n) => (
        <div
          key={n}
          className="flex h-20 flex-1 items-center justify-center rounded-lg bg-gray-200 text-center text-xs text-gray-400"
        >
          음식 이미지 {n}
        </div>
      ))}
    </div>
  );
}
