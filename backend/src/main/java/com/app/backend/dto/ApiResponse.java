package com.app.backend.dto;



import com.app.backend.support.TraceIdHolder;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;

import lombok.NoArgsConstructor;

import lombok.Setter;



import java.time.Instant;



/**

 * Standard API envelope: {@code success}, {@code message}, {@code data}, optional {@code traceId}, {@code timestamp}, {@code errorCode}.

 */

@Getter

@Setter

@NoArgsConstructor

@JsonInclude(JsonInclude.Include.NON_NULL)

public class ApiResponse<T> {



    private boolean success;

    private String message;

    private T data;

    /** ISO-8601 instant when the response was built (UTC). */

    private Instant timestamp;

    /** Request correlation id (matches logs when {@link com.app.backend.config.TraceIdFilter} is active). */

    private String traceId;

    /** Stable machine-readable code on errors (e.g. VALIDATION_ERROR). */

    private String errorCode;



    public static <T> ApiResponse<T> ok(T data) {

        ApiResponse<T> r = new ApiResponse<>();

        r.setSuccess(true);

        r.setMessage("OK");

        r.setData(data);

        r.setTimestamp(Instant.now());

        r.setTraceId(TraceIdHolder.currentOrNull());

        return r;

    }



    public static <T> ApiResponse<T> ok(String message, T data) {

        ApiResponse<T> r = new ApiResponse<>();

        r.setSuccess(true);

        r.setMessage(message);

        r.setData(data);

        r.setTimestamp(Instant.now());

        r.setTraceId(TraceIdHolder.currentOrNull());

        return r;

    }



    public static <T> ApiResponse<T> fail(String message) {

        return fail(message, null, null);

    }



    public static <T> ApiResponse<T> fail(String message, String errorCode) {

        return fail(message, null, errorCode);

    }



    public static <T> ApiResponse<T> fail(String message, T data) {

        return fail(message, data, null);

    }



    public static <T> ApiResponse<T> fail(String message, T data, String errorCode) {

        ApiResponse<T> r = new ApiResponse<>();

        r.setSuccess(false);

        r.setMessage(message);

        r.setData(data);

        r.setTimestamp(Instant.now());

        r.setTraceId(TraceIdHolder.currentOrNull());

        r.setErrorCode(errorCode);

        return r;

    }

}

