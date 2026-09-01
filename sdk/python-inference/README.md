# python-inference

이미지 검증 SDK 의 추론 구성 요소. 자바 SDK 와는 HTTP 로만 이어지며, 서로의
코드를 알지 못한다.

## 엔드포인트

| 경로 | 용도 |
|---|---|
| `POST /similarity` | 사진 두 장 → 유사도 하나. **레거시. 지우지 않는다** |
| `POST /embed` | 사진 N장 → 임베딩 N개 (float32 이진) |
| `GET /` | 상태 확인 + 모델 식별자 |

`/embed` 응답이 JSON 이 아닌 이유 — 자바 SDK 가 외부 의존성 0개를 유지하려면
JSON 파서를 넣을 수 없고, 768차원 실수를 텍스트로 보내면 전송량이 네 배가 된다.
모델 정보는 헤더(`X-Model-Id`, `X-Embedding-Dim`, `X-Embedding-Count`)로 간다.

## 기동

```bash
pip install -r requirements.txt        # transformers 도 필요하다
python -m uvicorn main:app --port 8000 --app-dir sdk/python-inference
```

## 스텁 서버 — 모델 없이 검증하기

```bash
python -m uvicorn stub_server:app --port 8000 --app-dir sdk/python-inference
```

`stub_server.py` 는 세 엔드포인트를 **모델 없이** 같은 형식으로 제공한다.
이미지 바이트에서 결정론적으로 벡터를 만들며, `/similarity` 와 `/embed` 가 같은
벡터를 쓰므로 두 경로 사이의 수치 일관성을 실제로 검증할 수 있다.

이걸로 자바 쪽 계약 테스트를 돌린다:

```bash
cd sdk && ./gradlew test -Dimageverify.contract.url=http://127.0.0.1:8000/similarity
```

`transformers` 도 모델 파일(350MB)도 없이 전송 형식과 계산 일치를 확인하기 위한
장치다. **모델 품질을 검증하지 않는다** — 그건 `ai/README.md` 의 평가가 하는 일이다.

## 알려진 환경 문제 (Windows + Anaconda)

Anaconda 의 `intel-openmp` 와 torch 가 각각 `libiomp5md.dll` 을 들고 있어
`import torch` 단계에서 충돌한다(`OMP: Error #15`). `main.py` 도 같은 문제를 겪는다.

```
C:\...\anaconda3\Library\bin\libiomp5md.dll
C:\...\anaconda3\Lib\site-packages\torch\lib\libiomp5md.dll
```

`stub_server.py` 는 `KMP_DUPLICATE_LIB_OK=TRUE` 를 걸고 스레드를 1개로 묶어
이를 피한다. 다만 그 플래그는 공식 문서가 "조용히 틀린 결과를 낼 수 있다"고
경고하는 것이라, 스텁은 `/similarity` 응답에 torch 를 거치지 않은 순수 파이썬
계산값(`controlSimilarity`)을 함께 실어 보낸다. 두 값이 갈리면 계약 테스트가
즉시 실패한다. 실측 차이는 1.9e-08 로, float32/float64 반올림 수준이다.

**제대로 된 해결은 깨끗한 가상환경을 쓰는 것이다.** 운영에서 `main.py` 를 돌릴
때는 우회 플래그에 기대지 말고 환경을 정리하는 편이 낫다.
