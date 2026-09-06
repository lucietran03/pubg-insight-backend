#!/usr/bin/env bash
# API smoke test against a RUNNING backend instance (real PUBG data, real HTTP).
# This exists because the AI assistant building this app cannot run it in its own
# sandbox (no outbound network) - run this yourself after `mvn spring-boot:run`.
#
# Usage:
#   ./api-test.sh [player-name] [base-url]
#
# Examples:
#   ./api-test.sh                       # uses default player name, http://localhost:8080
#   ./api-test.sh TGLTN                 # a specific player
#   ./api-test.sh TGLTN http://localhost:8080

set -uo pipefail

PLAYER_NAME="${1:-TGLTN}"
BASE_URL="${2:-http://localhost:8080}"

PASS=0
FAIL=0

check() {
  local description="$1"
  local expected_status="$2"
  local actual_status="$3"
  local body="$4"
  local body_contains="${5:-}"

  if [ "$actual_status" != "$expected_status" ]; then
    echo "FAIL: $description (expected HTTP $expected_status, got $actual_status)"
    echo "      body: $body"
    FAIL=$((FAIL + 1))
    return
  fi

  if [ -n "$body_contains" ] && [[ "$body" != *"$body_contains"* ]]; then
    echo "FAIL: $description (response missing expected content: $body_contains)"
    echo "      body: $body"
    FAIL=$((FAIL + 1))
    return
  fi

  echo "PASS: $description"
  PASS=$((PASS + 1))
}

echo "== API smoke test against $BASE_URL (player: $PLAYER_NAME) =="
echo

if ! curl -s -o /dev/null "$BASE_URL/health"; then
  echo "ERROR: could not reach $BASE_URL at all."
  echo "Is the backend running? Try: mvn spring-boot:run"
  exit 1
fi

# 1. Health check
resp=$(curl -s -w "\n%{http_code}" "$BASE_URL/health")
body=$(echo "$resp" | sed '$d')
status=$(echo "$resp" | tail -n 1)
check "GET /health" "200" "$status" "$body" "UP"

# 2. Player search - success case
resp=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/players/$PLAYER_NAME")
body=$(echo "$resp" | sed '$d')
status=$(echo "$resp" | tail -n 1)
check "GET /api/players/$PLAYER_NAME (found)" "200" "$status" "$body" "\"shardId\""

# Extract id and first recent match id without requiring jq to be installed.
PLAYER_ID=$(echo "$body" | grep -o '"id":"[^"]*"' | head -n 1 | sed -E 's/"id":"([^"]*)"/\1/')
FIRST_MATCH_ID=$(echo "$body" | grep -o '"recentMatchIds":\[[^]]*\]' | grep -o '"[a-f0-9-]\{10,\}"' | head -n 1 | tr -d '"')

# 3. Player search - not found case
resp=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/players/definitely-not-a-real-player-xyz123")
body=$(echo "$resp" | sed '$d')
status=$(echo "$resp" | tail -n 1)
check "GET /api/players/<nonsense> (not found)" "404" "$status" "$body" "error"

# 4. Season stats (Win Rate)
if [ -n "$PLAYER_ID" ]; then
  resp=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/players/$PLAYER_ID/season-stats")
  body=$(echo "$resp" | sed '$d')
  status=$(echo "$resp" | tail -n 1)
  check "GET /api/players/$PLAYER_ID/season-stats" "200" "$status" "$body" "winRate"
else
  echo "SKIP: season-stats (could not extract player id from search response)"
fi

# 5. Match stats (only possible if the player has a recent match)
if [ -n "$PLAYER_ID" ] && [ -n "$FIRST_MATCH_ID" ]; then
  resp=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/players/$PLAYER_ID/matches/$FIRST_MATCH_ID")
  body=$(echo "$resp" | sed '$d')
  status=$(echo "$resp" | tail -n 1)
  check "GET /api/players/$PLAYER_ID/matches/$FIRST_MATCH_ID" "200" "$status" "$body" "headshotRate"
else
  echo "SKIP: match stats (player has no recent matches, or id extraction failed)"
fi

echo
echo "== Results: $PASS passed, $FAIL failed =="
[ "$FAIL" -eq 0 ]
