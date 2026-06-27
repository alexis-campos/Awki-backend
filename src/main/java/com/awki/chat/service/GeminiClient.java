package com.awki.chat.service;

import com.awki.chat.config.GeminiProperties;
import com.awki.exception.BusinessRuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

@Service
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final GeminiProperties geminiProperties;
    private final WebClient webClient;

    public GeminiClient(GeminiProperties geminiProperties, WebClient.Builder webClientBuilder) {
        this.geminiProperties = geminiProperties;
        this.webClient = webClientBuilder
                .baseUrl(geminiProperties.getApiUrl())
                .build();
    }

    public Optional<String> generarContenido(String prompt) {
        // Validación de modo real y clave mock/vacía
        boolean isMockKey = geminiProperties.getApiKey() == null 
                || geminiProperties.getApiKey().isBlank() 
                || "mock-key".equalsIgnoreCase(geminiProperties.getApiKey().trim());

        if (!geminiProperties.isMockEnabled() && isMockKey) {
            log.error("Falta configurar la variable de entorno GEMINI_API_KEY para ejecutar en modo real.");
            throw new BusinessRuleException("MISSING_GEMINI_KEY", "Falta configurar la variable de entorno GEMINI_API_KEY para ejecutar en modo real");
        }

        // Modo Mock activado o clave es mock-key
        if (geminiProperties.isMockEnabled() || isMockKey) {
            log.info("[GeminiClient MOCK] Generando respuesta simulada para el prompt (longitud: {})", prompt.length());
            return Optional.of(generarRespuestaMock(prompt));
        }

        log.info("[GeminiClient REAL] Llamando a Gemini API: model={}, timeout={}s", 
                geminiProperties.getModel(), geminiProperties.getTimeoutSeconds());

        try {
            // Construir request body
            Map<String, Object> requestBody = construirRequestBody(prompt);

            // POST /v1beta/models/{model}:generateContent
            String uri = "/v1beta/models/" + geminiProperties.getModel() + ":generateContent";

            // Ejecutar llamada
            GeminiResponse response = webClient.post()
                    .uri(uri)
                    .header("x-goog-api-key", geminiProperties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    .timeout(Duration.ofSeconds(geminiProperties.getTimeoutSeconds()))
                    .block();

            if (response != null && response.candidates() != null && !response.candidates().isEmpty()) {
                String text = response.candidates().get(0).content().parts().get(0).text();
                return Optional.ofNullable(text);
            }

            log.warn("[GeminiClient] La API devolvió una respuesta vacía o sin candidatos válidos.");
            return Optional.empty();

        } catch (Exception e) {
            log.error("[GeminiClient] Error llamando a Gemini API: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Map<String, Object> construirRequestBody(String prompt) {
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("role", "user");
        content.put("parts", Collections.singletonList(part));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.4);
        generationConfig.put("maxOutputTokens", 500);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", Collections.singletonList(content));
        requestBody.put("generationConfig", generationConfig);

        return requestBody;
    }

    private String generarRespuestaMock(String prompt) {
        String lower = prompt.toLowerCase();
        
        // Simular respuestas del asistente basadas en el prompt
        if (lower.contains("sangrado") || lower.contains("hemorragia") || lower.contains("perdida de liquido") || lower.contains("pérdida de líquido")) {
            return "Lo que mencionas puede ser un signo de alarma durante el embarazo. Por seguridad, contacta a tu médico o acude al centro de salud más cercano. Si sientes que es urgente, usa el botón SOS.";
        }
        
        if (lower.contains("resumen") || lower.contains("generar un resumen clínico breve")) {
            return "Resumen Clínico Simulado:\n- Paciente reporta controles normales.\n- Dudas sobre alimentación y suplementos.\n- No se reportan signos de alarma activos.";
        }

        return "Hola. Gracias por tu consulta. Es normal tener dudas durante la gestación. Recuerda mantener tus controles al día y alimentarte de manera saludable. Si tienes algún dolor de cabeza fuerte, zumbido de oídos o sangrado, avísale a tu obstetra de inmediato.";
    }

    // Records internos para mapear la respuesta de Gemini API
    public record GeminiResponse(List<Candidate> candidates) {}
    public record Candidate(Content content) {}
    public record Content(List<Part> parts) {}
    public record Part(String text) {}
}
