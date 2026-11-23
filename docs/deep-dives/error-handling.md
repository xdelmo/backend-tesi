# Gestione errori (RFC 7807)

La dashboard passa ogni risposta HTTP da un `errorInterceptor`. Per leggerle senza casi speciali le serve un payload sempre uguale, qualunque sia l'eccezione.

## La scelta: un solo advice e un record

`GlobalExceptionHandler` (`@RestControllerAdvice`) intercetta tre famiglie di eccezioni e restituisce sempre il record `ApiError` (`status`, `error`, `message`, `timestamp`).

| Eccezione | Status | `error` | `message` |
|---|---|---|---|
| `MethodArgumentNotValidException` | `400` | `Validation Error` | `campo: messaggio, ...` |
| `IllegalArgumentException` | `400` | `Bad Request` | messaggio del service |
| `Exception` (fallback) | `500` | `Internal Server Error` | `Si è verificato un errore inatteso` |

```java
// src/main/java/com/apexflow/exception/GlobalExceptionHandler.java — fallback
@ExceptionHandler(Exception.class)
public ResponseEntity<ApiError> handleGeneric(Exception ex) {
    log.error("unexpected_error", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiError.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", "Si è verificato un errore inatteso"));
}
```

Lo stack trace finisce nel log, mai nella risposta: il client non vede classi, query o dettagli interni.

## Alternative scartate

- **Solo `ProblemDetail` di Spring** (`spring.mvc.problemdetails.enabled=true`, attivo per le eccezioni che l'advice non copre): il formato `type`/`title`/`detail` è diverso da `ApiError` e avrebbe dato due forme di errore da gestire.
- **`try/catch` nei controller**: duplicazione in ogni metodo e formati che divergono.
