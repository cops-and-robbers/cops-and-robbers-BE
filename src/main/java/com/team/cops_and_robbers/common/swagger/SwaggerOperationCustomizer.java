package com.team.cops_and_robbers.common.swagger;

import com.team.cops_and_robbers.auth.exception.AuthException;
import com.team.cops_and_robbers.auth.presentation.annotation.AuthUser;
import com.team.cops_and_robbers.common.exception.CommonException;
import com.team.cops_and_robbers.common.exception.ExceptionCode;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
public class SwaggerOperationCustomizer implements OperationCustomizer {

    private static final String MEDIA_TYPE = "application/json";
    private static final String ERROR_RESPONSE_REF = "#/components/schemas/ErrorResponse";

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        ApiResponses responses = operation.getResponses();
        String pathPattern = resolvePathPattern(handlerMethod);

        addErrorCodeExamples(responses, handlerMethod, pathPattern);
        addAuth401IfRequired(responses, handlerMethod, pathPattern);
        add500(responses, pathPattern);

        return operation;
    }

    private void addErrorCodeExamples(ApiResponses responses, HandlerMethod handlerMethod, String pathPattern) {
        Set<ApiErrorCode> annotations = AnnotatedElementUtils.findMergedRepeatableAnnotations(
                handlerMethod.getMethod(), ApiErrorCode.class, ApiErrorCodes.class);

        for (ApiErrorCode annotation : annotations) {
            Class<? extends ExceptionCode> enumClass = annotation.value();
            String[] codes = annotation.codes();

            if (codes.length == 0) {
                for (ExceptionCode code : enumClass.getEnumConstants()) {
                    injectExample(responses, code, pathPattern);
                }
            } else {
                for (String codeName : codes) {
                    ExceptionCode code = findEnumConstant(enumClass, codeName);
                    injectExample(responses, code, pathPattern);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private ExceptionCode findEnumConstant(Class<? extends ExceptionCode> enumClass, String codeName) {
        try {
            return (ExceptionCode) Enum.valueOf((Class<? extends Enum>) enumClass, codeName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    String.format("Enum constant '%s' not found in class '%s'. Please check @ApiErrorCode annotation.",
                            codeName, enumClass.getName()), e);
        }
    }

    private void injectExample(ApiResponses responses, ExceptionCode code, String pathPattern) {
        int status = code.getHttpStatus().value();
        ApiResponse apiResponse = responses.computeIfAbsent(
                String.valueOf(status), k -> new ApiResponse().description(status + " Error"));

        if (apiResponse.getContent() == null) apiResponse.setContent(new Content());
        MediaType mediaType = apiResponse.getContent().computeIfAbsent(MEDIA_TYPE, k ->
                new MediaType().schema(new Schema<>().$ref(ERROR_RESPONSE_REF)));

        if (mediaType.getExamples() == null) mediaType.setExamples(new LinkedHashMap<>());
        mediaType.getExamples().put(code.getTitle(), toExample(code, pathPattern));
    }

    private Example toExample(ExceptionCode code, String pathPattern) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("title", code.getTitle());
        value.put("status", code.getHttpStatus().value());
        value.put("detail", code.getDetail());
        value.put("instance", pathPattern);
        return new Example().summary(code.getTitle()).value(value);
    }

    private void addAuth401IfRequired(ApiResponses responses, HandlerMethod handlerMethod, String pathPattern) {
        boolean requiresAuth = Arrays.stream(handlerMethod.getMethodParameters())
                .anyMatch(p -> p.hasParameterAnnotation(AuthUser.class));
        if (requiresAuth && !responses.containsKey("401")) {
            injectExample(responses, AuthException.UNAUTHENTICATED_REQUEST, pathPattern);
        }
    }

    private void add500(ApiResponses responses, String pathPattern) {
        if (!responses.containsKey("500")) {
            injectExample(responses, CommonException.INTERNAL_SERVER_ERROR, pathPattern);
        }
    }

    private String resolvePathPattern(HandlerMethod handlerMethod) {
        RequestMapping classMapping = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getBeanType(), RequestMapping.class);
        RequestMapping methodMapping = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(), RequestMapping.class);

        String classPath = (classMapping != null && classMapping.value().length > 0) ? classMapping.value()[0] : "";
        String methodPath = (methodMapping != null && methodMapping.value().length > 0) ? methodMapping.value()[0] : "";

        return classPath + methodPath;
    }
}
