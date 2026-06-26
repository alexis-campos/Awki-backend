package com.awki.websocket.controller;

import com.awki.websocket.dto.PingMessage;
import com.awki.websocket.dto.PongMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.time.Instant;

@Controller
public class WebSocketController {

    @MessageMapping("/ping")
    @SendToUser("/queue/pong")
    public PongMessage handlePing(PingMessage ping) {
        return new PongMessage("pong", Instant.now().toEpochMilli());
    }
}
