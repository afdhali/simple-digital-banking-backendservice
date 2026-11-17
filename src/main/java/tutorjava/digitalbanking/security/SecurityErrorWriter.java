package tutorjava.digitalbanking.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import tutorjava.digitalbanking.common.exception.ErrorCode;
import tutorjava.digitalbanking.common.response.ApiError;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Writes the standard ApiError JSON from servlet filters
 * (where @RestControllerAdvice does not apply).
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorWriter {

    private final JsonMapper jsonMapper;

    public void write(HttpServletRequest request, HttpServletResponse response, ErrorCode code)
            throws IOException {
        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getWriter(),
                ApiError.of(code.getStatus(), code.name(), code.getDefaultMessage(), request.getRequestURI()));
    }
}
