package com.smartambulance.websocket;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class LiveEventBroadcaster {

    private final LiveWebSocketHandler webSocketHandler;
    private final List<LiveEvent> recentEvents = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_RECENT = 100;

    public LiveEventBroadcaster(LiveWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    public void broadcast(LiveEvent event) {
        recentEvents.add(0, event);
        while (recentEvents.size() > MAX_RECENT) {
            recentEvents.remove(recentEvents.size() - 1);
        }
        webSocketHandler.broadcast(event);
    }

    public List<LiveEvent> getRecentEvents(int limit) {
        synchronized (recentEvents) {
            int toIndex = Math.min(limit, recentEvents.size());
            return new ArrayList<>(recentEvents.subList(0, toIndex));
        }
    }
}
