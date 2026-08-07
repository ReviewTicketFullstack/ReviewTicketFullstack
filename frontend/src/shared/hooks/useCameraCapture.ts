import { useState, useRef } from 'react';

export interface CameraCaptureResult {
  /** Base64 인코딩된 사진 데이터 */
  photoData: string;
  /** 파일 타입 (e.g., "image/jpeg") */
  photoType: string;
}

export interface UseCameraCaptureReturn {
  /** 카메라 캡처 요청. 기기에서 카메라 앱 열기 */
  capturePhoto: () => void;
  /** 캡처된 사진 (Base64). 미리보기 표시용 */
  photo: CameraCaptureResult | null;
  /** 캡처된 원본 파일. 서버로 올릴 때 쓴다 — Base64 는 원본보다 약 33% 커진다 */
  photoFile: File | null;
  /** 사진 제거 */
  removePhoto: () => void;
  /** 파일 입력 핸들러 */
  handleFileSelected: (e: React.ChangeEvent<HTMLInputElement>) => void;
  /** 파일 입력 ref */
  fileInputRef: React.MutableRefObject<HTMLInputElement | null>;
  /** 카메라 지원 여부 */
  isCameraSupported: boolean;
  /** 카메라 권한 오류 메시지 */
  error: string | null;
}

/**
 * 기기 카메라로 사진을 촬영하는 hook.
 *
 * 카메라 입력만 허용하고 갤러리 선택은 불가능합니다.
 * 한 번에 1장씩만 촬영할 수 있으며, 새 사진을 촬영하면 이전 사진을 덮어씁니다.
 */
export function useCameraCapture(): UseCameraCaptureReturn {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [photo, setPhoto] = useState<CameraCaptureResult | null>(null);
  const [photoFile, setPhotoFile] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);

  // 기기가 HTML5 File API를 지원하는지 확인
  const isCameraSupported = typeof window !== 'undefined' && 'FileReader' in window;

  const capturePhoto = () => {
    if (!isCameraSupported) {
      setError('이 기기는 카메라 촬영을 지원하지 않습니다.');
      return;
    }

    if (fileInputRef.current) {
      fileInputRef.current.click();
    }
  };

  const handleFileSelected = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];

    if (!file) {
      setError(null);
      return;
    }

    // 이미지 파일만 허용
    if (!file.type.startsWith('image/')) {
      setError('이미지 파일만 첨부할 수 있습니다.');
      return;
    }

    const reader = new FileReader();
    reader.onload = (event) => {
      const data = event.target?.result as string;
      setPhoto({
        photoData: data,
        photoType: file.type,
      });
      setPhotoFile(file);
      setError(null);
    };
    reader.onerror = () => {
      setError('사진을 읽을 수 없습니다.');
    };

    reader.readAsDataURL(file);

    // input value 초기화해 같은 사진을 다시 선택 가능하게 함
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const removePhoto = () => {
    setPhoto(null);
    setPhotoFile(null);
    setError(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  return {
    capturePhoto,
    photo,
    photoFile,
    removePhoto,
    handleFileSelected,
    fileInputRef,
    isCameraSupported,
    error,
  };
}

// Hook 사용 예:
// const { photo, capturePhoto, error, removePhoto } = useCameraCapture();
// <input ref={fileInputRef} type="file" accept="image/*" capture="environment" className="hidden" onChange={handleFileSelected} />
// <button onClick={capturePhoto}>사진 촬영</button>
