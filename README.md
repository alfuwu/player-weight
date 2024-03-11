# Player Weight
Player Weight is a Minecraft fabric mod that adds weight values to all items, along with configurable penalties for breaching weight thresholds.

Player Weight is required on the server, and optionally the client.

## Configuration
Player Weight is config-driven; you can change all (reasonable) aspects of the mod using the config.
For example, you could make every item with 'dirt' in its name completely weightless, by adding an entry to the config.
The config has support for ModMenu, allowing in-game modification as well as being more user-friendly.
By default, the config adjusts the weight of some obvious items to a predetermined value to save you time setting the mod up, however you are free to remove the preset values if you like.

Below is the config entry that prevents air from adding weight to the user's inventory.

```json
{
  "text": "Air",
  "modifier": 0.0,
  "type": "PLAIN"
}
```

Note: setting the `weightModifiersAreMultiplicative` true/false value in the config to false will **not** prevent this entry, specifically, from having its weight set to zero.

In each config entry, there are three fields:
- `text`: This is the text that the mod will use to determine if a given item should have its weight modified
- `modifier`: By default, this value will multiply an item's weight by this amount. However, should `weightModifiersAreMultiplicative` be false, this will add to the item's weight instead
- `type`: This specifies how the mod should use the `text` field in checking if an item is valid for the modifier. It has four possible values: `PLAIN`, where the mod will simply check if `text` is contained within the item's ID, `REGEX`, where the mod will compile `text` as a regular expression and check if the item's ID matches, `ITEM`, where the mod will check if the item's ID matches text exactly, and finally, `TAG`, where it will check if an item is contained within a given tag.

For example, if I wanted only `another_mod:sludgeball` to have 1/10th of the weight it normally would, I would create an entry like this:
```json
{
  "text": "another_mod:sludgeball",
  "modifier": 0.1,
  "type": "ITEM"
}
```

`weightPunishments` is another list of entries inside the config file.
A punishment entry looks something like this:
```json
{
  "type": "SPEED",
  "value": 0.9,
  "begin": 0.8,
  "scaleWithWeight": true
}
```
This entry sets a player's speed to 90% of their original speed when their weight exceeds 80% of their maximum. The `scaleWithWeight` true/false value makes the effect become more and more pronounced the more weight the player tacks on.