# AWS setup guide (personal account)

Switched from the RMIT AWS Academy Learner Lab to a personal AWS account (too many Lab limits — no custom IAM roles, session credentials expiring every few hours). Using the AWS Free Tier + $100 promotional credit. `docs/decisions/LEARNER_LAB.md` is now historical only (kept for reference, no longer the active target).

Credentials are now permanent — no more "start the lab, get fresh creds" ritual. Do [A] once, then [B] once. Both survive forever unless you rotate/delete them yourself.

---

## [A] One-time: IAM user + access key

1. AWS Console → search **IAM** → **Users** → **Create user**.
2. Name: `pubg-insight-dev`. Don't tick "Provide user access to the AWS Management Console" (not needed — this user is for programmatic/SDK access only).
3. **Attach policies directly** → `AmazonS3FullAccess`, `AmazonDynamoDBFullAccess`, `AdministratorAccess-AWSElasticBeanstalk` (add `AWSLambda_FullAccess`, `AmazonAPIGatewayAdministrator`, `AmazonAthenaFullAccess` when those phases start).
4. Open the user → **Security credentials** tab → **Create access key** → choose **Command Line Interface (CLI)** → confirm → **Create access key**.
5. Copy the **Access key ID** + **Secret access key** immediately (shown once only).
6. Paste into `~/.aws/credentials`, no `aws_session_token` line needed:
   ```bash
   mkdir -p ~/.aws
   nano ~/.aws/credentials
   ```
   ```
   [default]
   aws_access_key_id=...
   aws_secret_access_key=...
   ```
7. Set the region once: `export AWS_REGION=us-east-1` in `~/.zshrc`. If skipped, the app defaults to `us-east-1` anyway.

**Never save the downloaded access-key CSV inside either project repo** — it's a real, permanent secret, not a Learner Lab session token that expires on its own. Keep it outside both repos (e.g. a password manager), and if it's already inside a repo folder, delete it there even if not yet committed.

Run the backend, search a player, open a match. `S3Exception`/`AccessDenied` in the console → recheck the credentials file, not a lab session (there's no session to expire anymore).

---

## [B] One-time: create the S3 bucket + DynamoDB table

**S3 bucket:**
1. **Check the region selector, top-right of the Console, before doing anything else.** A personal account's Console can default to any region (e.g. Sydney `ap-southeast-2`) — this is not the Lab, which was locked to one region. If it doesn't already say **US East (N. Virginia) `us-east-1`**, click it and switch. Creating the bucket in the wrong region means the backend (configured for `us-east-1`) will never find it.
2. Search **S3** → **Create bucket**.
3. Fill in exactly:

   | Field | Value |
   |---|---|
   | AWS Region | `us-east-1` (from step 1) |
   | Bucket namespace | default (Global namespace) |
   | Bucket name | `pubg-insight-match-cache` |
   | Copy settings from existing bucket | skip |
   | Object Ownership | default — ACLs disabled (recommended) |
   | Block Public Access | default — Block all public access **on** |
   | Bucket Versioning | default — Disable |
   | Tags | skip |
   | Default encryption | default — SSE-S3 |
   | Bucket Key | default |
   | Object Lock | default — Disable |

   - Bucket name must be **globally unique across all of AWS** — if taken, add a suffix, e.g. `pubg-insight-match-cache-nghi2026`.
   - If suffixed, add to `application-local.yml`:
     ```yaml
     aws:
       s3:
         cache-bucket: pubg-insight-match-cache-nghi2026
     ```
4. **Create bucket**.

**DynamoDB table:**
0. Same region check as above — must still say `us-east-1` top-right (the Console remembers your last pick, so this should already be correct).
1. Search **DynamoDB** → **Create table**.
2. Table name: `pubg-insight-analysis-history`.
3. Partition key: `playerId`, type **String**.
4. Sort key: `matchId`, type **String**.
5. Leave everything else default → **Create table**.

Done — restart the backend and both AWS integrations should work.

---

## Later: Elastic Beanstalk deployment

1. AWS Console → search **Elastic Beanstalk** → **Create Application**.
2. Name it, pick the Java/Corretto platform (closest match to Java 21).
3. **Configure more options** → **Security** panel → **Edit** → **Service role** → **Create and use new service role** (a normal account can create its own — no `LabRole` substitute needed).
4. **Save** → **Create app**.

Watch instance size/hours against the Free Tier (`t2.micro`/`t3.micro`) and the $100 credit — unlike the Lab, cost overruns here are real money. Set a **Billing alarm** (Billing Console → Budgets) early, e.g. at $20, as a safety net.

Every approved service (API Gateway, Lambda, DynamoDB, S3, Athena) — when a setup wizard asks for an execution/service role, let it create a new one (or reuse `pubg-insight-dev`'s policies) rather than looking for a pre-provisioned role like the Lab had.
