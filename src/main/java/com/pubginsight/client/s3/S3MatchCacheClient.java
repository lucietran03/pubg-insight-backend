package com.pubginsight.client.s3;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Optional;

// Thin wrapper around S3Client for caching completed PUBG match responses, keyed by
// matchId (match data is immutable once a match ends and is not player-specific, so one
// cached object correctly serves every player who ever looks up that match - see
// docs/ARCHITECTURE.md section 8 for the rate-limit problem this solves).
//
// This class only knows S3's semantics (object exists / doesn't / call failed) - like
// PubgApiClient (see D2 in docs/ARCHITECTURE.md), it never throws a feature-specific
// exception. It is MatchService's job to decide what a cache failure means for the
// match-analytics feature (answer: nothing - see the soft-fail comment there).
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

    // A missing object is a normal, expected outcome (cache miss) - not an error - so it
    // is reported as an empty Optional rather than an exception.
    //
    // Uses getObjectAsBytes() rather than getObject()+manual InputStream reading: it's the
    // SDK's own convenience method for exactly this "read the whole object into memory"
    // case (fine here - cached match JSON is small) and returns ResponseBytes<GetObjectResponse>,
    // whose asUtf8String() does the byte[]->String decoding for us. Same exception surface
    // (including NoSuchKeyException) as the streaming getObject() call either way.
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
