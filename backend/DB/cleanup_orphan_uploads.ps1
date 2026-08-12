# 업로드 폴더의 고아 파일을 찾아 지운다.
#
# 고아 파일이 생기는 이유 — POST /api/uploads 는 파일을 디스크에 남기고 주소만
# 돌려준다. 그 주소를 가게나 메뉴에 붙이는 것(PATCH)은 별도 요청이라, 사진만
# 올리고 저장을 누르지 않고 화면을 떠나면 그 파일은 아무도 가리키지 않는 채로
# 남는다. 리뷰 사진은 판정을 통과할 때만 디스크에 쓰므로 이 문제가 없다.
#
# 안전장치 — 기본은 목록만 보여주고 지우지 않는다. 실제로 지우려면 -Delete 를
# 붙인다. 지우기 전에 반드시 목록을 눈으로 확인할 것.
#
# 사용:
#   .\cleanup_orphan_uploads.ps1                    # 목록만 (기본)
#   .\cleanup_orphan_uploads.ps1 -Delete            # 실제 삭제
param(
    [switch]$Delete,
    [string]$UploadDir = "$PSScriptRoot\..\Server\uploads",
    [string]$MysqlExe  = "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe",
    [int]$Port = 21096,
    [string]$User = "reviewticket",
    [string]$Database = "reviewticket"
)

if (-not (Test-Path $UploadDir)) {
    Write-Error "업로드 폴더가 없습니다: $UploadDir"
    exit 1
}

# 어느 컬럼이든 사진 주소를 담는 곳은 전부 훑는다. 하나라도 빠뜨리면
# 살아있는 파일을 지우게 되므로, 컬럼을 추가할 때 이 목록도 같이 고쳐야 한다.
$query = @'
SELECT logo_url FROM store_table WHERE logo_url IS NOT NULL
UNION SELECT menu_image_url FROM menu_table WHERE menu_image_url IS NOT NULL
UNION SELECT sample_image_url_1 FROM menu_table WHERE sample_image_url_1 IS NOT NULL
UNION SELECT sample_image_url_2 FROM menu_table WHERE sample_image_url_2 IS NOT NULL
UNION SELECT sample_image_url_3 FROM menu_table WHERE sample_image_url_3 IS NOT NULL
UNION SELECT sample_image_url_4 FROM menu_table WHERE sample_image_url_4 IS NOT NULL
UNION SELECT sample_image_url_5 FROM menu_table WHERE sample_image_url_5 IS NOT NULL
UNION SELECT review_image_url FROM customer_review_table
UNION SELECT compare_image_url FROM customer_review_table
'@

Write-Host "DB 에서 참조 중인 사진 주소를 읽는 중... (비밀번호를 물어봅니다)"
$referenced = & $MysqlExe -u $User -p -P $Port -h 127.0.0.1 --default-character-set=utf8mb4 -N -e $query $Database

if ($LASTEXITCODE -ne 0) {
    Write-Error "DB 조회에 실패했습니다. 파일을 하나도 지우지 않고 멈춥니다."
    exit 1
}

# 조회가 0건이면 위험하다 — 연결은 됐는데 표가 비었거나 쿼리가 잘못된 경우,
# 그대로 진행하면 폴더 전체를 지우게 된다.
if (-not $referenced -or $referenced.Count -eq 0) {
    Write-Error "참조 목록이 비어 있습니다. 비정상으로 보여 멈춥니다."
    exit 1
}

$referencedNames = $referenced | ForEach-Object { Split-Path $_ -Leaf }
$onDisk = Get-ChildItem -File $UploadDir
$orphans = $onDisk | Where-Object { $referencedNames -notcontains $_.Name }

$totalMb = [math]::Round((($orphans | Measure-Object Length -Sum).Sum / 1MB), 2)
Write-Host ""
Write-Host "디스크 파일 : $($onDisk.Count) 개"
Write-Host "DB 참조     : $($referencedNames.Count) 개"
Write-Host "고아 파일   : $($orphans.Count) 개 ($totalMb MB)"
Write-Host ""

if ($orphans.Count -eq 0) {
    Write-Host "지울 것이 없습니다."
    exit 0
}

$orphans | ForEach-Object { Write-Host "  $($_.Name)  $([math]::Round($_.Length / 1KB)) KB" }

if (-not $Delete) {
    Write-Host ""
    Write-Host "목록만 보여줬습니다. 실제로 지우려면 -Delete 를 붙여 다시 실행하세요."
    exit 0
}

$orphans | Remove-Item -Force
Write-Host ""
Write-Host "$($orphans.Count) 개를 삭제했습니다."
