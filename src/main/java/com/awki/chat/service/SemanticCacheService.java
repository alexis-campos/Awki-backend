package com.awki.chat.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SemanticCacheService {

    private static final Logger log = LoggerFactory.getLogger(SemanticCacheService.class);
    private static final String CACHE_PREFIX = "chat:semantic:";

    private final StringRedisTemplate redisTemplate;

    public Optional<String> buscarEnCache(String consulta) {
        try {
            String hash = generarHash(consulta);
            String clave = CACHE_PREFIX + hash;
            String respuesta = redisTemplate.opsForValue().get(clave);

            if (respuesta != null) {
                log.info("[SemanticCache] HIT para la consulta (hash: {})", hash);
                return Optional.of(respuesta);
            }
        } catch (Exception e) {
            log.warn("[SemanticCache] Error al consultar Redis (resiliencia activada): {}", e.getMessage());
        }
        return Optional.empty();
    }

    public void guardarEnCache(String consulta, String respuesta) {
        if (!esCacheable(consulta, respuesta)) {
            log.info("[SemanticCache] Omitiendo caché para consulta/respuesta por contener datos sensibles o variables");
            return;
        }

        try {
            String hash = generarHash(consulta);
            String clave = CACHE_PREFIX + hash;
            redisTemplate.opsForValue().set(clave, respuesta, Duration.ofHours(24));
            log.info("[SemanticCache] Guardada respuesta en Redis (hash: {}, TTL: 24h)", hash);
        } catch (Exception e) {
            log.warn("[SemanticCache] Error al escribir en Redis (resiliencia activada): {}", e.getMessage());
        }
    }

    public String normalizar(String consulta) {
        if (consulta == null) {
            return "";
        }
        // Minúsculas, recortar espacios
        String normalizada = consulta.trim().toLowerCase();
        // Quitar acentos
        normalizada = Normalizer.normalize(normalizada, Normalizer.Form.NFD);
        normalizada = normalizada.replaceAll("\\p{M}", "");
        // Quitar signos de puntuación innecesarios
        normalizada = normalizada.replaceAll("[^a-z0-9\\s]", "");
        return normalizada;
    }

    private String generarHash(String consulta) {
        String normalizada = normalizar(consulta);
        return DigestUtils.md5DigestAsHex(normalizada.getBytes(StandardCharsets.UTF_8));
    }

    private boolean esCacheable(String consulta, String respuesta) {
        String text = (consulta + " " + respuesta).toLowerCase();

        // 1. No cachear si contiene marcas de alarma
        if (contieneMarcasAlarma(text)) {
            return false;
        }

        // 2. No cachear semanas de gestación o trimestre específico
        if (text.contains("semana") || text.contains("trimestre") || text.contains("sg")) {
            return false;
        }

        // 3. No cachear si hay mención a antecedentes clínicos
        if (text.contains("antecedente") || text.contains("diabetes") || text.contains("hipertens") || text.contains("preeclamp")) {
            return false;
        }

        // 4. No cachear si hay datos que puedan identificar o personalizar
        if (text.contains("nombre") || text.contains("dni") || text.contains("edad") || text.contains("años")) {
            return false;
        }

        return true;
    }

    private boolean contieneMarcasAlarma(String text) {
        return text.contains("sangrado") || text.contains("hemorragia") 
                || text.contains("cefalea") || text.contains("dolor de cabeza")
                || text.contains("vision borrosa") || text.contains("vision doble")
                || text.contains("convulsion") || text.contains("fiebre")
                || text.contains("perdida de liquido") || text.contains("perdida de agua")
                || text.contains("no siento movimientos") || text.contains("no se mueve")
                || text.contains("dolor abdominal") || text.contains("dolor de barriga")
                || text.contains("hinchazon") || text.contains("presion alta")
                || text.contains("zumbido");
    }
}
