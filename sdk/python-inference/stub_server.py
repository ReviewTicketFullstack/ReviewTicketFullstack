"""
계약 테스트용 스텁 추론 서버. 모델을 쓰지 않는다.

main.py 와 엔드포인트 세 개(/, /similarity, /embed)의 요청·응답 형식이 정확히
같다. 다른 점은 DINOv2 대신 이미지 바이트에서 결정론적으로 벡터를 만든다는
것뿐이다.

왜 필요한가
-----------
Phase 2 가 바꾸는 것은 "코사인을 누가 계산하는가"다. 전에는 torch 가 서버에서
계산했고, 이제는 자바가 받아 온 벡터로 계산한다. 두 값이 같은지(AC-30, AC-32)를
확인하려면 **같은 벡터**를 양쪽에 흘려 봐야 하는데, 그건 벡터가 어느 모델에서
나왔는지와 무관한 수치 문제다.

그래서 이 스텁은 transformers 도 모델 파일(350MB)도 없이 그 검증을 가능하게
한다. /similarity 와 /embed 가 같은 embed() 를 쓰므로 둘 사이의 일관성이
구조적으로 보장된다 — 한쪽만 고쳐서 통과시킬 수가 없다.

이 서버는 판정 품질을 검증하지 않는다. 벡터가 실제 음식 사진의 유사도를
반영하지 않기 때문이다. 그건 ai/README.md 의 모델 평가가 하는 일이다.

기동:
    python -m uvicorn stub_server:app --port 8000 --app-dir sdk/python-inference
"""

import hashlib
import io
import math
import os
import struct

# 이 환경의 anaconda 는 intel-openmp 를, torch 는 자기 libiomp5md.dll 을 각각
# 들고 있어 import 단계에서 충돌한다(OMP Error #15). 공식 우회책은 아래 플래그인데,
# 문서가 '조용히 틀린 결과를 낼 수 있다'고 경고한다 — 하필 우리가 재려는 것이
# 수치 일치라서 그냥 쓰면 검증이 무의미해진다.
#
# 그래서 두 가지를 함께 건다.
#  1) 스레드를 1개로 묶어 OpenMP 병렬 경로를 아예 타지 않게 한다.
#  2) /similarity 가 torch 값과 순수 파이썬 값을 함께 돌려준다(controlSimilarity).
#     둘이 어긋나면 우회책이 수치를 건드렸다는 뜻이므로 테스트가 즉시 실패한다.
os.environ.setdefault('KMP_DUPLICATE_LIB_OK', 'TRUE')

import torch
from fastapi import FastAPI, File, Response, UploadFile
from PIL import Image

MODEL_NAME = "stub-deterministic-v1"
DIM = 768

torch.set_num_threads(1)

app = FastAPI()


def embed(image_bytes: bytes) -> torch.Tensor:
    """바이트에서 결정론적으로 L2 정규화된 벡터 하나를 만든다.

    같은 입력이면 언제나 같은 벡터가 나와야 한다 — 그렇지 않으면 두 엔드포인트를
    비교하는 것 자체가 무의미해진다. 그래서 난수 시드를 내용 해시에서 뽑는다.

    실제 이미지로 디코드되는지도 확인한다. 형식 오류에 400 을 내는 동작까지
    main.py 와 같아야 계약 테스트가 의미를 갖는다.
    """
    Image.open(io.BytesIO(image_bytes)).convert("RGB")

    seed = int.from_bytes(hashlib.sha256(image_bytes).digest()[:8], "big")
    generator = torch.Generator().manual_seed(seed % (2**63))
    vector = torch.randn(DIM, generator=generator, dtype=torch.float32)
    return vector / vector.norm(p=2)


@app.get("/")
def health() -> dict:
    return {"status": "ok", "model": MODEL_NAME}


@app.post("/similarity")
def similarity(
    reviewImage: UploadFile = File(...),
    compareImage: UploadFile = File(...),
) -> dict:
    """레거시 경로. torch 가 코사인을 계산한다 — 비교 기준이 되는 쪽이다."""
    review_bytes = reviewImage.file.read()
    compare_bytes = compareImage.file.read()
    try:
        review = embed(review_bytes)
        compare = embed(compare_bytes)
    except Exception:
        return Response(status_code=400, content=b"cannot decode image")

    score = torch.cosine_similarity(review.unsqueeze(0), compare.unsqueeze(0)).item()

    # 대조군: torch 도 OpenMP 도 거치지 않는 같은 계산. 위 값과 갈리면
    # 우회책이 수치를 망가뜨린 것이므로 계약 테스트가 그것을 잡아낸다.
    # 받은 바이트의 해시와 파일명을 함께 돌려준다. 자바가 보낸 원본과 대조하면
    # 손으로 만든 multipart 본문이 제대로 파싱됐는지 증명할 수 있다(AC-70c).
    # 진짜 서버(main.py)에는 없는 필드지만, 키가 늘어나는 것은 계약을 깨지 않는다.
    return {
        "similarity": score,
        "controlSimilarity": _cosine(review.tolist(), compare.tolist()),
        "reviewSha256": hashlib.sha256(review_bytes).hexdigest(),
        "compareSha256": hashlib.sha256(compare_bytes).hexdigest(),
        "reviewFilename": reviewImage.filename,
        "compareFilename": compareImage.filename,
    }


def _cosine(a: list, b: list) -> float:
    dot = sum(x * y for x, y in zip(a, b))
    norm = math.sqrt(sum(x * x for x in a)) * math.sqrt(sum(y * y for y in b))
    return dot / max(norm, 1e-8)


@app.post("/embed")
def embed_endpoint(images: list[UploadFile] = File(...)) -> Response:
    """새 경로. 벡터를 그대로 내주고 코사인은 자바가 계산한다."""
    try:
        vectors = [embed(f.file.read()) for f in images]
    except Exception:
        return Response(status_code=400, content=b"cannot decode image")

    flat = torch.stack(vectors).flatten().tolist()
    return Response(
        content=struct.pack("<%df" % len(flat), *flat),
        media_type="application/octet-stream",
        headers={
            "X-Model-Id": MODEL_NAME,
            "X-Embedding-Dim": str(DIM),
            "X-Embedding-Count": str(len(vectors)),
        },
    )
