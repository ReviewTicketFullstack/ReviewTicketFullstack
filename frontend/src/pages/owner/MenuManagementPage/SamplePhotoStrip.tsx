const SAMPLE_SLOTS = [1, 2, 3, 4, 5];
// FE-2.3.1: '이미지+후기' 선택했을 때만 표본사진 5장 UI 노출
export function SamplePhotoStrip() {
  return (
    <div className="flex w-full gap-2">
      {SAMPLE_SLOTS.map((n) => (
        <div
          key={n}
          className="flex flex-1 aspect-square items-center justify-center rounded-lg bg-gray-200 text-center text-xs text-gray-400"
        >
          음식 이미지 {n}
        </div>
      ))}
    </div>
  );
}
