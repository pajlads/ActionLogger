package actionlogger.trackers;

import actionlogger.writers.JsonWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyListener;

import javax.annotation.Nullable;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.awt.event.KeyEvent.*;

@Slf4j
@RequiredArgsConstructor
public class DialogueTracker implements KeyListener {
    private final JsonWriter writer;
    private final Client client;
    private @Nullable DialogueEndedData dialogueEndedData = null;
    private String lastInteractedNpcName;
    private Integer lastInteractedNpcID;
    private WorldPoint lastInteractedNpcPosition;
    private Integer lastDialogueWidgetID;
    private String lastDialogueText;
    private List<String> lastDialogueOptions;

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (this.dialogueEndedData != null && this.dialogueEndedData.dialogueOptionChosen == null) {
            if (this.dialogueEndedData.dialogueOptions != null && this.dialogueEndedData.dialogueOptions.size() >= 2) {
                switch (e.getKeyCode()) {
                    case VK_1:
                        this.dialogueEndedData.dialogueOptionChosen = 1;
                        break;

                    case VK_2:
                        this.dialogueEndedData.dialogueOptionChosen = 2;
                        break;

                    case VK_3:
                        if (this.dialogueEndedData.dialogueOptions.size() >= 3) {
                            this.dialogueEndedData.dialogueOptionChosen = 3;
                        }
                        break;

                    case VK_4:
                        if (this.dialogueEndedData.dialogueOptions.size() >= 4) {
                            this.dialogueEndedData.dialogueOptionChosen = 4;
                        }
                        break;

                    case VK_5:
                        if (this.dialogueEndedData.dialogueOptions.size() >= 5) {
                            this.dialogueEndedData.dialogueOptionChosen = 5;
                        }
                        break;

                    case VK_6:
                        if (this.dialogueEndedData.dialogueOptions.size() >= 6) {
                            this.dialogueEndedData.dialogueOptionChosen = 6;
                        }
                        break;

                    case VK_7:
                        if (this.dialogueEndedData.dialogueOptions.size() >= 7) {
                            this.dialogueEndedData.dialogueOptionChosen = 7;
                        }
                        break;
                }
            } else {
                if (e.getKeyCode() == VK_SPACE) {
                    this.dialogueEndedData.dialogueOptionChosen = -1;
                }
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    private void beginDialogue(String actorName, Integer actorID, String text, List<String> options) {
        assert this.dialogueEndedData == null;

        var localPlayer = client.getLocalPlayer();
        if (localPlayer == null) {
            log.debug("Dialogue began, but no local player found");
            return;
        }

        var playerPosition = localPlayer.getWorldLocation();

        this.writer.write(DialogueStartedData.TYPE, new DialogueStartedData(actorName, actorID, lastInteractedNpcName, lastInteractedNpcID, lastInteractedNpcPosition, playerPosition, text, options));

        this.dialogueEndedData = new DialogueEndedData(actorName, actorID, lastInteractedNpcName, lastInteractedNpcID, lastInteractedNpcPosition, playerPosition, text, options);

        this.lastDialogueText = text;
        this.lastDialogueOptions = options;
    }

    private void endDialogue() {
        assert this.dialogueEndedData != null;

        this.writer.write(DialogueEndedData.TYPE, this.dialogueEndedData);

        this.dialogueEndedData = null;
    }

    @Subscribe
    public void onInteractingChanged(InteractingChanged event) {
        if (event.getSource() == client.getLocalPlayer() && event.getTarget() != null) {
            var target = event.getTarget();
            if (target instanceof NPC) {
                var npc = ((NPC) target);
                lastInteractedNpcName = npc.getName();
                lastInteractedNpcID = npc.getId();
                lastInteractedNpcPosition = npc.getWorldLocation();
            } else {
                log.debug("Interacting changed to '{}' (NON NPC)", event.getTarget().getName());
            }
        }
    }

    @Subscribe
    void onGameTick(GameTick e) {
        var npcDialogueTextWidget = client.getWidget(ComponentID.DIALOG_NPC_TEXT);
        var playerDialogueTextWidget = client.getWidget(ComponentID.DIALOG_PLAYER_TEXT);
        var playerDialogueOptionsWidget = client.getWidget(ComponentID.DIALOG_OPTION_OPTIONS);
        // TODO: Implement other widgets (as seen in the crowd-sourcing plugin)
        // Ensure quest dialogue that appears during quest cutscenes work as expected

        if (this.dialogueEndedData != null && this.lastDialogueWidgetID != null) {
            var w = client.getWidget(this.lastDialogueWidgetID);
            if (w == null) {
                // Previous widget closed, so the dialogue ended
                this.endDialogue();
            } else {
                if (!w.getText().equals(this.lastDialogueText)) {
                    // The same widget is open, but the text has changed
                    this.endDialogue();
                } else {
                    var children = w.getChildren();
                    if (children != null) {
                        var dialogueOptions = Arrays.stream(children).filter(Objects::nonNull).map(Widget::getText).filter(s -> !s.isBlank()).collect(Collectors.toList());
                        if (!dialogueOptions.equals(this.lastDialogueOptions)) {
                            // The same widget is option, but the options have changed
                            this.endDialogue();
                        }
                    }
                }
            }
        }

        if (this.dialogueEndedData == null) {
            if (npcDialogueTextWidget != null) {
                this.lastDialogueWidgetID = npcDialogueTextWidget.getId();

                String actorName = null;
                Integer actorID = null;
                var modelWidget = client.getWidget(ComponentID.DIALOG_NPC_HEAD_MODEL);
                if (modelWidget != null) {
                    actorID = modelWidget.getModelId();
                }
                var nameWidget = client.getWidget(ComponentID.DIALOG_NPC_NAME);
                if (nameWidget != null) {
                    actorName = nameWidget.getText();
                }

                this.beginDialogue(actorName, actorID, npcDialogueTextWidget.getText(), null);
            } else if (playerDialogueTextWidget != null) {
                this.lastDialogueWidgetID = playerDialogueTextWidget.getId();

                String actorName = client.getLocalPlayer().getName();

                this.beginDialogue(actorName, null, playerDialogueTextWidget.getText(), null);
            } else if (playerDialogueOptionsWidget != null) {
                this.lastDialogueWidgetID = playerDialogueOptionsWidget.getId();

                var children = playerDialogueOptionsWidget.getChildren();
                if (children == null) {
                    log.debug("Dialog options without children?");
                    return;
                }
                var dialogueOptions = Arrays.stream(children).filter(Objects::nonNull).map(Widget::getText).filter(s -> !s.isBlank()).collect(Collectors.toList());

                String actorName = client.getLocalPlayer().getName();

                this.beginDialogue(actorName, null, playerDialogueOptionsWidget.getText(), dialogueOptions);
            }
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked e) {
        if (e.getMenuAction() == MenuAction.WIDGET_CONTINUE) {
            if (this.dialogueEndedData != null && this.dialogueEndedData.dialogueOptionChosen == null) {
                // 1-indexed, so first option is 1, second option is 2
                this.dialogueEndedData.dialogueOptionChosen = e.getParam0();
            }
        }
    }


    @RequiredArgsConstructor
    private static class DialogueStartedData {
        public static final String TYPE = "DIALOGUE_STARTED";

        private final String actorName;
        private final Integer actorID;
        private final String lastInteractedName;
        private final Integer lastInteractedID;
        private final WorldPoint lastInteractedPosition;
        private final WorldPoint playerPosition;
        private final String dialogueText;
        private final List<String> dialogueOptions;
    }

    @RequiredArgsConstructor
    private static class DialogueEndedData {
        public static final String TYPE = "DIALOGUE_ENDED";

        private final String actorName;
        private final Integer actorID;
        private final String lastInteractedName;
        private final Integer lastInteractedID;
        private final WorldPoint lastInteractedPosition;
        private final WorldPoint playerPosition;
        private final String dialogueText;
        private final List<String> dialogueOptions;
        private Integer dialogueOptionChosen = -1;
    }
}
