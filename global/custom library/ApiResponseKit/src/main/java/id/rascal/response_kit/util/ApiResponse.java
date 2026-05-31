package id.rascal.response_kit.util;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import id.rascal.response_kit.template.*;

public class ApiResponse {

    private static final String DEFAULT_SUCCESS_MESSAGE = "Request processed successfully";
    private static final String DEFAULT_PAGED_SUCCESS_MESSAGE = "Data retrieved successfully";
    private static final String DEFAULT_VALIDATION_MESSAGE = "Validation failed";

    public static ResponseEntity<?> error (
        HttpStatus httpStatus,
        int status,
        String errorType,
        String message
    ) {
        return ResponseEntity.status(httpStatus).body(
            errorBody(status, errorType, message)
        );
    }

    public static ErrorTemplate errorBody (
        int status,
        String errorType,
        String message
    ) {
        return new ErrorTemplate(
            false,
            message,
            errorCode(status, errorType),
            null,
            MetaTemplate.now()
        );
    }

    public static ResponseEntity<?> validationError (
        List<FieldErrorTemplate> errors
    ) {
        return validationError(DEFAULT_VALIDATION_MESSAGE, errors);
    }

    public static ResponseEntity<?> validationError (
        String message,
        List<FieldErrorTemplate> errors
    ) {
        return ResponseEntity.badRequest().body(
            new ErrorTemplate(
                false,
                message,
                null,
                errors,
                MetaTemplate.now()
            )
        );
    }

    public static ResponseEntity<?> success (
        HttpStatus httpStatus,
        Object data
    ) {
        return success(httpStatus, DEFAULT_SUCCESS_MESSAGE, data);
    }

    public static ResponseEntity<?> success (
        HttpStatus httpStatus,
        String message,
        Object data
    ) {
        return ResponseEntity.status(httpStatus).body(
            new SuccessTemplate(
                true,
                message,
                data,
                MetaTemplate.now()
            )
        );
    }

    public static ResponseEntity<?> paged (
        HttpStatus httpStatus,
        Page<?> page
    ) {
        return paged(httpStatus, DEFAULT_PAGED_SUCCESS_MESSAGE, page);
    }

    public static ResponseEntity<?> paged (
        HttpStatus httpStatus,
        String message,
        Page<?> page
    ) {
        MetaTemplate meta = MetaTemplate.paged(new PaginationTemplate(
            page.getNumber() + 1,
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.hasNext(),
            page.hasPrevious()
        ));

        return ResponseEntity.status(httpStatus).body(
            new SuccessPagedTemplate(
                true,
                message,
                page.getContent(),
                meta
            )
        );
    }

    private static String errorCode(int status, String errorType) {
        if (errorType == null || errorType.isBlank()) {
            return String.valueOf(status);
        }

        return errorType
            .trim()
            .toUpperCase(Locale.ROOT);
    }
}
