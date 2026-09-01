"""
리뷰 사진 검증용 AI 서버.

백엔드(ReviewService)가 손님이 올린 리뷰 사진과 메뉴 표본 사진을 한 쌍씩 보내면,
DINOv2 임베딩의 코사인 유사도를 계산해 돌려준다. 0.80 문턱값과의 비교(통과/거부
판정)는 이 서버가 아니라 백엔드가 한다 — 여기는 숫자 하나만 책임진다.

기동:
    C:\\dev\\ReviewTicketFullstack\\ai\\dinov2-test\\.venv\\Scripts\\python.exe -m uvicorn main:app --port 8000
    (별도 venv 를 새로 만들지 않고 dinov2-test 의 venv 를 그대로 쓴다 — torch·
    transformers 가 이미 설치돼 있다. fastapi/uvicorn/python-multipart 만
    추가로 설치하면 된다: requirements.txt 참고)

백엔드 application.yml 의 reviewticket.ai.server-url 기본값이 이미
http://localhost:8000/similarity 라 백엔드 쪽 설정은 안 건드려도 된다.

요청/응답 형태는 ImageSimilarityClient.java 가 이미 가정하고 있던 것과
정확히 맞춘다 — 파트 이름 reviewImage/compareImage, 응답 키 similarity.
"""

import io
import struct
from contextlib import asynccontextmanager
from typing import Optional

import torch
from fastapi import FastAPI, File, Response, UploadFile
from PIL import Image
from transformers import AutoImageProcessor, AutoModel

MODEL_NAME = "facebook/dinov2-base"

# 요청 하나(이미지 한 쌍)의 연산에 CPU 스레드를 몇 개까지 쓸지 제한한다.
#
# 기본값(전체 코어)으로 두면, 백엔드가 메뉴 표본 사진 5장을 동시에 보낼 때
# 다섯 요청이 서로 같은 코어를 붙잡으려 경쟁해 오히려 전체가 더 느려진다.
# 요청 하나당 스레드를 1개로 좁히는 대신, 요청 다섯 개가 서로 다른 코어에서
# 동시에 진행되게 한다 — "하나를 최대한 빨리"가 아니라 "여러 개를 동시에"를
# 우선하는 설정이다.
torch.set_num_threads(1)

model: Optional[AutoModel] = None
processor: Optional[AutoImageProcessor] = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    # 모델은 기동 시 한 번만 로드한다 — 요청마다 로드하면 그 자체가 몇 초씩 걸린다.
    global model, processor
    print(f"모델 로딩: {MODEL_NAME}")
    processor = AutoImageProcessor.from_pretrained(MODEL_NAME)
    model = AutoModel.from_pretrained(MODEL_NAME)
    model.eval()
    print("모델 로딩 완료 — /similarity 요청을 받을 준비가 됐다")
    yield


app = FastAPI(lifespan=lifespan)


def embed(image: Image.Image) -> torch.Tensor:
    inputs = processor(images=image, return_tensors="pt")
    with torch.no_grad():
        outputs = model(**inputs)
    # [CLS] 토큰 임베딩을 쓴다 — eval_menu_match_v2.py 와 같은 방식이다.
    cls_embedding = outputs.last_hidden_state[:, 0]
    return cls_embedding / cls_embedding.norm(p=2, dim=-1, keepdim=True)


@app.get("/")
def health() -> dict:
    """서버가 떠 있고 모델이 로드됐는지만 확인하는 자리. 백엔드는 안 부른다."""
    return {"status": "ok", "model": MODEL_NAME}


# 일부러 async def 가 아니라 그냥 def 다. FastAPI(Starlette)는 동기 함수를
# 만나면 요청마다 별도 스레드풀 스레드에서 돌린다. torch 의 실제 행렬 연산은
# (파이썬이 아니라 C++/네이티브 코드가 도는 동안) GIL을 놓아 주므로, 이 스레드들이
# 진짜로 동시에 CPU를 나눠 쓸 수 있다. 백엔드가 표본 사진 5장을 동시에 보내는
# 구조와 이게 맞물려야 실제로 5배 가까이 빨라진다 — async def 로 만들면 오히려
# 이 블로킹 연산이 이벤트 루프를 막아 버려 더 나빠진다.
@app.post("/similarity")
def similarity(
    reviewImage: UploadFile = File(...),
    compareImage: UploadFile = File(...),
) -> dict:
    review = Image.open(io.BytesIO(reviewImage.file.read())).convert("RGB")
    compare = Image.open(io.BytesIO(compareImage.file.read())).convert("RGB")

    score = torch.cosine_similarity(embed(review), embed(compare)).item()

    return {"similarity": score}


# ---------------------------------------------------------------------------
# /embed — Phase 2 에서 추가. /similarity 는 그대로 둔다(롤백 경로).
#
# /similarity 는 임베딩을 서버 안에 가둬 버려서 캐시를 걸 방법이 없다. 리뷰
# 한 건에 표본 5장이면 임베딩을 10번(5쌍 × 2장) 계산하고, 표본 사진은 바뀌지도
# 않았는데 리뷰마다 다시 계산한다. 임베딩을 값으로 꺼내 주면 백엔드가 그걸
# 캐시해 둘 수 있고, 그때부터는 리뷰 사진 한 장만 새로 계산하면 된다.
#
# 응답을 JSON 이 아니라 float32 바이너리로 돌려주는 이유 — 자바 SDK 가 외부
# 의존성 0개를 유지하려면 JSON 파서를 넣을 수 없다. 게다가 768차원 실수를
# 텍스트로 쓰면 전송량이 4배쯤 된다.
# ---------------------------------------------------------------------------


def embed_batch(images: list) -> torch.Tensor:
    """여러 장을 한 번에 임베딩한다. 반환값은 L2 정규화된 (N, D) 텐서다.

    한 장씩 도는 대신 배치로 넣는다 — 행렬 연산이 커질수록 장당 비용이 준다.
    정규화를 서버에서 끝내는 것도 의도적이다. 자바 쪽에서 다시 정규화하면
    부동소수점 처리가 두 곳으로 나뉘어 값이 미세하게 갈릴 수 있다.
    """
    inputs = processor(images=images, return_tensors="pt")
    with torch.no_grad():
        outputs = model(**inputs)
    cls_embeddings = outputs.last_hidden_state[:, 0]
    return cls_embeddings / cls_embeddings.norm(p=2, dim=-1, keepdim=True)


def pack_embeddings(embeddings: torch.Tensor) -> bytes:
    """(N, D) 텐서를 리틀엔디언 float32 연속 바이트로 만든다."""
    flat = embeddings.to(torch.float32).contiguous().flatten().tolist()
    return struct.pack("<%df" % len(flat), *flat)


@app.post("/embed")
def embed_endpoint(images: list[UploadFile] = File(...)) -> Response:
    """이미지 N장을 받아 임베딩 N개를 이진으로 돌려준다.

    파트 이름은 images 하나이며 여러 번 반복해 보낸다. 순서는 보존된다 —
    i번째 벡터가 i번째 이미지의 것이다.
    """
    if model is None or processor is None:
        return Response(status_code=503, content=b"model not loaded")

    try:
        decoded = [Image.open(io.BytesIO(f.file.read())).convert("RGB") for f in images]
    except Exception:
        # 어떤 장이 문제인지는 알려주지 않는다 — 백엔드가 할 수 있는 일이 없고,
        # 서버가 무엇을 읽을 수 있는지 떠보는 통로가 되지 않게 한다.
        return Response(status_code=400, content=b"cannot decode image")

    embeddings = embed_batch(decoded)

    return Response(
        content=pack_embeddings(embeddings),
        media_type="application/octet-stream",
        headers={
            "X-Model-Id": MODEL_NAME,
            "X-Embedding-Dim": str(embeddings.shape[1]),
            "X-Embedding-Count": str(embeddings.shape[0]),
        },
    )
