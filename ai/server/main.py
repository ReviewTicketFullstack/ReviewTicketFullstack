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
from contextlib import asynccontextmanager
from typing import Optional

import torch
from fastapi import FastAPI, File, UploadFile
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
