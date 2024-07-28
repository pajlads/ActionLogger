package actionlogger.trackers;

import actionlogger.Utils;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.awt.event.KeyEvent.*;

@Slf4j
public class DialogueTracker implements KeyListener {
    private final FileOutputStream fh;
    private final Client client;
    private @Nullable DialogueData dialogueData = null;
    private String lastInteractedNpcName;
    private Integer lastInteractedNpcID;
    private WorldPoint lastInteractedNpcPosition;
    private Integer lastDialogueWidgetID;
    private String lastDialogueText;
    private List<String> lastDialogueOptions;

    public DialogueTracker(FileOutputStream fh, Client client) {
        this.fh = fh;
        this.client = client;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (this.dialogueData != null && this.dialogueData.dialogueOptionChosen == null) {
            if (this.dialogueData.dialogueOptions != null && this.dialogueData.dialogueOptions.size() >= 2) {
                switch (e.getKeyCode()) {
                    case VK_1:
                        this.dialogueData.dialogueOptionChosen = 1;
                        break;

                    case VK_2:
                        this.dialogueData.dialogueOptionChosen = 2;
                        break;

                    case VK_3:
                        if (this.dialogueData.dialogueOptions.size() >= 3) {
                            this.dialogueData.dialogueOptionChosen = 3;
                        }
                        break;

                    case VK_4:
                        if (this.dialogueData.dialogueOptions.size() >= 4) {
                            this.dialogueData.dialogueOptionChosen = 4;
                        }
                        break;

                    case VK_5:
                        if (this.dialogueData.dialogueOptions.size() >= 5) {
                            this.dialogueData.dialogueOptionChosen = 5;
                        }
                        break;

                    case VK_6:
                        if (this.dialogueData.dialogueOptions.size() >= 6) {
                            this.dialogueData.dialogueOptionChosen = 6;
                        }
                        break;

                    case VK_7:
                        if (this.dialogueData.dialogueOptions.size() >= 7) {
                            this.dialogueData.dialogueOptionChosen = 7;
                        }
                        break;
                }
            } else {
                if (e.getKeyCode() == VK_SPACE) {
                    this.dialogueData.dialogueOptionChosen = -1;
                }
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    private void beginDialogue(String actorName, Integer actorID, String text, List<String> options) {
        assert this.dialogueData == null;

        this.lastDialogueText = text;
        this.lastDialogueOptions = options;

        this.dialogueData = new DialogueData();
        this.dialogueData.actorName = actorName;
        this.dialogueData.actorID = actorID;
        this.dialogueData.lastInteractedName = lastInteractedNpcName;
        this.dialogueData.lastInteractedID = lastInteractedNpcID;
        this.dialogueData.lastInteractedPosition = lastInteractedNpcPosition;
        this.dialogueData.playerPosition = client.getLocalPlayer().getWorldLocation();
        this.dialogueData.dialogueText = text;
        this.dialogueData.dialogueOptions = options;
    }

    private void endDialogue() {
        assert this.dialogueData != null;

        // TODO: Make "Dialog shown" and "Dialog finished" separate events
        // TODO: Write a json object?

        this.write(this.dialogueData.buildString());

        this.dialogueData = null;
    }

    private void write(String body) {
        try {
            fh.write(String.format("[%d, %s] %s\n", client.getTickCount(), Utils.getTimestamp(), body).getBytes());
        } catch (IOException e) {
            log.warn("Failed to write DialogueTracker data: {}", e.getMessage());
        }
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
        // Ensure quest dialogue that appears during quests/cinematics/cutscenes work as expected

        if (this.dialogueData != null && this.lastDialogueWidgetID != null) {
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

        if (this.dialogueData == null) {
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
            if (this.dialogueData != null && this.dialogueData.dialogueOptionChosen == null) {
                // 1-indexed, so first option is 1, second option is 2
                this.dialogueData.dialogueOptionChosen = e.getParam0();
            }
        }
    }

    static class DialogueData {
        public String actorName;
        public Integer actorID;
        public String lastInteractedName;
        public Integer lastInteractedID;
        public WorldPoint lastInteractedPosition;
        public WorldPoint playerPosition;
        public String dialogueText;
        public List<String> dialogueOptions;
        public Integer dialogueOptionChosen;

        public String buildString() {
            var b = new StringBuilder();
            b.append("DialogueData{");
            b.append(String.format("actorName=%s, ", this.actorName));
            b.append(String.format("actorID=%d, ", this.actorID));
            b.append(String.format("lastInteractedName=%s, ", this.lastInteractedName));
            b.append(String.format("lastInteractedID=%d, ", this.lastInteractedID));
            b.append(String.format("lastInteractedPosition=%s, ", this.lastInteractedPosition));
            b.append(String.format("playerPosition=%s, ", this.playerPosition));
            b.append(String.format("dialogueText=%s, ", this.dialogueText));
            b.append(String.format("dialogueOptions=%s, ", this.dialogueOptions));
            b.append(String.format("dialogueOptionChosen=%d", this.dialogueOptionChosen));
            b.append("}");

            return b.toString();
        }
    }
}
