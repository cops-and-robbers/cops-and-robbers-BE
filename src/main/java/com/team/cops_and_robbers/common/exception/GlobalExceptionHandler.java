package com.team.cops_and_robbers.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 어플리케이션 로직에서 발생시킨 예외를 처리합니다.
     */
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorResponse> handleApplicationException(ApplicationException ex,
                                                                    HttpServletRequest request) {
        ExceptionCode code = ex.getCode();
        String instance = request.getRequestURI();

        log.info("비즈니스 로직 예외 | code={}, title=\"{}\", detail=\"{}\", instance={}",
                code.getHttpStatus().value(), code.getTitle(), code.getDetail(), instance);

        return createErrorResponse(code, instance);
    }

    /**
     * S3, 외부 API 등 외부 인프라와 연동에 실패 시 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(InfrastructureException.class)
    public ResponseEntity<ErrorResponse> handleInfrastructureException(InfrastructureException ex, HttpServletRequest request) {
        ExceptionCode code = ex.getCode();
        log.error("외부 시스템 연동 오류 | code={}, title=\"{}\", detail=\"{}\", instance={} ",
                code.getHttpStatus().value(), code.getTitle(), code.getDetail(), request.getRequestURI(), ex);
        return createErrorResponse(ex.getCode(), request.getRequestURI());
    }

    /**
     * @Valid 유효성 검사 실패 시 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex,
                                                                   HttpServletRequest request) {
        // 어떤 필드에서 오류가 났는지에 대한 detail 정보 작성
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ExceptionCode code = CommonException.INVALID_INPUT_VALUE;
        String instance = request.getRequestURI();
        loggingClientError(code, detail, instance);
        return createErrorResponse(code, detail, request.getRequestURI());
    }

    /**
     * @RequestBody JSON 파싱 실패 등 Request Body를 읽을 수 없을 때 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                      HttpServletRequest request) {
        ExceptionCode code = CommonException.INVALID_REQUEST_BODY;
        loggingClientError(code, code.getDetail(), request.getRequestURI());
        return createErrorResponse(code, request.getRequestURI());
    }

    /**
     * 서버가 지원하지 않는 Content-Type으로 요청 시 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                                                         HttpServletRequest request) {
        String detail = String.format("지원하는 Media Type은 '%s' 입니다.", ex.getSupportedMediaTypes());
        ExceptionCode code = CommonException.UNSUPPORTED_MEDIA_TYPE;
        String instance = request.getRequestURI();
        loggingClientError(code, detail, instance);
        return createErrorResponse(code, detail, instance);
    }

    /**
     * @RequestPart 필수 요청 파트 누락 시 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestPart(MissingServletRequestPartException ex, HttpServletRequest request) {
        String partName = ex.getRequestPartName();
        String detail = String.format("필수 파트 '%s'가 요청에 없습니다.", partName);
        ExceptionCode code = CommonException.MISSING_REQUEST_PART;
        String instance = request.getRequestURI();
        loggingClientError(code, detail, instance);
        return createErrorResponse(code, detail, instance);
    }

    /**
     * @RequestParam 필수 요청 파라미터 누락시 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParams(MissingServletRequestParameterException ex,
                                                             HttpServletRequest request) {
        String name = ex.getParameterName(); // 누락된 파라미터 이름
        String expectType = ex.getParameterType();
        String detail = String.format("필수 쿼리 파라미터 '%s'(%s)가 누락되었습니다.", name, expectType);
        ExceptionCode code = CommonException.INVALID_QUERY_PARAMETER;
        String instance = request.getRequestURI();
        loggingClientError(code, detail, instance);
        return createErrorResponse(code, detail, instance);
    }

    /**
     * @RequestParam 쿼리 파라미터의 타입이 일치하지 않을 때 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        String paramName = ex.getName();
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "알 수 없는 타입";
        String detail = String.format("쿼리 파라미터 '%s'는 '%s' 타입이어야 합니다.", paramName, requiredType);
        ExceptionCode code = CommonException.QUERY_PARAMETER_TYPE_MISMATCH;
        String instance = request.getRequestURI();
        loggingClientError(code, detail, instance);
        return createErrorResponse(code, detail, instance);
    }

    /**
     * 지원하지 않는 HTTP 메서드 호출 시 예외를 처리합니다.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e,
                                                                             HttpServletRequest request) {
        String detail = String.format("해당 엔드포인트는 '%s' 메소드를 지원하지 않습니다.", e.getMethod());
        ExceptionCode code = CommonException.METHOD_NOT_ALLOWED;
        String instance = request.getRequestURI();
        loggingClientError(code, detail, instance);
        return createErrorResponse(code, detail, instance);
    }

    /**
     * 서버가 지원하지 않는 엔드포인트 요청시 예외를 처리합니다.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException e, HttpServletRequest request) {
        String instance = request.getRequestURI();
        ExceptionCode code = CommonException.ENDPOINT_NOT_FOUND;
        loggingClientError(code, code.getDetail(), instance);
        return createErrorResponse(code, instance);
    }

    /**
     * 알 수 없는 오류로 인한 예외를 처리합니다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> exceptionHandler(Exception ex, HttpServletRequest request) {
        CommonException code = CommonException.INTERNAL_SERVER_ERROR;
        log.error("예상하지 못한 예외 발생 | ", ex);
        return createErrorResponse(code, request.getRequestURI());
    }


    private ResponseEntity<ErrorResponse> createErrorResponse(ExceptionCode code, String instance) {
        ErrorResponse errorResponse = ErrorResponse.of(code, instance);
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(errorResponse);
    }

    private ResponseEntity<ErrorResponse> createErrorResponse(ExceptionCode code, String detail, String instance) {
        ErrorResponse errorResponse = ErrorResponse.of(code, detail, instance);
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(errorResponse);
    }

    private void loggingClientError(ExceptionCode code, String detail, String instance) {
        log.info("클라이언트 요청 오류 | code={}, title=\"{}\", detail=\"{}\", instance={}",
                code.getHttpStatus().value(), code.getTitle(), detail, instance);
    }

    /**
     * 단일 파라미터 검증 실패 시 예외를 처리 합니다.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(HandlerMethodValidationException ex,
                                                                       HttpServletRequest request) {
        String detail = ex.getAllErrors().stream()
                .map(error -> ((DefaultMessageSourceResolvable) error).getDefaultMessage())
                .collect(Collectors.joining(", "));

        ExceptionCode code = CommonException.INVALID_QUERY_PARAMETER;
        loggingClientError(code, detail, request.getRequestURI());
        return createErrorResponse(code, detail, request.getRequestURI());
    }

    /**
     * Bean Validation 에서 발생 하는 검증 위반 예외를 처리 합니다.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                   HttpServletRequest request) {
        String detail = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .collect(Collectors.joining(", "));
        ExceptionCode code = CommonException.INVALID_QUERY_PARAMETER;
        loggingClientError(code, detail, request.getRequestURI());
        return createErrorResponse(code, detail, request.getRequestURI());
    }
}
