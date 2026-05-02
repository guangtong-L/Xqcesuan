#!/usr/bin/env bash
# 5 分钟接口冒烟（后端启动后跑）
# 期望全部 code=0 / 1002 / 1003，traceId 非空
set -e
BASE="${BASE:-http://localhost:8080}"
OPENID="mock_openid_local_dev"

hr() { echo; echo "===== $1 ====="; }

hr "stats/today"
curl -s "$BASE/api/stats/today" | tee /tmp/r.json && echo

hr "auth/login"
curl -s -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" -H "X-WX-OPENID: $OPENID" \
  -d '{"code":"local_mock_code"}' | tee /tmp/r.json && echo

hr "fortune/calc 第 1 次"
RESP=$(curl -s -X POST "$BASE/api/fortune/calc" \
  -H "Content-Type: application/json" -H "X-WX-OPENID: $OPENID" \
  -d '{"birthday":"1995-08-15","birthHour":14,"gender":"M","tags":["career","love"],"calendarType":"solar"}')
echo "$RESP"
RECORD_ID=$(echo "$RESP" | sed 's/.*"recordId":"\([^"]*\)".*/\1/')
SCORE1=$(echo "$RESP" | grep -o '"score":[0-9]*' | head -1 | cut -d: -f2)
echo "recordId=$RECORD_ID score=$SCORE1"

hr "fortune/calc 第 2 次（应同 score）"
SCORE2=$(curl -s -X POST "$BASE/api/fortune/calc" \
  -H "Content-Type: application/json" -H "X-WX-OPENID: $OPENID" \
  -d '{"birthday":"1995-08-15","birthHour":14,"gender":"M","tags":["career","love"],"calendarType":"solar"}' \
  | grep -o '"score":[0-9]*' | head -1 | cut -d: -f2)
[[ "$SCORE1" == "$SCORE2" ]] && echo "PASS 幂等 score=$SCORE2" || { echo "FAIL: $SCORE1 vs $SCORE2"; exit 1; }

hr "fortune/history"
curl -s "$BASE/api/fortune/history?page=1&pageSize=10" -H "X-WX-OPENID: $OPENID" | tee /tmp/r.json && echo

hr "fortune/unlock"
curl -s -X POST "$BASE/api/fortune/unlock" \
  -H "Content-Type: application/json" -H "X-WX-OPENID: $OPENID" \
  -d "{\"recordId\":\"$RECORD_ID\",\"adToken\":\"mock_ad_token\"}" | tee /tmp/r.json && echo

hr "未来生日 → 1003"
curl -s -X POST "$BASE/api/fortune/calc" \
  -H "Content-Type: application/json" -H "X-WX-OPENID: $OPENID" \
  -d '{"birthday":"2099-01-01","gender":"M"}' && echo

hr "gender 非法 → 1002"
curl -s -X POST "$BASE/api/fortune/calc" \
  -H "Content-Type: application/json" -H "X-WX-OPENID: $OPENID" \
  -d '{"birthday":"1995-08-15","gender":"X"}' && echo

hr "delete 单条 → deleted=1"
curl -s -X DELETE "$BASE/api/fortune/history/$RECORD_ID" -H "X-WX-OPENID: $OPENID" && echo

hr "DONE"
