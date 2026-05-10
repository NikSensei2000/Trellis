package com.github.niksensei2000.trellis.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.niksensei2000.trellis.execution.DomainError;
import com.github.niksensei2000.trellis.execution.ErrorCategory;
import com.github.niksensei2000.trellis.execution.Result;

/**
 * Shared, thread-safe Jackson configuration for Trellis components and adapters.
 * <p>
 * Centralizing {@link ObjectMapper} setup prevents subtle drift between modules (one module
 * writing ISO-8601 strings while another emits epoch millis). A single mapper also makes security
 * reviews easier: unknown properties are ignored for forward compatibility, and “empty” values
 * are omitted on serialization to reduce payload noise.
 * </p>
 * <p>
 * Serialization uses {@link JsonInclude.Include#NON_EMPTY}, which omits values considered empty
 * (including {@code null} for many property types) in addition to empty collections and strings.
 * That choice aligns with typical API ergonomics while staying close to the Trellis guideline of
 * combining non-null and non-empty semantics at the boundary.
 * </p>
 * <p>
 * Callers that need different behavior should construct a dedicated mapper rather than mutating
 * the shared instance returned by {@link #mapper()}.
 * </p>
 */
public final class MapperUtility {

    /**
     * Stable error code when JSON serialization fails unexpectedly.
     */
    public static final String SERIALIZATION_ERROR = "trellis.mapper.serialization_failed";

    /**
     * Stable error code when JSON deserialization fails unexpectedly.
     */
    public static final String DESERIALIZATION_ERROR = "trellis.mapper.deserialization_failed";

    private static final ObjectMapper SHARED = createMapper();

    private MapperUtility() {
    }

    /**
     * Returns the shared, immutable-configuration mapper instance.
     * <p>
     * {@link ObjectMapper} is thread-safe for read operations after configuration; do not
     * reconfigure this instance at runtime.
     * </p>
     *
     * @return configured mapper
     */
    public static ObjectMapper mapper() {
        return SHARED;
    }

    /**
     * Serializes {@code obj} to JSON.
     * <p>
     * Failures are captured as {@link Result#failure} so calling code can remain
     * exception-free for expected mapping problems.
     * </p>
     *
     * @param obj object to serialize; may be {@code null} depending on Jackson modules in use
     * @return JSON text on success, or a {@link DomainError} on failure
     */
    public static Result<String, DomainError> serialize(Object obj) {
        try {
            return Result.success(SHARED.writeValueAsString(obj));
        } catch (JsonProcessingException ex) {
            return Result.failure(new DomainError(
                    SERIALIZATION_ERROR,
                    ex.getOriginalMessage() != null ? ex.getOriginalMessage() : ex.getMessage(),
                    ErrorCategory.SYSTEM
            ));
        }
    }

    /**
     * Deserializes JSON into {@code clazz}.
     *
     * @param json  JSON text
     * @param clazz target type
     * @param <T>   target type
     * @return instance on success, or a {@link DomainError} on failure
     */
    public static <T> Result<T, DomainError> deserialize(String json, Class<T> clazz) {
        try {
            return Result.success(SHARED.readValue(json, clazz));
        } catch (JsonProcessingException ex) {
            return Result.failure(new DomainError(
                    DESERIALIZATION_ERROR,
                    ex.getOriginalMessage() != null ? ex.getOriginalMessage() : ex.getMessage(),
                    ErrorCategory.SYSTEM
            ));
        }
    }

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setDefaultPropertyInclusion(JsonInclude.Value.construct(
                JsonInclude.Include.NON_EMPTY,
                JsonInclude.Include.NON_EMPTY
        ));
        return mapper;
    }
}
