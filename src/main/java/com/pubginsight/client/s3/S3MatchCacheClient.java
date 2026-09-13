package com.pubginsight.client.s3;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Optional;

// Caches completed PUBG match responses, keyed by matchId - match data is immutable once a
// match ends and isn't player-specific, so one cached object serves every player who looks it up.
@Component
public class S3MatchCacheClient {

    private static final String CACHE_KEY_PREFIX = "matches/";
    private static final String CACHE_KEY_SUFFIX = ".json";

    private final S3Client s3Client;
    private final String bucketName;

    public S3MatchCacheClient(S3Client s3Client, AwsS3Properties properties) {
        this.s3Client = s3Client;
        this.bucketName = properties.cacheBucket();
    }

    // A missing object is a normal cache miss, not an error, so it's reported as an empty
    // Optional rather than an exception.
    public Optional<String> getCachedMatchJson(String matchId) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(cacheKey(matchId))
                    .build();

            String json = s3Client.getObjectAsBytes(request).asUtf8String();
            return Optional.of(json);
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (SdkException e) {
            throw new S3CacheException("Failed to read cached match '" + matchId + "' from S3", e);
        }
    }

    public void cacheMatchJson(String matchId, String rawJson) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(cacheKey(matchId))
                    .contentType("application/json")
                    .build();

            s3Client.putObject(request, RequestBody.fromString(rawJson));
        } catch (SdkException e) {
            throw new S3CacheException("Failed to cache match '" + matchId + "' in S3", e);
        }
    }

    private String cacheKey(String matchId) {
        return CACHE_KEY_PREFIX + matchId + CACHE_KEY_SUFFIX;
    }
}
