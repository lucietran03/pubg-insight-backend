# AWS Academy Learner Lab — step-by-step setup guide

This is a click-by-click guide for getting AWS credentials from the RMIT-provided AWS Academy Learner Lab and putting them where this app can use them. Written for someone who has never used AWS Academy before.

Exact button labels can shift slightly between AWS Academy versions — if something is named a little differently than described here, look for the closest match; the overall flow (Start Lab → wait for green → open AWS Details → copy credentials) has been stable for years.

---

## 1. Get into the Learner Lab

1. Log into **AWS Academy** at `https://awsacademy.instructure.com` (or via the link RMIT gave you — it may go through Canvas first, then hand off to AWS Academy).
2. On the left sidebar, click **Courses**, then click into your course (something like *"AWS Academy Learner Lab - Foundation Services"* or whatever your instructor named it).
3. Click **Modules** in the course's left sidebar.
4. Find and click the module item literally called **Learner Lab** (sometimes shown as "Learner Lab - Foundation Services" or similar — it's the one that opens the lab launcher, not a reading/quiz item).

This opens the **Learner Lab launcher page** — a mostly-white page with a black **Start Lab** button near the top, and a light gray/green circle icon next to the word **AWS** near the top right.

---

## 2. Start the lab

1. Click the black **Start Lab** button.
2. Wait. A small circle icon next to the **AWS** text will be **gray/orange while starting**, then turn **green** once the lab environment is ready. This usually takes 1–3 minutes — don't click anything else while waiting.
3. Once the circle is green, the lab is running. It stays running for a limited time (commonly a few hours per session, shown as a countdown timer on this same page) — if it times out, you just repeat this whole guide to start a new session and get fresh credentials.

---

## 3. Get your AWS credentials

You do **not** need to log into the AWS Console with a username/password — the Lab gives you short-lived credentials directly.

1. Still on the Learner Lab page, click the **AWS Details** button (it appears near the top once the lab is started/green — sometimes it's a link that just says "AWS Details", sometimes it's next to or replaces the "AWS" text/circle you saw in step 2).
2. A panel opens showing:
   - **Region** — a string like `us-east-1`. **Write this down** — you need it for `AWS_REGION` (see step 5 below).
   - An **AWS CLI** section with a **Show** link/button.
3. Click **Show** under the AWS CLI section. It reveals a text block that looks like this (values below are examples, not real):
   ```
   [default]
   aws_access_key_id=ASIAABCDEFGHIJK12345
   aws_secret_access_key=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
   aws_session_token=FQoGZXIvYXdzEBEaDMlong/random/string...
   ```
4. Select and copy that **entire block** (all 4 lines, including the `[default]` line).

If instead of "AWS Details" you see a button that just says **AWS** and clicking it opens the AWS Management Console directly in a new tab — that also works fine for one-off manual checks (e.g. confirming a DynamoDB table exists), but for this app to run locally you still need the copied credentials block from "AWS Details", not just console access.

---

## 4. Put the credentials where your machine can find them

The AWS SDK (used by this backend) looks for credentials in `~/.aws/credentials` by default.

1. Open a terminal.
2. Check if the file/folder already exist:
   ```bash
   mkdir -p ~/.aws
   ```
   (safe to run even if `~/.aws` already exists — it does nothing in that case)
3. Open `~/.aws/credentials` in any text editor (create it if it doesn't exist):
   ```bash
   nano ~/.aws/credentials
   ```
   (or open it in VS Code / IntelliJ / any editor you prefer)
4. **Replace the entire contents of the file** with the block you copied in step 3.4 above — if the file already has an old `[default]` block from a previous session, delete it first (old, expired credentials left in the file will get picked up instead of the new ones and cause the exact "token is malformed or otherwise invalid" error seen before).
5. Save and close the file.

**This step must be repeated every time you start a new Lab session** — the credentials are temporary and stop working when the session ends or times out (usually after a few hours). If you restart work on a different day, assume you need to redo steps 2–4.

---

## 5. Set the region for this app

The region from step 3.2 (e.g. `us-east-1`) needs to reach the app too:

- **Easiest**: set it as an environment variable before running the backend:
  ```bash
  export AWS_REGION=us-east-1   # replace with your actual region from AWS Details
  ```
  (add this line to your shell profile, e.g. `~/.zshrc`, so you don't have to re-type it every terminal session — though you'll still need to redo step 4 for credentials each Lab session)
- **Alternative**: set it directly in `src/main/resources/application-local.yml` under `aws.region:` (see that file — it currently reads `${AWS_REGION:us-east-1}`, so if you don't export the env var, it silently falls back to `us-east-1` as a default; only relevant if the Lab actually gives you a different region).

---

## 6. Verify it worked

1. Run the backend locally as usual.
2. Search for a player and view one of their matches — this triggers an S3 cache read/write in `MatchService`.
3. Check the backend's console/log output:
   - If you see `S3 match cache read failed ... falling back to PUBG API` with a `WARN` and a cause like `S3Exception: The provided token is malformed or otherwise invalid` → credentials are still wrong/expired, redo steps 2–4.
   - If you see no such warning at all → the S3 cache is working. The app still functions correctly either way (see `docs/ARCHITECTURE.md` design decision D12) — this step is just to confirm the AWS integration itself is live, not just still falling back.

---

## 7. One-time setup: create the actual DynamoDB table and S3 bucket

Credentials alone aren't enough the first time — the table/bucket this app expects don't exist until you create them once. This is manual Console setup, which is explicitly allowed by the assignment rubric (only the *runtime* behavior needs to be automated, not initial resource creation).

1. Click the **AWS** button/link from the Learner Lab page (opens the AWS Console, already logged in).
2. In the Console's top search bar, type **DynamoDB** and click into that service.
3. Click **Create table**.
   - Table name: `pubg-insight-analysis-history` (must match `DYNAMODB_ANALYSIS_HISTORY_TABLE` in `application.yml`, or set that env var to whatever you name it instead)
   - Partition key: `playerId`, type **String**
   - Sort key: `matchId`, type **String**
   - Leave other settings at their defaults, click **Create table**.
4. Back in the Console's search bar, type **S3** and click into that service.
5. Click **Create bucket**.
   - Bucket name: `pubg-insight-match-cache` (must match `S3_CACHE_BUCKET` in `application.yml`, or set that env var instead — note S3 bucket names must be globally unique across all of AWS, so you may need to add a suffix, e.g. `pubg-insight-match-cache-yourname`, and update the env var to match)
   - Leave other settings at their defaults, click **Create bucket**.

Do this once per Lab account (it persists across Lab session restarts — only the *credentials* expire, not the resources you created with them).
