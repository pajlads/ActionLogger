# Action Logger

Action Logger logs actions your character makes to disk in a programmatically accessible format. These actions intend to make debugging quest helpers easier.

The files are stored in your RuneLite folder under the `actionlogger` directory. At each start, a file is created with the current unix timestamp (e.g. `1724499647864-logs.txt`).  
Each new line contains an action defined in a JSON format

## Types of actions logged

### DIALOGUE_STARTED

Fires when a dialogue is opened

```json5
{
  "tick": 95,
  "ts": "2024-08-25T08:59:42Z",
  "type": "DIALOGUE_STARTED",
  "data": {
    "actorName": "pajdenk",
    "lastInteractedName": "Lumbridge Guide",
    "lastInteractedID": 306,
    "lastInteractedPosition": {
      "x": 3238,
      "y": 3220,
      "plane": 0
    },
    "playerPosition": {
      "x": 3237,
      "y": 3220,
      "plane": 0
    },
    "dialogueText": "",
    "dialogueOptions": [
      "Select an option",
      "Where can I find a quest to go on?",
      "What monsters should I fight?",
      "Where can I make money?",
      "Where can I find more information?",
      "More options..."
    ]
  }
}
```

### DIALOGUE_ENDED

Fires when a dialogue ends, either by the user selecting an option or leaving the dialogue

Where possible, the `dialogueOptionChosen` will be filled in with information about which of the dialogue options was chosen, -1 meaning they didn't chose an option or they chose "Continue" which usually isn't included in the `dialogueOptions` key

```json5
{
  "tick": 101,
  "ts": "2024-08-25T08:59:42Z",
  "type": "DIALOGUE_ENDED",
  "data": {
    "actorName": "pajdenk",
    "lastInteractedName": "Lumbridge Guide",
    "lastInteractedID": 306,
    "lastInteractedPosition": {
      "x": 3238,
      "y": 3220,
      "plane": 0
    },
    "playerPosition": {
      "x": 3237,
      "y": 3220,
      "plane": 0
    },
    "dialogueText": "",
    "dialogueOptions": [
      "Select an option",
      "Where can I find a quest to go on?",
      "What monsters should I fight?",
      "Where can I make money?",
      "Where can I find more information?",
      "More options..."
    ],
    "dialogueOptionChosen": -1
  }
}
```

### VARBIT_CHANGED

Fires when a Varbit value changes

You can Inspect the Varbit in Chisel, e.g. https://chisel.weirdgloop.org/varbs/display?varbit=10060

```json5
{
  "tick": 104,
  "ts": "2024-08-25T08:59:42Z",
  "type": "VARBIT_CHANGED",
  "data": {
    // ID of the Varbit being changed
    "id": 10060,
    "oldValue": 0,
    "newValue": 2
  }
}
```

### VARPLAYER_CHANGED

Fires when a Varplayer value changes

You can Inspect the Varplayer in Chisel, e.g. https://chisel.weirdgloop.org/varbs/display?varplayer=3803

```json5
{
  "tick": 0,
  "ts": "2024-08-25T08:59:42Z",
  "type": "VARPLAYER_CHANGED",
  "data": {
    // ID of the Varplayer being changed
    "id": 3803,
    "oldValue": 0,
    "newValue": 20000
  }
}
```

### INVENTORY_CHANGED

Fires when the player's inventory changes

#### User picked up a cabbage into the first slot

```json5
{
  "tick": 0,
  "ts": "2024-08-25T08:59:42Z",
  "type": "INVENTORY_CHANGED",
  "data": {
    "oldInventory": [
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
    ],
    "oldQuantities": [
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0
    ],
    "newInventory": [
      1965, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
    ],
    "newQuantities": [
      1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0
    ]
  }
}
```

#### User moved the cabbage from the first slot to the last slot

```json5
{
  "tick": 0,
  "ts": "2024-08-25T08:59:42Z",
  "type": "INVENTORY_CHANGED",
  "data": {
    "oldInventory": [
      1965, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1
    ],
    "oldQuantities": [
      1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0
    ],
    "newInventory": [
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
      -1, -1, -1, -1, -1, -1, -1, -1, -1, 1965
    ],
    "newQuantities": [
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 1
    ]
  }
}
```

## Chat commands

There are various helpful chat commands available when the Action Logger plugin is installed.

To invoke a command, type `::ActionLogger <COMMAND>` or `::ActLog <COMMAND>` with a command listed below

### Restart

Ends the logging of the current file and starts logging to a new file.

Example: `::actlog restart`

### Dump

Dumps all nearby objects, ground items, and npcs to the Action Logger file. Coordinates are all scene-relative

To dump everything: `::actlog dump`
To dump NPCs only: `::actlog dump npcs`
To dump NPCs and ground items only: `::actlog dump npcs grounditems`

Example output:
```json
{
  "tick": 1449,
  "ts": "2025-07-12T14:08:59Z",
  "type": "DUMP",
  "data": {
    "scene": {
      "baseX": 3152,
      "baseY": 3160,
      "isInstance": false
    },
    "worldViewPlane": 1,
    "groundItems": [
      {
        "x": 42,
        "y": 41,
        "z": 0,
        "id": 526,
        "name": "Bones",
        "quantity": 1
      }
    ],
    "objects": [
      {
        "x": 1,
        "y": 1,
        "z": 0,
        "id": 4737,
        "type": "GROUND"
      }
    ],
    "npcs": [
      {
        "x": 56,
        "y": 60,
        "z": 1,
        "id": 3106,
        "name": "Man"
      }
    ]
  }
}
```