import { DynamoDBClient, GetItemCommand } from "@aws-sdk/client-dynamodb";
import { unmarshall } from "@aws-sdk/util-dynamodb";

const TABLE_NAME = process.env.ANALYSIS_HISTORY_TABLE ?? "pubg-insight-analysis-history";
const APP_URL = process.env.APP_URL ?? "https://d13c09lhflfxxl.cloudfront.net";

const client = new DynamoDBClient({});

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function page(title, bodyHtml, statusCode) {
  return {
    statusCode,
    headers: { "Content-Type": "text/html; charset=utf-8" },
    body: `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<title>${escapeHtml(title)} — PUBG Insight</title>
<style>
  body { margin: 0; background: #121212; color: #f5f5f5; font-family: system-ui, -apple-system, "Segoe UI", Roboto, sans-serif; }
  .wrap { max-width: 560px; margin: 0 auto; padding: 32px 20px; }
  .brand { font-weight: 800; letter-spacing: 1px; color: #F2A900; margin-bottom: 24px; }
  .card { background: #1C1C1C; border-radius: 8px; padding: 24px; }
  h1 { font-size: 1.4rem; margin: 0 0 4px; }
  .muted { color: #9c9c9c; font-size: 0.85rem; }
  .stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin: 20px 0; text-align: center; }
  .stat-value { font-size: 1.4rem; font-weight: 800; }
  .stat-label { font-size: 0.75rem; color: #9c9c9c; text-transform: uppercase; letter-spacing: 0.5px; }
  .summary { line-height: 1.6; margin: 16px 0; }
  .strength { color: #F2A900; font-weight: 700; margin-top: 12px; }
  a.cta { display: inline-block; margin-top: 24px; color: #121212; background: #F2A900; padding: 10px 18px; border-radius: 6px; text-decoration: none; font-weight: 700; }
</style>
</head>
<body>
  <div class="wrap">
    <div class="brand">PUBG INSIGHT</div>
    <div class="card">${bodyHtml}</div>
  </div>
</body>
</html>`,
  };
}

export const handler = async (event) => {
  const playerId = event.pathParameters?.playerId;
  const matchId = event.pathParameters?.matchId;

  if (!playerId || !matchId) {
    return page("Invalid link", "<h1>Invalid share link</h1><p class=\"muted\">Missing player or match id.</p>", 400);
  }

  let item;
  try {
    const result = await client.send(
      new GetItemCommand({
        TableName: TABLE_NAME,
        Key: { playerId: { S: playerId }, matchId: { S: matchId } },
      })
    );
    item = result.Item ? unmarshall(result.Item) : null;
  } catch (err) {
    console.error("DynamoDB read failed", err);
    return page("Error", "<h1>Something went wrong</h1><p class=\"muted\">Please try again later.</p>", 500);
  }

  if (!item) {
    return page(
      "Not found",
      '<h1>This analysis is no longer available</h1><p class="muted">It may have expired or the link is incorrect.</p>' +
        `<a class="cta" href="${escapeHtml(APP_URL)}">Open PUBG Insight</a>`,
      404
    );
  }

  const topStrength = item.strengths?.[0];

  const body = `
    <h1>${escapeHtml(item.mapName)} · ${escapeHtml(item.gameMode)}</h1>
    <p class="muted">Placement #${escapeHtml(item.winPlace)} · analyzed ${escapeHtml(new Date(item.createdAt).toLocaleDateString())}</p>
    <div class="stats">
      <div><div class="stat-value">${escapeHtml(item.kills)}</div><div class="stat-label">Kills</div></div>
      <div><div class="stat-value">${escapeHtml(Math.round(item.damageDealt))}</div><div class="stat-label">Damage</div></div>
      <div><div class="stat-value">${escapeHtml(Math.round((item.headshotRate ?? 0) * 100))}%</div><div class="stat-label">Headshot</div></div>
    </div>
    ${item.insightSummary ? `<p class="summary">${escapeHtml(item.insightSummary)}</p>` : ""}
    ${topStrength ? `<p class="strength">✓ ${escapeHtml(topStrength)}</p>` : ""}
    <a class="cta" href="${escapeHtml(APP_URL)}">View full analysis on PUBG Insight</a>
  `;

  return page(`${item.mapName} analysis`, body, 200);
};
