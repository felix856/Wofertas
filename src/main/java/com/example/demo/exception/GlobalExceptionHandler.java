package com.example.demo.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Validações de @Valid em DTOs
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        Map<String, String> errorsMap = new HashMap<>();
        for (FieldError fe : fieldErrors) {
            errorsMap.put(fe.getField(), fe.getDefaultMessage());
        }

        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "Dados de entrada inválidos",
                request.getRequestURI()
        );
        body.setFieldErrors(errorsMap);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // JSON mal formado / payload inválido
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Malformed JSON request",
                ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : "Payload inválido",
                request.getRequestURI()
        );
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // Trata RuntimeException (útil para os orElseThrow(() -> new RuntimeException("... não encontrado")))
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex, HttpServletRequest request) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Erro no processamento";
        HttpStatus status = decideStatusFromMessage(msg);

        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                msg,
                request.getRequestURI()
        );
        return new ResponseEntity<>(body, status);
    }

    // Fallback para qualquer outra exceção
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Ocorreu um erro inesperado",
                request.getRequestURI()
        );
        // opcional: logar stacktrace
        ex.printStackTrace();
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // heurística simples para mapear mensagens para status HTTP (pode ajustar depois)
    private HttpStatus decideStatusFromMessage(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("não encontrado") || lower.contains("nao encontrado") || lower.contains("not found")) {
            return HttpStatus.NOT_FOUND;
        }
        if (lower.contains("autentic") || lower.contains("credenciais")) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (lower.contains("permiss") || lower.contains("permitid") || lower.contains("forbidden") || lower.contains("acesso negado")) {
            return HttpStatus.FORBIDDEN;
        }
        if (lower.contains("e-mail indisponivel") || lower.contains("email indisponivel")
                || lower.contains("smtp") || lower.contains("mail server")) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        if (lower.contains("ja cadastrado") || lower.contains("já cadastrado")
                || lower.contains("duplicad") || lower.contains("duplicate")) {
            return HttpStatus.CONFLICT;
        }
        // outras heurísticas podem ser adicionadas
        return HttpStatus.BAD_REQUEST;
    }
}
