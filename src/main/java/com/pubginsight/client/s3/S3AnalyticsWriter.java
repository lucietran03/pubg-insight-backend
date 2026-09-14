package com.pubginsight.client.s3;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

// Separate from the raw PUBG JSON:API match cache (fragile to query - mixed "included"
// item types); this feed gives Athena a real population to compute percentiles against.
@Component
public class S3AnalyticsWriter {

    private static final String ANALYTICS_KEY_PREFIX = "analytics/";
    private static final String ANALYTICS_KEY_SUFFIX = ".json";

    private final S3Client s3Client;
    private final String bucketName;

    public S3AnalyticsWriter(S3Client s3Client, AwsS3Properties properties) {
        this.s3Client = s3Client;
        this.bucketName = properties.cacheBucket();
    }

    // Best-effort: a write failure here must never fail the match-stats request that
    // triggered it, since this feed is purely additive analytics, not user-facing data.
    public void writeRecord(String matchId, String playerId, String json) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(analyticsKey(matchId, playerId))
                    .contentType("application/json")
                    .build();

            s3Client.putObject(request, RequestBody.fromString(json));
        } catch (SdkException e) {
            throw new S3CacheException("Failed to write analytics record for match '" + matchId + "'", e);
        }
    }

    private String analyticsKey(String matchId, String playerId) {
        return ANALYTICS_KEY_PREFIX + matchId + "-" + playerId + ANALYTICS_KEY_SUFFIX;
    }
}
