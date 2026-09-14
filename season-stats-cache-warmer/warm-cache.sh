#!/bin/bash
set -euo pipefail

TABLE_NAME="${ANALYSIS_HISTORY_TABLE:-pubg-insight-analysis-history}"
BACKEND_BASE_URL="${BACKEND_BASE_URL:?BACKEND_BASE_URL env var required}"

# The AWS CLI auto-paginates scan by default, so this already covers every distinct
# player anyone has ever analyzed, not just the first page.
player_ids=$(aws dynamodb scan \
  --table-name "$TABLE_NAME" \
  --projection-expression "playerId" \
  --output text \
  --query "Items[].playerId.S" | tr '\t' '\n' | sort -u)

count=0
failed=0
for player_id in $player_ids; do
  echo "Warming season-stats cache for player $player_id"
  if curl -sf --max-time 30 "${BACKEND_BASE_URL}/api/players/${player_id}/season-stats" -o /dev/null; then
    count=$((count + 1))
  else
    echo "Failed to warm cache for $player_id"
    failed=$((failed + 1))
  fi
done

echo "Warmed season-stats cache for $count player(s), $failed failure(s)"
