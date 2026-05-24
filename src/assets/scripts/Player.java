import core.Global;
import core.World.Creatures.Player.Player;
import core.World.WorldUtils;

import static core.Global.*;

void onNoClip() {
    Player.noClip = true;
}

void offNoClip() {
    Player.noClip = false;
}

void toggleNoClip() {
    Player.noClip = !Player.noClip;
}

boolean kill(int id) {
    var entity = Global.entityPool.getEntity(id);
    if (entity != null) {
        entity.remove();
        Application.log.info("Killed entity {} ({})", entity.id(), entity);
        return true;
    }
    return false;
}

void spawnPlayer() {
    Global.player = WorldUtils.spawn(content.creatureById("player"), true);
}

void spawn(String creatureId) {
    var creatureType = content.creaturesRegistry.typeByName(creatureId);
    if (creatureType == null) {
        log.error("Unknown creature with id: '{}'", creatureId);
        return;
    }
    var spawn = WorldUtils.spawn(creatureType, true);
    spawn.setPosition(player.x(), player.y());
}

void give(String itemId) {
    give(itemId, 1);
}

void give(String itemId, int amount) {
    var item = content.itemsRegistry.typeByName(itemId);
    if (item == null) {
        log.error("Unknown item with id: '{}'", itemId);
        return;
    }
    Inventory.addItemStack(new ItemStack(item, amount));
}

void tpBlock(float x, float y) {
    player.setPosition(x, y);
}
