package com.devflow.copilot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"devflow.ai.provider=local-rule", "devflow.ai.api-key=", "devflow.ai.protocol=chat-completions-compatible", "devflow.ai.fallback-to-local=true"})
@ActiveProfiles("test")
class ProviderExecutionMetadataMigrationTest {

    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void latestFlywayMigrationAddsExecutionMetadataAndConservativelyBackfillsLegacyRows() {
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'generation_record'",
                String.class
        );
        assertThat(columns).contains(
                "requested_provider", "requested_model", "fallback_used", "fallback_reason",
                "provider_error_type", "provider_failure_stage", "provider_http_status",
                "provider_http_status_family", "provider_duration_bucket", "provider_response_body_present",
                "provider_response_size_bucket", "provider_content_type_category", "provider_retry_after_present",
                "provider_request_id_present", "provider_client_request_id"
        );

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT requested_provider, requested_model, fallback_used, fallback_reason FROM generation_record LIMIT 1"
        );
        assertThat(row.get("requested_provider")).isNotNull();
        assertThat(row.get("requested_model")).isNotNull();
        assertThat(row.get("fallback_used")).isEqualTo(false);
        assertThat(row.get("fallback_reason")).isNull();

        Map<String, Object> observability = jdbcTemplate.queryForMap(
                "SELECT provider_error_type, provider_http_status, provider_client_request_id FROM generation_record LIMIT 1"
        );
        assertThat(observability.get("provider_error_type")).isNull();
        assertThat(observability.get("provider_http_status")).isNull();
        assertThat(observability.get("provider_client_request_id")).isNull();
    }
}
