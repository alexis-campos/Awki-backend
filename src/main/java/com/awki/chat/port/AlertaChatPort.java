package com.awki.chat.port;

import java.util.UUID;

public interface AlertaChatPort {
    void crearAlertaSiCorresponde(UUID embarazoId, String contenido, boolean alarmaProbable);
}
