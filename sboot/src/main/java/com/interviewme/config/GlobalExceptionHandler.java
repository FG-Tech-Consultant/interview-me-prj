package com.interviewme.config;

import com.interviewme.aichat.exception.LlmUnavailableException;
import com.interviewme.billing.exception.DuplicateRefundException;
import com.interviewme.billing.exception.InsufficientBalanceException;
import com.interviewme.billing.exception.WalletNotFoundException;
import com.interviewme.aichat.exception.ChatQuotaExceededException;
import com.interviewme.aichat.exception.ChatRateLimitException;
import com.interviewme.common.exception.*;
import com.interviewme.common.exception.PublicProfileNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        response.put("timestamp", OffsetDateTime.now().toString());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Bad Request");
        response.put("message", "Validation failed");
        response.put("fieldErrors", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", OffsetDateTime.now().toString());
        response.put("status", HttpStatus.CONFLICT.value());
        response.put("error", "Conflict");
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(Exception ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", OffsetDateTime.now().toString());
        response.put("status", HttpStatus.UNAUTHORIZED.value());
        response.put("error", "Unauthorized");
        response.put("message", "Invalid email or password");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(ProfileNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleProfileNotFoundException(ProfileNotFoundException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", OffsetDateTime.now().toString());
        response.put("status", HttpStatus.NOT_FOUND.value());
        response.put("error", "Not Found");
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(SkillNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleSkillNotFoundException(SkillNotFoundException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", OffsetDateTime.now().toString());
        response.put("status", HttpStatus.NOT_FOUND.value());
        response.put("error", "Not Found");
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(DuplicateProfileException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateProfileException(DuplicateProfileException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", OffsetDateTime.now().toString());
        response.put("status", HttpStatus.CONFLICT.value());
        response.put("error", "Conflict");
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(DuplicateSkillException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateSkillException(DuplicateSkillException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", OffsetDateTime.now().toString());
        response.put("status", HttpStatus.CONFLICT.value());
        response.put("error", "Conflict");
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLockException(OptimisticLockException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", OffsetDateTime.now().toString());
        response.put("status", HttpStatus.CONFLICT.value());
        response.put("error", "Conflict");
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(ValidationException ex) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();
        fieldErrors.put(ex.getField(), ex.getMessage());

        response.put("timestamp", OffsetDateTime.now().toString());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Bad Request");
        response.put("message", "Validation failed");
        response.put("fieldErrors", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleProjectNotFoundException(ProjectNotFoundException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", OffsetDateTime.now().toString());
        response.put("status", HttpStatus.NOT_FOUND.value());
        response.put("error", "Not Found");
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(PackageNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePackageNotFoundException(PackageNotFoundException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", OffsetDateTime.now().toString());
        response.put("status", HttpStatus.NOT_FOUND.value());
        response.put("error", "Not Found");
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(StoryNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleStoryNotFoundException(StoryNotFoundException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", OffsetDateTime.now().toString());
        response.put("status", HttpStatus.NOT_FOUND.value());
        response.put("error", "Not Found");
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientBalanceException(InsufficientBalanceException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", OffsetDateTime.now().toString());
        response.put("status", 402);
        response.put("error", "Payment Required");
        response.put("message", ex.getMessage());
        response.put("required", ex.getRequired());
        response.put("available", ex.getAvailable());

        return ResponseEntity.status(402).body(response);
    }

    @ExceptionHandler(DuplicateRefundException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateRefundException(DuplicateRefundException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", OffsetDateTime.now().toString());
        response.put("status", HttpStatus.CONFLICT.value());
        response.put("error", "Conflict");
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(PublicProfileNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePublicProfileNotFoundException(PublicProfileNotFoundException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", OffsetDateTime.now().toString());
        response.put("status", HttpStatus.NOT_FOUND.value());
        response.put("error", "Not Found");
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleWalletNotFoundException(WalletNotFoundException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", OffsetDateTime.now().toString());
        response.put("status", HttpStatus.NOT_FOUND.value());
        response.put("error", "Not Found");
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(AnalysisNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAnalysisNotFoundException(AnalysisNotFoundException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", OffsetDateTime.now().toString());
        response.put("status", HttpStatus.NOT_FOUND.value());
        response.put("error", "Not Found");
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(InvalidPdfException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidPdfException(InvalidPdfException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", OffsetDateTime.now().toString());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Bad Request");
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(SectionNotApplicableException.class)
    public ResponseEntity<Map<String, Object>> handleSectionNotApplicableException(SectionNotApplicableException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", OffsetDateTime.now().toString());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Bad Request");
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(AnalysisNotCompletedException.class)
    public ResponseEntity<Map<String, Object>> handleAnalysisNotCompletedException(AnalysisNotCompletedException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", OffsetDateTime.now().toString());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Bad Request");
        response.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", OffsetDateTime.now().toString());
        response.put("status", 413);
        response.put("error", "Payload Too Large");
        response.put("message", "File exceeds maximum size of 10MB");

        return ResponseEntity.status(413).body(response);
    }

    @ExceptionHandler(ChatQuotaExceededException.class)
    public ResponseEntity<Map<String, Object>> handleChatQuotaExceededException(ChatQuotaExceededException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", OffsetDateTime.now().toString());
        response.put("status", 402);
        response.put("error", "Payment Required");
        response.put("code", "CHAT_QUOTA_EXCEEDED");
        response.put("message", ex.getMessage());

        return ResponseEntity.status(402).body(response);
    }

    @ExceptionHandler(ChatRateLimitException.class)
    public ResponseEntity<Map<String, Object>> handleChatRateLimitException(ChatRateLimitException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", OffsetDateTime.now().toString());
        response.put("status", 429);
        response.put("error", "Too Many Requests");
        response.put("code", "RATE_LIMITED");
        response.put("message", ex.getMessage());

        return ResponseEntity.status(429)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(response);
    }

    @ExceptionHandler(LlmUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleLlmUnavailableException(LlmUnavailableException ex) {
        log.error("LLM service unavailable: {}", ex.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", OffsetDateTime.now().toString());
        response.put("status", 503);
        response.put("error", "Service Unavailable");
        response.put("code", "LLM_UNAVAILABLE");
        response.put("message", "AI service temporarily unavailable");

        return ResponseEntity.status(503).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", OffsetDateTime.now().toString());
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("error", "Internal Server Error");
        response.put("message", "An unexpected error occurred");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
