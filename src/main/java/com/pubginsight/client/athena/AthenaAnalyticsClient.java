package com.pubginsight.client.athena;

import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.GetQueryExecutionRequest;
import software.amazon.awssdk.services.athena.model.GetQueryExecutionResponse;
import software.amazon.awssdk.services.athena.model.GetQueryResultsRequest;
import software.amazon.awssdk.services.athena.model.QueryExecutionContext;
import software.amazon.awssdk.services.athena.model.QueryExecutionState;
import software.amazon.awssdk.services.athena.model.ResultConfiguration;
import software.amazon.awssdk.services.athena.model.Row;
import software.amazon.awssdk.services.athena.model.StartQueryExecutionRequest;

import java.util.List;

// Athena queries are asynchronous: start, then poll until the engine reports a terminal
// state, then fetch results - there is no synchronous "just run this and get rows back" call.
@Component
public class AthenaAnalyticsClient {

    private static final int MAX_POLL_ATTEMPTS = 20;
    private static final long POLL_INTERVAL_MILLIS = 500;

    private final AthenaClient athenaClient;
    private final AwsAthenaProperties properties;

    public AthenaAnalyticsClient(AthenaClient athenaClient, AwsAthenaProperties properties) {
        this.athenaClient = athenaClient;
        this.properties = properties;
    }

    // Population baseline for a single game mode: median damage and sample size across
    // every match this app has ever analyzed for anyone, not just a fixed ceiling.
    public PopulationComparison compareDamage(String gameMode, double matchDamage) {
        String sql = "SELECT approx_percentile(damageDealt, 0.5) AS median_damage, count(*) AS sample_size "
                + "FROM " + properties.table() + " WHERE gameMode = '" + escapeSql(gameMode) + "'";

        String queryExecutionId = startQuery(sql);
        QueryExecutionState finalState = awaitCompletion(queryExecutionId);

        if (finalState != QueryExecutionState.SUCCEEDED) {
            throw new AthenaQueryException("Athena query did not succeed: " + finalState);
        }

        List<Row> rows = athenaClient.getQueryResults(GetQueryResultsRequest.builder()
                        .queryExecutionId(queryExecutionId)
                        .build())
                .resultSet()
                .rows();

        // Row 0 is the header (column names); row 1 holds the aggregate values.
        if (rows.size() < 2) {
            return new PopulationComparison(0.0, 0, 0.0);
        }
        List<String> values = rows.get(1).data().stream().map(datum -> datum.varCharValue()).toList();
        double medianDamage = values.get(0) == null ? 0.0 : Double.parseDouble(values.get(0));
        long sampleSize = values.get(1) == null ? 0 : Long.parseLong(values.get(1));
        double deltaPct = medianDamage == 0.0 ? 0.0 : ((matchDamage - medianDamage) / medianDamage) * 100.0;

        return new PopulationComparison(medianDamage, sampleSize, deltaPct);
    }

    private String startQuery(String sql) {
        StartQueryExecutionRequest request = StartQueryExecutionRequest.builder()
                .queryString(sql)
                .queryExecutionContext(QueryExecutionContext.builder().database(properties.database()).build())
                .resultConfiguration(ResultConfiguration.builder().outputLocation(properties.outputLocation()).build())
                .build();
        return athenaClient.startQueryExecution(request).queryExecutionId();
    }

    private QueryExecutionState awaitCompletion(String queryExecutionId) {
        for (int attempt = 0; attempt < MAX_POLL_ATTEMPTS; attempt++) {
            GetQueryExecutionResponse response = athenaClient.getQueryExecution(
                    GetQueryExecutionRequest.builder().queryExecutionId(queryExecutionId).build());
            QueryExecutionState state = response.queryExecution().status().state();

            if (state == QueryExecutionState.SUCCEEDED
                    || state == QueryExecutionState.FAILED
                    || state == QueryExecutionState.CANCELLED) {
                return state;
            }

            try {
                Thread.sleep(POLL_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AthenaQueryException("Interrupted while waiting for Athena query", e);
            }
        }
        throw new AthenaQueryException("Athena query timed out after " + MAX_POLL_ATTEMPTS + " polls");
    }

    // Single-quotes are the only realistic injection vector here (gameMode comes from an
    // enum-like PUBG value, not free text) - doubled per standard SQL escaping.
    private static String escapeSql(String value) {
        return value.replace("'", "''");
    }
}
