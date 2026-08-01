// SPDX-FileCopyrightText: 2026 pajlada <rasmus.karlsson@pajlada.com>
//
// SPDX-License-Identifier: BSD-2-Clause

package actionlogger.trackers;

import actionlogger.writers.JsonWriter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@RequiredArgsConstructor
public class ChatTracker {
    private final JsonWriter writer;

    @Subscribe
    public void onChatMessage(ChatMessage e) {
        if (e.getType() == ChatMessageType.ITEM_EXAMINE ||
            e.getType() == ChatMessageType.GAMEMESSAGE ||
            e.getType() == ChatMessageType.MESBOX ||
            e.getType() == ChatMessageType.DIALOG ||
            e.getType() == ChatMessageType.CONSOLE) {
            this.writer.write("CHAT", new ChatTracker.ChatData(e.getType().toString(), e.getName(), e.getMessage()));
        }
    }


    @Value
    private static class ChatData {
        String type;
        String name;
        String message;
    }
}
