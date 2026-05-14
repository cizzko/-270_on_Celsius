package core.content.creatures;

import core.Time;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.Items.ItemStack;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.Textures.TextureDrawing;
import core.content.entity.*;
import core.g2d.Atlas;
import core.g2d.Fill;
import core.math.Rectangle;
import core.math.Vector2f;
import core.util.Color;

import static core.Global.*;
import static core.World.Textures.TextureDrawing.blockSize;

public class ItemEntity implements LivingEntity {
    private static final int ITEM_DROPPED_SIZE = 32;
    private static final int ITEM_DROPPED_HITBOX = ITEM_DROPPED_SIZE + 2;

    protected short id;
    protected float x, y;
    protected ItemStack itemStack;
    protected float hp, phase;
    protected boolean isUnbreakable, dead;
    protected Vector2f velocity = new Vector2f();

    public ItemEntity(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    @Override
    public final short getId() {
        return id;
    }

    @Override
    public final void setId(int id) {
        this.id = (short) id;
    }

    @Override
    public final float getX() { return x; }

    @Override
    public final float getY() { return y; }

    @Override
    public final void setX(float x) { this.x = x; }
    @Override
    public final void setY(float y) { this.y = y; }

    @Override
    public final float getHp() { return hp; }

    @Override
    public final void setPosition(float x, float y) { this.x = x; this.y = y; }

    @Override
    public void getHitboxTo(Rectangle out) {
        out.set(x, y, ITEM_DROPPED_SIZE, ITEM_DROPPED_SIZE);
    }

    @Override
    public CollisionResult onCollide(HitboxComponent them) {
        if (them instanceof PlayerEntity) {
            Inventory.addItemStack(itemStack);
            remove();
        } else if (them instanceof ItemEntity other &&
                   other.itemStack.isSame(itemStack)) {
            if (itemStack.getCount() > other.itemStack.getCount()) {
                itemStack.merge(other.itemStack);
                other.remove();
            } else {
                other.itemStack.merge(itemStack);
                remove();
            }
        }
        return CollisionResult.WALKTHROUGH;
    }

    @Override
    public void update() {
        final float STOP_DST = ITEM_DROPPED_HITBOX;
        final float MOVE_DST = (3*ITEM_DROPPED_HITBOX);

        float dstToPlayer = player.dst2(x, y);
        var playerTex = player.creature.texture;
        float pcx = player.getX() + playerTex.width()/4f;
        float pcy = player.getY() + playerTex.height()/4f;

        if (dstToPlayer <= STOP_DST*STOP_DST) {
            // velocity.set(pcx - x, pcy - y);
            velocity.lerp(pcx - x, pcy - y, 0.025f);
            // velocity.lerpDeltaTime(pcx - x, pcy - y, 0.05f);
        } else if (dstToPlayer <= MOVE_DST*MOVE_DST) {
            float base = 0.0025f * getWeight();
            float alpha = 1 - (float)Math.pow(1 - base, Time.delta);
            velocity.lerp(pcx - x, pcy - y, alpha);
        }
    }

    @Override
    public void draw(float drawX) {
        float amplitude = blockSize / 4f;
        float frequency = 0.45f;
        float periodTicks = Time.ONE_SECOND / frequency;
        phase = (phase + Time.delta) % periodTicks;

        if (phase < 0f) {
            phase += periodTicks;
        }
        float tSeconds = phase / Time.ONE_SECOND;
        float yOffset = amplitude * 0.5f * (1f - (float)Math.cos(2f * Math.PI * frequency * tSeconds));
        // System.out.println("phase = " + phase + " | " + phase + " | " + tSeconds + " | " + yOffset);
        Atlas.Region tex = itemStack.getItem().texture;
        batch.draw(tex, drawX, y + yOffset, ITEM_DROPPED_SIZE, ITEM_DROPPED_SIZE);
        Fill.rectangleBorder(drawX, y + yOffset, ITEM_DROPPED_SIZE, ITEM_DROPPED_SIZE, Color.WHITE);
        if (itemStack.getCount() > 1) {
            TextureDrawing.drawText(drawX, y+yOffset, String.valueOf(itemStack.getCount()), Color.WHITE);
        }
    }

    @Override
    public boolean hasFloor() {
        int minX = (int) Math.floor(x / blockSize);
        int maxX = (int) Math.floor((x + ITEM_DROPPED_SIZE - GAP) / blockSize);
        int minY = (int) Math.floor((y - GAP) / blockSize);

        for (int x = minX; x <= maxX; x++) {
            int blockId = world.getBlockId(x, minY);
            if (blockId <= 0)
                continue;
            var block = content.blocksRegistry.typeById(blockId);
            if (block.type == StaticObjectsConst.Type.SOLID) {
                return true;
            }
        }
        return false;
    }

    @Override
    public float getMaxHp() {
        return 100;
    }

    @Override
    public boolean isUnbreakable() {
        return isUnbreakable;
    }

    @Override
    public boolean isDead() {
        return dead;
    }

    @Override
    public void setHp(float hp) {
        this.hp = hp;
        if (hp <= 0)
            remove();
    }

    @Override
    public void damage(float d, DamageSource source) {
        if (source == DamageSource.FALL || dead)
            return;

        this.hp -= d;
        if (hp <= 0)
            remove();
    }

    @Override
    public void setUnbreakable(boolean unbreakable) {

    }

    @Override
    public Vector2f getVelocity() {
        return velocity;
    }

    @Override
    public void jump(float impulse) {

    }

    @Override
    public void moveAt(Vector2f vel) {

    }

    @Override
    public float getWeight() {
        return itemStack.getItem().weight;
    }

    @Override
    public void remove() {
        LivingEntity.super.remove();

        dead = true;
        hp = 0;
        velocity.set(0, 0);
    }

    @Override
    public String toString() {
        return "Item[" + itemStack + "]#" + id;
    }
}
