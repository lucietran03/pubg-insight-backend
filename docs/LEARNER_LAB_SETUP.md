# AWS Academy Learner Lab — setup guide

Two situations, two sections. Find yours, skip the rest:

- **First time ever** → do [A] then [B], in that order.
- **Every other time you sit down to work** → just do [A].

`docs/LEARNER_LAB.md` is the official, unedited AWS Academy readme — the source of truth if anything here looks outdated. Exact button labels can shift slightly between versions; if something's named a little differently, look for the closest match.

---

## [A] Every session: start the lab, get fresh credentials

Credentials expire every session (a few hours) — repeat this every time, even if you did it yesterday.

1. Log into **AWS Academy** (`awsacademy.instructure.com`, or via the RMIT/Canvas link).
2. **Courses** → your course → **Modules** → click **Learner Lab**.
3. Click the black **Start Lab** button.
4. Wait for the circle next to **AWS** to turn **green** (1–3 min). Don't click anything else meanwhile.
5. Click **AWS Details**.
6. Note the **Region** shown (e.g. `us-east-1`) — see step 9 below.
7. Under **AWS CLI**, click **Show**. Copy the whole 4-line block:
   ```
   [default]
   aws_access_key_id=...
   aws_secret_access_key=...
   aws_session_token=...
   ```
8. Paste it into `~/.aws/credentials` on your machine, **replacing** anything already there (an old block left in this file causes the exact "token is malformed or otherwise invalid" error — always fully overwrite, never append).
   ```bash
   nano ~/.aws/credentials   # or open in any editor
   ```
9. Region only needs setting once (it doesn't change between sessions): `export AWS_REGION=us-east-1` in your shell profile (`~/.zshrc`), matching whatever step 6 showed. If you skip this, the app defaults to `us-east-1` anyway.
10. Run the backend. Search a player, open a match. Check the console:
    - See `S3Exception: The provided token is malformed...` → credentials are stale, redo steps 3–8.
    - No such warning → AWS calls are going through.

---

## [B] First time ever: create the S3 bucket + DynamoDB table

Do this **once**, after completing [A] at least once (you need working credentials to reach the AWS Console). It survives every future session — only the credentials from [A] expire, not what you create here.

**S3 bucket:**
1. Click the **AWS** button (opens the AWS Console).
2. Search **S3** in the top bar → click into it → **Create bucket**.
3. Name it `pubg-insight-match-cache`.
   - S3 names must be **globally unique across all of AWS** — if that name's taken, add a suffix, e.g. `pubg-insight-match-cache-nghi2026`.
   - If you used a suffix, add this to `application-local.yml`:
     ```yaml
     aws:
       s3:
         cache-bucket: pubg-insight-match-cache-nghi2026
     ```
4. Leave everything else default → **Create bucket**.

**DynamoDB table:**
1. Search **DynamoDB** in the Console's top bar → click into it → **Create table**.
2. Table name: `pubg-insight-analysis-history`.
3. Partition key: `playerId`, type **String**.
4. Sort key: `matchId`, type **String**.
5. Leave everything else default → **Create table**.

Done — restart the backend and both AWS integrations should work.

---

## Later (not needed yet — deployment step)

Elastic Beanstalk deployment, once the backend is actually ready to go live:

1. AWS Console → search **Elastic Beanstalk** → **Create Application**.
2. Name it, pick the Java/Corretto platform (closest match to Java 21).
3. **Configure more options** → **Security** panel → **Edit**:
   - **Service role** → `LabRole` (never "create new" — the Lab blocks it).
   - In `us-east-1`: **EC2 key pair** → `vockey`, **IAM instance profile** → `LabInstanceProfile`.
4. **Save** → **Create app**.

Instance size is capped at nano/micro/small/medium/large — anything bigger gets auto-terminated.

Every approved service (API Gateway, Lambda, DynamoDB, S3, Athena) already supports attaching `LabRole` directly — whenever a setup wizard asks for a role, the answer is always the existing `LabRole`, never a custom one.
