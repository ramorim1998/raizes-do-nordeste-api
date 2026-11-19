package br.com.raizesdonordeste.raizes_api.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail tratarRecursoNaoEncontrado(RecursoNaoEncontradoException exception) {
        return problema(HttpStatus.NOT_FOUND, "Recurso não encontrado", exception.getMessage());
    }

    @ExceptionHandler(ConflitoException.class)
    public ProblemDetail tratarConflito(ConflitoException exception) {
        return problema(HttpStatus.CONFLICT, "Conflito", exception.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail tratarViolacaoDeRestricao(ConstraintViolationException exception) {
        List<ErroCampo> erros = exception.getConstraintViolations().stream()
            .map(violacao -> new ErroCampo(violacao.getPropertyPath().toString(), violacao.getMessage()))
            .toList();
        return erroValidacao(erros);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                 HttpHeaders headers, HttpStatusCode status,
                                                                 WebRequest request) {
        List<ErroCampo> erros = exception.getBindingResult().getFieldErrors().stream()
            .map(erro -> new ErroCampo(erro.getField(), erro.getDefaultMessage()))
            .toList();
        return handleExceptionInternal(exception, erroValidacao(erros), headers, status, request);
    }

    private static ProblemDetail erroValidacao(List<ErroCampo> erros) {
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "Dados inválidos",
            "Corrija os campos indicados e tente novamente.");
        problema.setProperty("erros", erros);
        return problema;
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        return problema;
    }

    private record ErroCampo(String campo, String mensagem) {
    }
}
