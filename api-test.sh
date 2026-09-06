#!/usr/bin/env bash
# API smoke test against a RUNNING backend instance (real PUBG data, real HTTP).
# This exists because the AI assistant building this app cannot run it in its own
# sandbox (no outbound network) - run this yourself after `mvn spring-boot:run`.
#
# Every call prints the real HTTP status and response body it got back, THEN a
# PASS/FAIL verdict - so you can see the actual data, not just trust the verdict.
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
SKIP=0

# Performs the GET, prints the real status + body, and verdicts against expectations.
# Sets $body and $status as side effects so callers (e.g. to extract an id) can use them.
call() {
  local description="$1"
  local url="$2"
  local expected_status="$3"
  local body_contains="${4:-}"

  local resp
  resp=$(curl -s -w "\n%{http_code}" "$url")
  body=$(echo "$resp" | sed '$d')
  status=$(echo "$resp" | tail -n 1)

  echo "--> $description"
  echo "    GET $url"
  echo "    HTTP $status"
  echo "    $body"

  if [ "$status" != "$expected_status" ]; then
    echo "    VERDICT: FAIL (expected HTTP $expected_status, got $status)"
    FAIL=$((FAIL + 1))
    echo
    return
  fi

  if [ -n "$body_contains" ] && [[ "$body" != *"$body_contains"* ]]; then
    echo "    VERDICT: FAIL (response missing expected content: $body_contains)"
    FAIL=$((FAIL + 1))
    echo
    return
  fi

  echo "    VERDICT: PASS"
  PASS=$((PASS + 1))
  echo
}

echo "== API smoke test against $BASE_URL (player: $PLAYER_NAME) =="
echo "Every call below shows the real status + body it got back - check it yourself,"
echo "don't just trust the PASS/FAIL line."
echo

if ! curl -s -o /dev/null "$BASE_URL/health"; then
  echo "ERROR: could not reach $BASE_URL at all."
  echo "Is the backend running? Try: mvn spring-boot:run"
  exit 1
fi

call "1. Health check" "$BASE_URL/health" "200" "UP"

call "2. Player search (should exist)" "$BASE_URL/api/players/$PLAYER_NAME" "200" "\"shardId\""

# Extract id and first recent match id from the last call's $body, without needing jq.
# Tolerant of both compact ("id":"x") and spaced ("id": "x") JSON - grep for the key
# then pull out just the trailing quoted value, rather than assuming zero whitespace.
PLAYER_ID=$(echo "$body" | grep -o '"id"[[:space:]]*:[[:space:]]*"[^"]*"' | head -n 1 | grep -o '"[^"]*"$' | tr -d '"')
FIRST_MATCH_ID=$(echo "$body" | grep -o '"recentMatchIds"[[:space:]]*:[[:space:]]*\[[^]]*\]' | grep -o '"[a-f0-9-]\{10,\}"' | head -n 1 | tr -d '"')
echo "    (extracted PLAYER_ID=${PLAYER_ID:-<none - see warning below>}, FIRST_MATCH_ID=${FIRST_MATCH_ID:-none})"
echo

call "3. Player search (nonsense name, should 404)" \
  "$BASE_URL/api/players/definitely-not-a-real-player-xyz123" "404" "error"

if [ -n "$PLAYER_ID" ]; then
  call "4. Season stats" "$BASE_URL/api/players/$PLAYER_ID/season-stats" "200" "winRate"
else
  echo "SKIP: season-stats (could not extract player id from call #2's response - this"
  echo "      means call #2's response shape changed, NOT that season-stats works)"
  echo
  SKIP=$((SKIP + 1))
fi

if [ -n "$PLAYER_ID" ] && [ -n "$FIRST_MATCH_ID" ]; then
  call "5. Match stats" "$BASE_URL/api/players/$PLAYER_ID/matches/$FIRST_MATCH_ID" "200" "headshotRate"
else
  echo "SKIP: match stats (player has no recent matches, or id extraction failed above)"
  echo
  SKIP=$((SKIP + 1))
fi

echo "== Results: $PASS passed, $FAIL failed, $SKIP skipped =="
if [ "$SKIP" -gt 0 ]; then
  echo "WARNING: $SKIP check(s) were skipped, not verified - do not treat this run as a full pass."
fi
[ "$FAIL" -eq 0 ] && [ "$SKIP" -eq 0 ]
