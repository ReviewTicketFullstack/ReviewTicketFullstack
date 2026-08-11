import { useRef, useState } from "react";
import { uploadImage, type UploadResult } from "@/api/uploadApi";
import { ApiError } from "@/shared/api";

/**
 * 서버 ImageResizer.SUPPORTED_TYPES 와 같은 목록이다.
 *
 * webp 는 넣지 않는다 — ImageIO 가 기본으로 읽지 못해 서버가
 * UNSUPPORTED_IMAGE_TYPE 로 거절한다. accept 에도 같은 값을 써서
 * 파일 선택 창에서부터 못 고르게 한다.
 */
const ACCEPTED_TYPES: readonly string[] = ["image/jpeg", "image/png"];

/** application.yml 의 spring.servlet.multipart.max-file-size 와 같은 값. */
const MAX_FILE_SIZE = 15 * 1024 * 1024;

/** 서버는 errorCode 만 보낸다. 화면에 뜰 문구는 여기서 고른다. */
function toUploadMessage(error: unknown, minLongEdge?: number): string {
  if (!(error instanceof ApiError)) return "사진을 올리지 못했어요.";

  switch (error.errorCode) {
    case "IMAGE_TOO_SMALL":
      return `사진이 너무 작아요. 긴 쪽이 ${minLongEdge}px 이상인 사진을 올려 주세요.`;
    case "UNSUPPORTED_IMAGE_TYPE":
      return "jpg, png 사진만 올릴 수 있어요.";
    case "FILE_TOO_LARGE":
      return "15MB 이하 사진만 올릴 수 있어요.";
    default:
      return "사진을 올리지 못했어요.";
  }
}

export interface ImageUploadOverlayProps {
  src: string | null;
  /** 무엇의 사진인지. img alt 와 스크린리더 문구에 함께 쓴다 */
  alt: string;
  /** 오버레이 문구 */
  label?: string;
  /** 크기·모서리·배경은 부모가 정한다 — 이 컴포넌트는 치수를 갖지 않는다 */
  className?: string;
  /** 참이면 오버레이를 아예 그리지 않는다. 읽기 전용과 같다 */
  disabled?: boolean;
  /**
   * 긴 변의 하한(px). 메뉴 표본 사진처럼 화질이 AI 판정에 영향을 주는 자리에서만
   * 넘긴다 — 기준 사진이 저화질이면 같은 음식을 찍은 리뷰도 유사도가 낮게 나와
   * 거부된다. 로고나 목록 썸네일은 작아도 되므로 넘기지 않는다.
   */
  minLongEdge?: number;
  /** 업로드 성공. 받은 주소를 어디에 붙일지는 부른 쪽이 정한다 */
  onUploaded: (result: UploadResult) => void;
  /** 실패 문구. 화면의 에러 자리에 그대로 넣으면 된다 */
  onError?: (message: string) => void;
}

/**
 * 사진 위에 마우스를 올리면 "업로드"가 뜨고, 누르면 파일을 골라 서버에 올린다.
 *
 * 하는 일은 POST /api/uploads 까지다 — 돌려받은 주소를 가게나 메뉴에 실제로
 * 붙이는 것(PATCH)은 이 컴포넌트의 몫이 아니다. 주소만 onUploaded 로 올린다.
 */
export function ImageUploadOverlay({
  src,
  alt,
  label = "업로드",
  className = "",
  disabled = false,
  minLongEdge,
  onUploaded,
  onError,
}: ImageUploadOverlayProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [isUploading, setIsUploading] = useState(false);

  const handleFileSelected = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    // 같은 사진을 다시 고를 수 있도록 즉시 비운다.
    if (inputRef.current) inputRef.current.value = "";
    if (!file) return;

    // 서버가 거절할 것이 확실한 파일은 올리기 전에 막는다.
    if (!ACCEPTED_TYPES.includes(file.type)) {
      onError?.("jpg, png 사진만 올릴 수 있어요.");
      return;
    }
    if (file.size === 0) {
      onError?.("빈 파일이에요. 다른 사진을 골라 주세요.");
      return;
    }
    if (file.size > MAX_FILE_SIZE) {
      onError?.("15MB 이하 사진만 올릴 수 있어요.");
      return;
    }

    setIsUploading(true);
    try {
      onUploaded(await uploadImage(file, minLongEdge));
    } catch (error) {
      onError?.(toUploadMessage(error, minLongEdge));
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <div className={`group relative overflow-hidden ${className}`}>
      {src ? (
        <img src={src} alt={alt} className="h-full w-full object-cover" />
      ) : (
        <span className="flex h-full w-full items-center justify-center text-xs text-neutral-400">
          사진 없음
        </span>
      )}

      {!disabled && (
        <>
          {/* button 이라 Tab 포커스와 Enter·Space 가 그대로 동작한다.
              focus-visible 을 함께 걸어야 마우스 없이도 오버레이가 보인다. */}
          <button
            type="button"
            aria-label={`${alt} 사진 ${src ? "바꾸기" : "올리기"}`}
            disabled={isUploading}
            onClick={() => inputRef.current?.click()}
            className={`absolute inset-0 flex items-center justify-center bg-black/50 text-xs font-semibold text-white transition-opacity focus-visible:opacity-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-800 ${
              isUploading ? "opacity-100" : "opacity-0 group-hover:opacity-100"
            }`}
          >
            {isUploading ? "올리는 중" : label}
          </button>

          <input
            ref={inputRef}
            type="file"
            accept={ACCEPTED_TYPES.join(",")}
            className="hidden"
            onChange={handleFileSelected}
          />
        </>
      )}
    </div>
  );
}
