# Heimdall 성능 테스트 스크립트 (Windows용)
# PowerShell에서 실행

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$ApiKey = "test-api-key",
    [int]$ConcurrentRequests = 100,
    [int]$TotalRequests = 10000,
    [int]$Duration = 60
)

Write-Host "========================================" -ForegroundColor Blue
Write-Host "  Heimdall 성능 테스트" -ForegroundColor Blue
Write-Host "========================================" -ForegroundColor Blue
Write-Host ""
Write-Host "설정:" -ForegroundColor Yellow
Write-Host "  - Base URL: $BaseUrl"
Write-Host "  - 동시 요청: $ConcurrentRequests"
Write-Host "  - 총 요청: $TotalRequests"
Write-Host "  - 테스트 시간: ${Duration}s"
Write-Host ""

# 필수 도구 확인 (curl 또는 Invoke-WebRequest 사용)
if (-not (Get-Command curl -ErrorAction SilentlyContinue)) {
    Write-Host "❌ curl이 설치되어 있지 않습니다." -ForegroundColor Red
    Write-Host "또는 Invoke-WebRequest를 사용합니다..." -ForegroundColor Yellow
    $useCurl = $false
} else {
    $useCurl = $true
}

# 테스트 데이터
$testData = @{
    source = "performance-test"
    message = "Performance test log message with some data to simulate real workload"
    severity = "INFO"
    timestamp = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
    metadata = @{
        test = $true
        environment = "performance"
        iteration = 1
    }
} | ConvertTo-Json

Write-Host "✓ 테스트 준비 완료" -ForegroundColor Green
Write-Host ""

# 헤더 설정
$headers = @{
    "Content-Type" = "application/json"
    "X-API-Key" = $ApiKey
}

# 성능 측정 함수
function Measure-ApiPerformance {
    param(
        [string]$Url,
        [string]$Method = "GET",
        [string]$Body = $null,
        [int]$Count = 100,
        [string]$Description
    )

    Write-Host "[$Description]" -ForegroundColor Blue
    Write-Host "------------------------------------"

    $successCount = 0
    $failureCount = 0
    $totalTime = 0
    $responseTimes = @()

    for ($i = 0; $i -lt $Count; $i++) {
        try {
            $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
            
            if ($Method -eq "POST") {
                if ($useCurl) {
                    $result = curl -X POST -H "Content-Type: application/json" -H "X-API-Key: $ApiKey" -d $Body $Url -s -w "%{http_code}" -o nul
                } else {
                    $result = Invoke-WebRequest -Uri $Url -Method Post -Headers $headers -Body $Body -UseBasicParsing
                }
            } else {
                if ($useCurl) {
                    $result = curl -H "X-API-Key: $ApiKey" $Url -s -w "%{http_code}" -o nul
                } else {
                    $result = Invoke-WebRequest -Uri $Url -Method Get -Headers $headers -UseBasicParsing
                }
            }

            $stopwatch.Stop()
            $elapsed = $stopwatch.ElapsedMilliseconds
            $responseTimes += $elapsed
            $totalTime += $elapsed
            $successCount++

            if (($i + 1) % 10 -eq 0) {
                Write-Progress -Activity $Description -Status "진행 중..." -PercentComplete (($i + 1) / $Count * 100)
            }
        }
        catch {
            $failureCount++
        }
    }

    Write-Progress -Activity $Description -Completed

    # 통계 계산
    $avgTime = if ($successCount -gt 0) { $totalTime / $successCount } else { 0 }
    $sortedTimes = $responseTimes | Sort-Object
    $p50 = if ($sortedTimes.Count -gt 0) { $sortedTimes[[Math]::Floor($sortedTimes.Count * 0.50)] } else { 0 }
    $p95 = if ($sortedTimes.Count -gt 0) { $sortedTimes[[Math]::Floor($sortedTimes.Count * 0.95)] } else { 0 }
    $p99 = if ($sortedTimes.Count -gt 0) { $sortedTimes[[Math]::Floor($sortedTimes.Count * 0.99)] } else { 0 }
    $rps = if ($totalTime -gt 0) { [Math]::Round($successCount / ($totalTime / 1000), 2) } else { 0 }

    Write-Host "  총 요청: $Count"
    Write-Host "  성공: $successCount" -ForegroundColor Green
    Write-Host "  실패: $failureCount" -ForegroundColor $(if ($failureCount -gt 0) { "Red" } else { "Green" })
    Write-Host "  평균 응답 시간: $([Math]::Round($avgTime, 2))ms"
    Write-Host "  P50: ${p50}ms"
    Write-Host "  P95: ${p95}ms"
    Write-Host "  P99: ${p99}ms"
    Write-Host "  RPS (요청/초): $rps"
    Write-Host ""
}

# 1. Health Check 테스트
Measure-ApiPerformance -Url "$BaseUrl/actuator/health" -Method "GET" -Count 100 -Description "1/5 Health Check 테스트"

# 2. 로그 수집 테스트
Measure-ApiPerformance -Url "$BaseUrl/api/v1/logs" -Method "POST" -Body $testData -Count 1000 -Description "2/5 로그 수집 (POST) 테스트"

# 3. 로그 검색 테스트
Measure-ApiPerformance -Url "$BaseUrl/api/v1/logs/search?page=0&size=20" -Method "GET" -Count 500 -Description "3/5 로그 검색 (GET) 테스트"

# 4. 통계 조회 테스트
Measure-ApiPerformance -Url "$BaseUrl/api/v1/statistics/overall" -Method "GET" -Count 200 -Description "4/5 통계 조회 테스트"

# 5. 혼합 부하 테스트 (병렬)
Write-Host "[5/5] 혼합 부하 테스트 (병렬)" -ForegroundColor Blue
Write-Host "------------------------------------"
Write-Host "POST, GET, Stats 엔드포인트를 동시에 테스트합니다..."

$jobs = @()

# POST 작업
$jobs += Start-Job -ScriptBlock {
    param($BaseUrl, $ApiKey, $TestData)
    $headers = @{ "Content-Type" = "application/json"; "X-API-Key" = $ApiKey }
    $count = 0
    for ($i = 0; $i -lt 500; $i++) {
        try {
            Invoke-WebRequest -Uri "$BaseUrl/api/v1/logs" -Method Post -Headers $headers -Body $TestData -UseBasicParsing | Out-Null
            $count++
        } catch {}
    }
    return $count
} -ArgumentList $BaseUrl, $ApiKey, $testData

# GET 작업
$jobs += Start-Job -ScriptBlock {
    param($BaseUrl, $ApiKey)
    $headers = @{ "X-API-Key" = $ApiKey }
    $count = 0
    for ($i = 0; $i -lt 500; $i++) {
        try {
            Invoke-WebRequest -Uri "$BaseUrl/api/v1/logs/search?page=0&size=20" -Method Get -Headers $headers -UseBasicParsing | Out-Null
            $count++
        } catch {}
    }
    return $count
} -ArgumentList $BaseUrl, $ApiKey

# Stats 작업
$jobs += Start-Job -ScriptBlock {
    param($BaseUrl, $ApiKey)
    $headers = @{ "X-API-Key" = $ApiKey }
    $count = 0
    for ($i = 0; $i -lt 200; $i++) {
        try {
            Invoke-WebRequest -Uri "$BaseUrl/api/v1/statistics/overall" -Method Get -Headers $headers -UseBasicParsing | Out-Null
            $count++
        } catch {}
    }
    return $count
} -ArgumentList $BaseUrl, $ApiKey

# 작업 완료 대기
$results = $jobs | Wait-Job | Receive-Job
$jobs | Remove-Job

Write-Host "  POST 성공: $($results[0])" -ForegroundColor Green
Write-Host "  GET 성공: $($results[1])" -ForegroundColor Green
Write-Host "  STATS 성공: $($results[2])" -ForegroundColor Green
Write-Host ""

Write-Host "========================================" -ForegroundColor Blue
Write-Host "✓ 성능 테스트 완료!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Blue
Write-Host ""
Write-Host "권장사항:" -ForegroundColor Yellow
Write-Host "  - Grafana 대시보드에서 실시간 메트릭을 확인하세요"
Write-Host "  - Prometheus에서 응답 시간, 에러율을 모니터링하세요"
Write-Host "  - 높은 부하에서 메모리, CPU 사용률을 체크하세요"
