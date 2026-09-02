const SAMPLE_SLOTS = [1, 2, 3, 4, 5];
// FE-2.3.1: '이미지+후기' 선택했을 때만 표본사진 5장 UI 노출
export function SamplePhotoStrip() {
  return (
    <div className="flex w-full gap-2">
      {SAMPLE_SLOTS.map((n) => (
        <div
          key={n}
          className="flex aspect-square flex-1 items-center justify-center rounded-xl bg-fill-100 text-center text-xs text-ink-500"
        >
          음식 이미지 {n}
        </div>
      ))}
    </div>
  );
}
