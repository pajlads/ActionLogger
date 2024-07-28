# Action Logger

Action Logger logs actions your character makes to disk in a programmatically accessible format. These actions intend to make debugging quest helpers easier.

The files are stored in your RuneLite folder under the `actionlogger` directory. At each start, a file is created with the current unix timestamp (e.g. `1724499647864-logs.txt`).  
Each new line contains an action defined in a JSON format

## Types of actions logged

### DIALOGUE_STARTED

Fires when a dialogue is opened

```json5
{
  "tickCount": 95,
  "timestamp": "2024-08-24 13:23:19.312",
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
  "tickCount": 101,
  "timestamp": "2024-08-24 13:23:22.912",
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
  "tickCount": 104,
  "timestamp": "2024-08-24 13:23:24.692",
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
  "tickCount": 0,
  "timestamp": "2024-08-24 13:22:22.092",
  "type": "VARPLAYER_CHANGED",
  "data": {
    // ID of the Varplayer being changed
    "id": 3803,
    "oldValue": 0,
    "newValue": 20000
  }
}
```
