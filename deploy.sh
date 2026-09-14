#!/bin/bash
# Builds the backend jar locally and deploys it to the existing Elastic Beanstalk
# environment. Run this from a machine with real Maven Central access - the sandbox
# this repo is normally edited in has no path to repo.maven.apache.org.
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"

APPLICATION_NAME="pubg-insight-backend"
ENVIRONMENT_NAME="Pubg-insight-backend-env"
S3_BUCKET="elasticbeanstalk-us-east-1-262666781802"
REGION="us-east-1"
JAR_PATH="target/pubg-insight-backend-0.0.1-SNAPSHOT.jar"
VERSION_LABEL="deploy-$(date +%Y%m%d%H%M%S)"
S3_KEY="${APPLICATION_NAME}/${VERSION_LABEL}.jar"

echo "==> Running mvn clean package (includes tests)"
mvn clean package

if [ ! -f "$JAR_PATH" ]; then
  echo "ERROR: expected jar not found at $JAR_PATH" >&2
  exit 1
fi

echo "==> Uploading $JAR_PATH to s3://${S3_BUCKET}/${S3_KEY}"
aws s3 cp "$JAR_PATH" "s3://${S3_BUCKET}/${S3_KEY}" --region "$REGION"

echo "==> Creating application version $VERSION_LABEL"
aws elasticbeanstalk create-application-version \
  --application-name "$APPLICATION_NAME" \
  --version-label "$VERSION_LABEL" \
  --source-bundle "S3Bucket=${S3_BUCKET},S3Key=${S3_KEY}" \
  --region "$REGION" \
  --output json

echo "==> Deploying $VERSION_LABEL to $ENVIRONMENT_NAME"
aws elasticbeanstalk update-environment \
  --environment-name "$ENVIRONMENT_NAME" \
  --version-label "$VERSION_LABEL" \
  --region "$REGION" \
  --output json

echo "==> Waiting for environment to finish updating"
aws elasticbeanstalk wait environment-updated \
  --environment-names "$ENVIRONMENT_NAME" \
  --region "$REGION"

aws elasticbeanstalk describe-environments \
  --environment-names "$ENVIRONMENT_NAME" \
  --region "$REGION" \
  --query "Environments[0].{Status:Status,Health:Health,VersionLabel:VersionLabel}" \
  --output table

echo "==> Done. Deployed version: $VERSION_LABEL"
