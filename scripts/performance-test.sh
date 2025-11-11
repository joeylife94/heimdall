#!/bin/bash

# Heimdall 성능 테스트 스크립트
# Apache Bench (ab)를 사용한 부하 테스트

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 설정
BASE_URL="${BASE_URL:-http://localhost:8080}"
API_KEY="${API_KEY:-test-api-key}"
CONCURRENT_REQUESTS="${CONCURRENT_REQUESTS:-100}"
TOTAL_REQUESTS="${TOTAL_REQUESTS:-10000}"
TIMEOUT="${TIMEOUT:-30}"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Heimdall 성능 테스트${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${YELLOW}설정:${NC}"
echo "  - Base URL: $BASE_URL"
echo "  - 동시 요청: $CONCURRENT_REQUESTS"
echo "  - 총 요청: $TOTAL_REQUESTS"
echo "  - 타임아웃: ${TIMEOUT}s"
echo ""

# 필수 도구 확인
command -v ab >/dev/null 2>&1 || {
    echo -e "${RED}❌ Apache Bench (ab)가 설치되어 있지 않습니다.${NC}"
    echo "설치 방법: sudo apt-get install apache2-utils (Ubuntu/Debian)"
    exit 1
}

command -v jq >/dev/null 2>&1 || {
    echo -e "${YELLOW}⚠️  jq가 설치되어 있지 않습니다. JSON 파싱이 제한됩니다.${NC}"
}

# 테스트 데이터 준비
TEST_DATA=$(cat <<EOF
{
  "source": "performance-test",
  "message": "Performance test log message with some data to simulate real workload",
  "severity": "INFO",
  "timestamp": "$(date -u +"%Y-%m-%dT%H:%M:%S.%3NZ")",
  "metadata": {
    "test": true,
    "environment": "performance",
    "iteration": 1
  }
}
EOF
)

echo "$TEST_DATA" > /tmp/heimdall-test-data.json

# 헤더 파일 생성
cat > /tmp/heimdall-test-headers.txt <<EOF
Content-Type: application/json
X-API-Key: $API_KEY
EOF

echo -e "${GREEN}✓ 테스트 준비 완료${NC}"
echo ""

# 1. Health Check 테스트
echo -e "${BLUE}[1/5] Health Check 테스트${NC}"
echo "------------------------------------"
ab -n 100 -c 10 \
   -t $TIMEOUT \
   "${BASE_URL}/actuator/health" \
   2>&1 | grep -E "Requests per second|Time per request|Transfer rate|Failed requests"
echo ""

# 2. 로그 수집 테스트 (POST)
echo -e "${BLUE}[2/5] 로그 수집 (POST) 테스트${NC}"
echo "------------------------------------"
ab -n $TOTAL_REQUESTS -c $CONCURRENT_REQUESTS \
   -t $TIMEOUT \
   -p /tmp/heimdall-test-data.json \
   -T "application/json" \
   -H "X-API-Key: $API_KEY" \
   "${BASE_URL}/api/v1/logs" \
   2>&1 | grep -E "Requests per second|Time per request|Transfer rate|Failed requests|Percentage of"
echo ""

# 3. 로그 검색 테스트 (GET)
echo -e "${BLUE}[3/5] 로그 검색 (GET) 테스트${NC}"
echo "------------------------------------"
ab -n $((TOTAL_REQUESTS / 2)) -c $CONCURRENT_REQUESTS \
   -t $TIMEOUT \
   -H "X-API-Key: $API_KEY" \
   "${BASE_URL}/api/v1/logs/search?page=0&size=20" \
   2>&1 | grep -E "Requests per second|Time per request|Transfer rate|Failed requests|Percentage of"
echo ""

# 4. 통계 조회 테스트
echo -e "${BLUE}[4/5] 통계 조회 테스트${NC}"
echo "------------------------------------"
ab -n 1000 -c 50 \
   -t $TIMEOUT \
   -H "X-API-Key: $API_KEY" \
   "${BASE_URL}/api/v1/statistics/overall" \
   2>&1 | grep -E "Requests per second|Time per request|Transfer rate|Failed requests"
echo ""

# 5. 혼합 부하 테스트 (병렬 실행)
echo -e "${BLUE}[5/5] 혼합 부하 테스트 (병렬)${NC}"
echo "------------------------------------"
echo "POST, GET, Stats 엔드포인트를 동시에 테스트합니다..."

# 백그라운드에서 실행
ab -n 5000 -c 50 \
   -p /tmp/heimdall-test-data.json \
   -T "application/json" \
   -H "X-API-Key: $API_KEY" \
   "${BASE_URL}/api/v1/logs" > /tmp/ab-post.log 2>&1 &
POST_PID=$!

ab -n 5000 -c 50 \
   -H "X-API-Key: $API_KEY" \
   "${BASE_URL}/api/v1/logs/search?page=0&size=20" > /tmp/ab-get.log 2>&1 &
GET_PID=$!

ab -n 2000 -c 25 \
   -H "X-API-Key: $API_KEY" \
   "${BASE_URL}/api/v1/statistics/overall" > /tmp/ab-stats.log 2>&1 &
STATS_PID=$!

# 모든 테스트 완료 대기
wait $POST_PID
wait $GET_PID
wait $STATS_PID

echo ""
echo -e "${GREEN}POST 결과:${NC}"
grep -E "Requests per second|Failed requests" /tmp/ab-post.log

echo ""
echo -e "${GREEN}GET 결과:${NC}"
grep -E "Requests per second|Failed requests" /tmp/ab-get.log

echo ""
echo -e "${GREEN}STATS 결과:${NC}"
grep -E "Requests per second|Failed requests" /tmp/ab-stats.log

# 정리
rm -f /tmp/heimdall-test-data.json
rm -f /tmp/heimdall-test-headers.txt
rm -f /tmp/ab-*.log

echo ""
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}✓ 성능 테스트 완료!${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${YELLOW}권장사항:${NC}"
echo "  - Grafana 대시보드에서 실시간 메트릭을 확인하세요"
echo "  - Prometheus에서 응답 시간, 에러율을 모니터링하세요"
echo "  - 높은 부하에서 메모리, CPU 사용률을 체크하세요"
