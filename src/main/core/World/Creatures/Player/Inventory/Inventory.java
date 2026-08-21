package core.World.Creatures.Player.Inventory;

import core.Global;
import core.Time;
import core.World.Creatures.Physics;
import core.World.WorldUtils;
import core.content.blocks.Block;
import core.content.creatures.ItemEntity;
import core.content.ItemStack;
import core.content.items.Item;
import core.content.items.ItemBlock;
import core.content.entity.comp.InventoryComponent;
import core.g2d.Atlas;
import core.g2d.StackfulRender;
import core.graphic.WorldDrawing;
import core.math.Point2i;
import core.util.Config;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static core.Global.*;
import static core.World.WorldUtils.getDistanceToMouse;
import static core.WorldCoordinates.toWorld;
import static core.graphic.Color.rgba8888;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_TAB;

public class Inventory {
    public static boolean inventoryOpen = false;
    private static final boolean buildGrid = Config.getBoolean("BuildGrid");

    private static final int VISIBLE_COLUMNS = 6;
    private static final int VISIBLE_ROWS = 5;
    private static final int START_Y_OFFSET = 4;
    private static final int HORIZONTAL_OFFSET = 1;
    private static final int SEARCH_RADIUS = 20;
    private static final float FOLLOW_SPEED = 0.1f;
    private static final float LEAVE_SPEED = 0.1f;
    private static final float MAX_HOLD_DISTANCE = 10f;
    private static final int MAX_PLACEMENT_VERTICAL = 15;
    private static final int MAX_PLACEMENT_HORIZONTAL = 20;

    private static float animationPhase = 0f;
    private static final float ANIMATION_PERIOD_TICKS = Time.ONE_SECOND / 0.45f;
    private static final float MAX_DROP_SPEED = 1.5f;
    private static final float MIN_DROP_RADIAL_SPEED = 0.4f;
    private static final int DROP_GRACE_TICKS = 15;

    private static int scrollOffset = 0;
    private static boolean testFilled = false;

    private static final ArrayList<ItemStack> allItems = new ArrayList<>();
    private static final ArrayList<Point2i> allSlots = new ArrayList<>();

    private static final ArrayList<ActiveEntity> activeEntities = new ArrayList<>();

    private static ArrayList<Point2i> candidates = new ArrayList<>();

    private static final ArrayList<Integer> display = new ArrayList<>();
    private static final ArrayList<Point2i> targets = new ArrayList<>();

    private static final Map<String, Integer> pinned = new HashMap<>();
    private static final Map<Integer, Integer> candidateByItem = new HashMap<>();
    private static final Map<String, Point2i> relCell = new HashMap<>();
    private static int lastScroll = 0;
    private static long lastSig = 0;

    private static final int[][] DIRS = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

    private static final float DRAG_START_DISTANCE = 10f;
    private static int dragIdx = -1;
    private static boolean dragMoved = false;
    private static float dragStartX = 0, dragStartY = 0;
    private static final Point2i dragFrom = new Point2i();

    public static void update() {
        if (input.justPressed(GLFW_KEY_TAB)) {
            inventoryOpen = !inventoryOpen;
            if (inventoryOpen) {
                open();
            } else {
                close();
            }
        }

        if (!inventoryOpen) {
            return;
        }

        float scrollDelta = input.scrollDelta();
        if (scrollDelta != 0 && !allItems.isEmpty()) {
            scrollOffset = (scrollOffset + (scrollDelta > 0 ? 1 : -1) + allItems.size()) % allItems.size();
            onScroll();
        } else {
            reconcile();
        }

        updateActive();
        updateSelection();

        if (input.justClicked(GLFW_MOUSE_BUTTON_LEFT)) {
            int i = entityAtMouse();
            if (i >= 0) {
                previewClick(i);
                startDrag(i);
            }
        }
        if (dragIdx >= 0 && input.clicked(GLFW_MOUSE_BUTTON_LEFT)) {
            dragMove();
        }
        if (input.releasedButton(GLFW_MOUSE_BUTTON_LEFT) && dragIdx >= 0) {
            endDrag();
        }
        if (input.justClicked(GLFW_MOUSE_BUTTON_RIGHT)) {
            togglePin();
        }
    }

    private static void open() {
        lastSig = 0;
        fillTest();
        buildItems();
        if (!allItems.isEmpty()) {
            scrollOffset = ((scrollOffset % allItems.size()) + allItems.size()) % allItems.size();
        } else {
            scrollOffset = 0;
        }
        calcCandidates();
        rebuildVisible();
    }

    //todo
    private static void fillTest() {
        if (testFilled) {
            return;
        }
        testFilled = true;
        String[] ids = {
                "stone", "dirt", "sand", "grass", "copper", "iron", "aluminum",
                "trunk", "foliage", "smallStone", "stick", "stoneGear", "copperIngot",
                "glass", "salt", "stoneDust", "copperWire", "glassLens", "aluminumPipe",
                "simpleElectricMotor"
        };
        for (String id : ids) {
            player.addItem(new ItemStack(content.itemById(id)));
        }
    }

    private static void close() {
        clearActive();
        allItems.clear();
        allSlots.clear();
        candidates.clear();
        lastSig = 0;
        candidateByItem.clear();
        relCell.clear();
        lastScroll = 0;
        resetDrag();
    }

    private static void buildItems() {
        allItems.clear();
        allSlots.clear();
        var items = player.items();
        for (int row = 0; row < items.size(); row++) {
            var line = items.get(row);
            for (int col = 0; col < line.size(); col++) {
                ItemStack stack = line.get(col);
                if (stack != null) {
                    allItems.add(stack);
                    allSlots.add(new Point2i(row, col));
                }
            }
        }
    }

    private static void clearActive() {
        for (ActiveEntity a : activeEntities) {
            if (!a.entity.isDead()) {
                a.entity.remove();
            }
        }
        activeEntities.clear();
    }

    private static ItemEntity createPreview(ItemStack stack, Point2i pos) {
        ItemEntity ent = WorldUtils.spawnItemEntity(stack, pos.x, pos.y);
        ent.updateLastPosition();
        ent.preview = true;
        ent.setPhase(animationPhase);
        return ent;
    }

    private static void addActive(ItemEntity ent, int itemIdx, Point2i target, boolean leaving, boolean entering) {
        activeEntities.add(new ActiveEntity(ent, itemIdx, target, leaving, entering, 0));
    }

    private static void rebuildVisible() {
        clearActive();
        computeDisplay();
        for (int i = 0; i < display.size(); i++) {
            int itemIdx = display.get(i);
            Point2i target = targets.get(i);
            addActive(createPreview(allItems.get(itemIdx), target), itemIdx, target, false, false);
        }
    }

    private static void onScroll() {
        reconcile();
    }

    private static void reconcile() {
        int playerStackCount = countStacks();

        if (playerStackCount != allItems.size()) {
            buildItems();
            calcCandidates();
            computeDisplay();
            refresh();
            return;
        }

        calcCandidates();
        computeDisplay();
        if (display.isEmpty()) {
            return;
        }

        Set<Integer> newVisible = new HashSet<>(display);

        for (ActiveEntity a : activeEntities) {
            if (a.leaving) {
                continue;
            }
            if (!newVisible.contains(a.itemIndex)) {
                a.leaving = true;
                a.entering = false;
                a.target = new Point2i(player.blockX(), player.blockY());
            }
        }

        for (int i = 0; i < display.size(); i++) {
            int itemIdx = display.get(i);
            boolean exists = activeEntities.stream()
                    .anyMatch(a -> a.itemIndex == itemIdx && !a.leaving);
            if (!exists) {
                addActive(createPreview(allItems.get(itemIdx), targets.get(i)),
                        itemIdx, targets.get(i), false, true);
            }
        }

        Map<Integer, Point2i> targetByItemIdx = new HashMap<>();
        for (int i = 0; i < display.size(); i++) {
            targetByItemIdx.put(display.get(i), targets.get(i));
        }
        for (ActiveEntity a : activeEntities) {
            if (a.leaving || a.entering) {
                continue;
            }
            Point2i newTarget = targetByItemIdx.get(a.itemIndex);
            if (newTarget != null) {
                a.target = newTarget;
            }
        }
    }

    private static void computeDisplay() {
        display.clear();
        targets.clear();
        int totalItems = allItems.size();
        if (totalItems == 0 || candidates.isEmpty()) {
            return;
        }

        long currentSignature = candidatesSignature();
        boolean shapeChanged = currentSignature != lastSig;
        lastSig = currentSignature;

        candidateByItem.clear();
        Set<Integer> usedCandidates = new HashSet<>();

        Map<String, Integer> itemIdxByKey = new LinkedHashMap<>();
        for (int i = 0; i < allItems.size(); i++) {
            itemIdxByKey.putIfAbsent(allItems.get(i).item().key, i);
        }

        for (Map.Entry<String, Integer> e : pinned.entrySet()) {
            Integer itemIdx = itemIdxByKey.get(e.getKey());
            if (itemIdx == null) {
                continue;
            }
            int anchor = e.getValue();
            if (anchor < 0 || anchor >= candidates.size() || usedCandidates.contains(anchor)) {
                anchor = nearestFree(anchor, usedCandidates);
            }
            candidateByItem.put(itemIdx, anchor);
            usedCandidates.add(anchor);
        }

        ArrayList<Integer> freeCandidates = new ArrayList<>();
        for (int c = 0; c < candidates.size(); c++) {
            if (!usedCandidates.contains(c)) {
                freeCandidates.add(c);
            }
        }
        boolean relayout = scrollOffset != lastScroll || shapeChanged;
        int startIdx = ((scrollOffset % totalItems) + totalItems) % totalItems;
        for (int offset = 0; offset < totalItems && !freeCandidates.isEmpty(); offset++) {
            int itemIdx = (startIdx + offset) % totalItems;
            if (candidateByItem.containsKey(itemIdx)) {
                continue;
            }
            int chosen;
            if (!relayout) {
                Point2i rel = relCell.get(allItems.get(itemIdx).item().key);
                if (rel != null) {
                    int desired = cellIndex(player.blockX() + rel.x, player.blockY() + rel.y);
                    if (desired >= 0 && freeCandidates.contains(desired)) {
                        chosen = desired;
                    } else {
                        chosen = nearestFreeTo(player.blockX() + rel.x, player.blockY() + rel.y, freeCandidates);
                    }
                } else {
                    chosen = freeCandidates.getFirst();
                }
            } else {
                chosen = freeCandidates.getFirst();
            }
            candidateByItem.put(itemIdx, chosen);
            usedCandidates.add(chosen);
            freeCandidates.remove((Integer) chosen);
        }

        ArrayList<Integer> sorted = new ArrayList<>(candidateByItem.keySet());
        sorted.sort(Comparator.comparingInt(candidateByItem::get));
        for (int itemIdx : sorted) {
            display.add(itemIdx);
            targets.add(candidates.get(candidateByItem.get(itemIdx)));
        }

        relCell.clear();
        for (int i = 0; i < display.size(); i++) {
            Point2i target = targets.get(i);
            relCell.put(
                    allItems.get(display.get(i)).item().key,
                    new Point2i(target.x - player.blockX(), target.y - player.blockY()));
        }
        lastScroll = scrollOffset;
    }

    private static int nearestFree(int anchor, Set<Integer> used) {
        if (candidates.isEmpty()) {
            return 0;
        }
        if (anchor < 0) {
            anchor = 0;
        }
        for (int radius = 0; radius < candidates.size(); radius++) {
            for (int sign = 1; sign >= -1; sign -= 2) {
                int idx = anchor + sign * radius;
                if (idx >= 0 && idx < candidates.size() && !used.contains(idx)) {
                    return idx;
                }
            }
        }
        for (int i = 0; i < candidates.size(); i++) {
            if (!used.contains(i)) {
                return i;
            }
        }
        return 0;
    }

    private static int cellIndex(int x, int y) {
        for (int i = 0; i < candidates.size(); i++) {
            Point2i c = candidates.get(i);
            if (c.x == x && c.y == y) {
                return i;
            }
        }
        return -1;
    }

    private static boolean inSafeZone(double wx, double wy) {
        int idx = dragIdx;
        for (int i = 0; i < activeEntities.size(); i++) {
            if (i == idx) {
                continue;
            }
            ActiveEntity a = activeEntities.get(i);
            if (a.leaving || a.entering) {
                continue;
            }
            ItemEntity ent = a.entity;
            double dx = (ent.x() + ent.width() * 0.5) - wx;
            double dy = (ent.y() + ent.height() * 0.5) - wy;
            double dist = Math.hypot(dx, dy);
            if (dist <= 1.0) {
                return true;
            }
        }
        return false;
    }

    private static int nearestFreeTo(int x, int y, ArrayList<Integer> free) {
        int best = free.getFirst();
        long bestDist = Long.MAX_VALUE;
        for (int idx : free) {
            Point2i c = candidates.get(idx);
            long d = (long) (c.x - x) * (c.x - x) + (long) (c.y - y) * (c.y - y);
            if (d < bestDist) {
                bestDist = d;
                best = idx;
            }
        }
        return best;
    }

    private static void updateActive() {
        float dt = Time.delta;
        animationPhase = (animationPhase + dt) % ANIMATION_PERIOD_TICKS;
        if (animationPhase < 0f) {
            animationPhase += ANIMATION_PERIOD_TICKS;
        }

        for (int i = activeEntities.size() - 1; i >= 0; i--) {
            ActiveEntity a = activeEntities.get(i);
            if (a.entity.isDead()) {
                removeActive(i);
                continue;
            }
            if (i == dragIdx && dragMoved) {
                continue;
            }
            a.spawnTicks++;

            Point2i target = a.target;
            boolean leaving = a.leaving;
            boolean entering = a.entering;
            ItemEntity ent = a.entity;

            float speed = leaving ? LEAVE_SPEED : FOLLOW_SPEED;
            float alpha = 1f - (float) Math.exp(-speed * dt);
            double newX = ent.x() + (target.x - ent.x()) * alpha;
            double newY = ent.y() + (target.y - ent.y()) * alpha;
            ent.setPosition(newX, newY);
            ent.updateLastPosition();
            ent.velocity().set(0, 0);
            ent.acceleration().set(0, 0);

            double dist = Math.sqrt((target.x - newX) * (target.x - newX) + (target.y - newY) * (target.y - newY));
            if (leaving && dist < 0.3) {
                ent.remove();
                removeActive(i);
            } else if (entering && dist < 0.1) {
                a.entering = false;
            }
        }
        if (dropAny()) {
            rebuild();
            return;
        }
    }

    private static boolean dropAny() {
        int bestIndex = -1;
        double bestDist = -1;

        for (int i = 0; i < activeEntities.size(); i++) {
            ActiveEntity a = activeEntities.get(i);
            if (a.leaving || a.entering || i == dragIdx || a.spawnTicks < DROP_GRACE_TICKS) {
                continue;
            }

            ItemEntity ent = a.entity;
            double dx = player.x() - ent.x();
            double dy = player.y() - ent.y();
            double dist = Math.hypot(dx, dy);
            if (dist < 1e-4) {
                continue;
            }

            float ratio = (float) (dist / MAX_HOLD_DISTANCE);
            float hold = Math.max(0f, Math.min(1f, 1f - ratio * ratio));
            float radialSpeed = (float) (player.velocity().x * dx + player.velocity().y * dy) / (float) dist;

            if (radialSpeed > hold * MAX_DROP_SPEED && radialSpeed > MIN_DROP_RADIAL_SPEED && dist > bestDist) {
                bestDist = dist;
                bestIndex = i;
            }
        }

        if (bestIndex < 0) {
            return false;
        }

        ActiveEntity best = activeEntities.get(bestIndex);
        ItemEntity ent = best.entity;
        int itemIdx = best.itemIndex;
        ent.preview = false;
        ent.velocity().set(player.velocity().x, player.velocity().y);
        ent.acceleration().set(0, 0);
        ent.selected = false;
        ent.pinned = false;
        removeFromInv(itemIdx);
        removeActive(bestIndex);
        return true;
    }

    private static void removeFromInv(int itemIdx) {
        Point2i slot = allSlots.get(itemIdx);
        player.setItem(slot.x, slot.y, null);
        if (player.itemInHandIdx.equals(slot)) {
            player.resetItemInHand();
        }
    }

    private static void rebuild() {
        lastSig = 0;
        buildItems();
        calcCandidates();
        computeDisplay();
        reassignTargets();
    }

    private static void reassignTargets() {
        Map<String, Integer> activeByKey = new HashMap<>();
        for (int i = 0; i < activeEntities.size(); i++) {
            ActiveEntity a = activeEntities.get(i);
            if (a.leaving) {
                continue;
            }
            activeByKey.put(a.entity.itemStack.item().key, i);
        }

        Map<String, Point2i> targetByKey = new HashMap<>();
        for (int i = 0; i < display.size(); i++) {
            int itemIdx = display.get(i);
            targetByKey.put(allItems.get(itemIdx).item().key, targets.get(i));
        }

        for (ActiveEntity a : activeEntities) {
            if (a.leaving) {
                continue;
            }
            if (!targetByKey.containsKey(a.entity.itemStack.item().key)) {
                a.leaving = true;
                a.entering = false;
                a.target = new Point2i(player.blockX(), player.blockY());
            }
        }

        for (int i = 0; i < display.size(); i++) {
            int itemIdx = display.get(i);
            Point2i target = targets.get(i);
            String key = allItems.get(itemIdx).item().key;
            Integer activeIdx = activeByKey.get(key);
            if (activeIdx != null && !activeEntities.get(activeIdx).leaving) {
                ActiveEntity a = activeEntities.get(activeIdx);
                a.itemIndex = itemIdx;
                a.target = target;
                a.entering = false;
            } else {
                addActive(createPreview(allItems.get(itemIdx), target), itemIdx, target, false, true);
            }
        }
        updateSelection();
    }

    private static int countStacks() {
        int count = 0;
        var items = player.items();
        for (var line : items) {
            for (var stack : line) {
                if (stack != null) {
                    count++;
                }
            }
        }
        return count;
    }

    private static void refresh() {
        clearActive();
        for (int i = 0; i < display.size(); i++) {
            int itemIdx = display.get(i);
            Point2i target = targets.get(i);
            addActive(createPreview(allItems.get(itemIdx), target), itemIdx, target, false, false);
        }
    }

    private static void removeActive(int index) {
        activeEntities.remove(index);
    }

    private static void updateSelection() {
        for (ActiveEntity a : activeEntities) {
            ItemEntity ent = a.entity;
            boolean selected = false;
            if (player.hasItemInHand() && !a.leaving && !a.entering) {
                Point2i slot = allSlots.get(a.itemIndex);
                selected = player.itemInHandIdx.equals(slot);
            }
            ent.selected = selected;
            ent.pinned = pinned.containsKey(allItems.get(a.itemIndex).item().key);
        }
    }

    public static void updateBlocksPreview() {
        var itemInHand = player.getItemInHand();
        if (itemInHand != null && itemInHand.item() instanceof ItemBlock b) {
            Point2i blockPos = input.mouseBlockPos();

            if (!player.hasDraggingItem()) {
                boolean canBuild = getDistanceToMouse() < 8 && world.checkPlaceRules(blockPos.x, blockPos.y, b.block);
                WorldDrawing.addBlockPreview(blockPos.x, blockPos.y, b.block.id, (byte) b.block.maxHp, canBuild);
            }
        }
    }

    public static void drawGrid(int blockX, int blockY) {
        if (!buildGrid || player.hasDraggingItem()) {
            return;
        }
        var itemInHand = player.getItemInHand();
        if (itemInHand != null && itemInHand.item() instanceof ItemBlock) {
            Atlas.Region tex = atlas.get("World/buildGrid");
            float w = toWorld(tex.width());
            float h = toWorld(tex.height());
            StackfulRender.draw(tex,
                    rgba8888(230, 230, 230, 150),
                    blockX - toWorld(243f), blockY - toWorld(244f), w, h);
        }
    }

    private static void calcCandidates() {
        candidates.clear();
        if (player == null || world == null) {
            return;
        }

        int centerX = player.blockX() + HORIZONTAL_OFFSET;
        int playerBY = player.blockY();
        Set<Point2i> playerOccupied = occupiedBlocks();

        Map<Point2i, Integer> pathDistances = new LinkedHashMap<>();
        Queue<Point2i> queue = new ArrayDeque<>();

        Point2i start = new Point2i(centerX, playerBY);
        queue.add(start);
        pathDistances.put(start, 0);

        while (!queue.isEmpty()) {
            Point2i p = queue.poll();
            int currentDist = pathDistances.get(p);

            for (int[] dir : DIRS) {
                int nx = p.x + dir[0];
                int ny = p.y + dir[1];

                if (nx < 0 || nx >= world.sizeX || ny < 0 || ny >= world.sizeY) {
                    continue;
                }
                if (Math.abs(nx - centerX) > SEARCH_RADIUS || Math.abs(ny - playerBY) > SEARCH_RADIUS) {
                    continue;
                }

                Point2i np = new Point2i(nx, ny);

                if (isFree(nx, ny) && !pathDistances.containsKey(np)) {
                    pathDistances.put(np, currentDist + 1);
                    queue.add(np);
                }
            }
        }

        int fromBX = player.blockX();
        int fromBY = player.blockY();
        for (Point2i p : pathDistances.keySet()) {
            if (playerOccupied.contains(p) || !lineOfSight(fromBX, fromBY, p.x, p.y)
                    || Math.abs(p.x - fromBX) > MAX_PLACEMENT_HORIZONTAL
                    || Math.abs(p.y - fromBY) > MAX_PLACEMENT_VERTICAL) {
                continue;
            }
            candidates.add(p);
        }

        int targetWidth = VISIBLE_COLUMNS;
        int targetHeight = VISIBLE_ROWS;
        int startY = playerBY + START_Y_OFFSET;

        candidates.sort((a, b) -> {
            int idealX_A = Math.abs(a.x - centerX);
            int idealY_A = a.y >= startY ? (a.y - startY) : (startY - a.y) * 2;

            int idealX_B = Math.abs(b.x - centerX);
            int idealY_B = b.y >= startY ? (b.y - startY) : (startY - b.y) * 2;

            int penaltyA = Math.max(0, idealX_A - targetWidth / 2) + Math.max(0, idealY_A - targetHeight);
            int penaltyB = Math.max(0, idealX_B - targetWidth / 2) + Math.max(0, idealY_B - targetHeight);

            if (penaltyA != penaltyB) {
                return Integer.compare(penaltyA, penaltyB);
            }
            if (a.y != b.y) {
                return Integer.compare(a.y, b.y);
            }
            return Integer.compare(a.x, b.x);
        });
    }

    private static Set<Point2i> occupiedBlocks() {
        return Collections.singleton(new Point2i(player.blockX(), player.blockY()));
    }

    private static boolean isFree(int x, int y) {
        return x >= 0 && x < world.sizeX && y >= 0 && y < world.sizeY
                && !world.isBlockType(x, y, Block.Type.SOLID);
    }

    private static boolean lineOfSight(int fromX, int fromY, int toX, int toY) {
        return ItemEntity.raycastTo(fromX, fromY, toX, toY, (x, y) -> !isFree(x, y));
    }

    private static long candidatesSignature() {
        if (candidates.isEmpty()) {
            return 0;
        }
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (Point2i p : candidates) {
            if (p.x < minX) {
                minX = p.x;
            }
            if (p.x > maxX) {
                maxX = p.x;
            }
            if (p.y < minY) {
                minY = p.y;
            }
            if (p.y > maxY) {
                maxY = p.y;
            }
        }
        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        return ((long) width << 32) | ((long) height << 16) | (candidates.size() & 0xFFFF);
    }

    private static int entityAtMouse() {
        var mouseWorldPos = input.mouseWorldPos();
        double mx = mouseWorldPos.x;
        double my = mouseWorldPos.y;

        for (ActiveEntity a : activeEntities) {
            ItemEntity ent = a.entity;
            if (ent.isDead() || a.leaving) {
                continue;
            }

            double rx = Physics.applyAlpha(ent.lastX(), ent.x());
            double ry = Physics.applyAlpha(ent.lastY(), ent.y()) + ent.bobOffset();
            float w = ent.width();
            float h = ent.height();

            if (mx >= rx && mx <= rx + w && my >= ry && my <= ry + h) {
                return activeEntities.indexOf(a);
            }
        }
        return -1;
    }

    private static void previewClick(int i) {
        if (i < 0) {
            return;
        }
        Point2i slot = allSlots.get(activeItemIdx(i));
        if (player.itemInHandIdx.equals(slot)) {
            player.resetItemInHand();
        } else {
            player.itemInHandIdx.set(slot);
        }
    }

    private static int entityAtCell(int bx, int by) {
        for (int i = 0; i < activeEntities.size(); i++) {
            if (i == dragIdx) {
                continue;
            }
            ActiveEntity a = activeEntities.get(i);
            if (a.leaving || a.entering) {
                continue;
            }
            if (a.target.x == bx && a.target.y == by) {
                return i;
            }
        }
        return -1;
    }

    private static void startDrag(int entityIndex) {
        dragIdx = entityIndex;
        dragMoved = false;
        dragStartX = input.mousePos().x;
        dragStartY = input.mousePos().y;
        Point2i t = activeEntities.get(entityIndex).target;
        dragFrom.set(t.x, t.y);
    }

    private static void dragMove() {
        int idx = dragIdx;
        if (idx < 0 || idx >= activeEntities.size()) {
            resetDrag();
            return;
        }
        var mp = input.mousePos();
        float dx = mp.x - dragStartX;
        float dy = mp.y - dragStartY;
        if (!dragMoved && dx * dx + dy * dy > DRAG_START_DISTANCE * DRAG_START_DISTANCE) {
            dragMoved = true;
            ActiveEntity a = activeEntities.get(idx);
            Point2i slot = allSlots.get(a.itemIndex);
            player.draggingItemIdx.set(slot.x, slot.y);
        }
        if (dragMoved) {
            var mw = input.mouseWorldPos();
            ItemEntity ent = activeEntities.get(idx).entity;
            ent.setPosition(mw.x, mw.y);
            ent.updateLastPosition();
        }
    }

    private static void endDrag() {
        int idx = dragIdx;
        if (idx < 0 || idx >= activeEntities.size()) {
            resetDrag();
            return;
        }
        if (!dragMoved) {
            resetDrag();
            return;
        }

        int fromItemIdx = activeEntities.get(idx).itemIndex;
        Point2i mbp = input.mouseBlockPos();

        int toEntity = entityAtCell(mbp.x, mbp.y);
        if (toEntity >= 0) {
            swap(fromItemIdx, activeEntities.get(toEntity).itemIndex, toEntity);
            resetDrag();
            return;
        }

        var mw = input.mouseWorldPos();
        if (inSafeZone(mw.x, mw.y)) {
            resetDrag();
            return;
        }

        drop(fromItemIdx, idx);
        resetDrag();
    }

    private static void resetDrag() {
        dragIdx = -1;
        dragMoved = false;
        player.resetDraggingItem();
    }

    private static void swap(int fromItemIdx, int toItemIdx, int toEntIdx) {
        if (fromItemIdx == toItemIdx) {
            return;
        }

        Point2i fromSlot = allSlots.get(fromItemIdx);
        Point2i toSlot = allSlots.get(toItemIdx);
        var fromStack = allItems.get(fromItemIdx);
        var toStack = allItems.get(toItemIdx);

        player.setItem(fromSlot, toStack);
        player.setItem(toSlot, fromStack);

        if (player.itemInHandIdx.equals(fromSlot)) {
            player.itemInHandIdx.set(toSlot.x, toSlot.y);
        } else if (player.itemInHandIdx.equals(toSlot)) {
            player.itemInHandIdx.set(fromSlot.x, fromSlot.y);
        }

        Point2i toCell = activeEntities.get(toEntIdx).target;
        relCell.put(fromStack.item().key,
                new Point2i(toCell.x - player.blockX(), toCell.y - player.blockY()));
        relCell.put(toStack.item().key,
                new Point2i(dragFrom.x - player.blockX(), dragFrom.y - player.blockY()));

        updatePin(fromStack.item().key, toCell);
        updatePin(toStack.item().key, dragFrom);

        rebuildAfterSwap();
    }

    private static void updatePin(String key, Point2i cell) {
        Integer pinnedAnchor = pinned.get(key);
        if (pinnedAnchor == null) {
            return;
        }
        int newAnchor = cellIndex(cell.x, cell.y);
        if (newAnchor >= 0) {
            pinned.put(key, newAnchor);
        } else {
            pinned.remove(key);
        }
    }

    private static void rebuildAfterSwap() {
        buildItems();
        calcCandidates();
        computeDisplay();
        refresh();
    }

    private static void drop(int fromItemIdx, int fromEntIdx) {
        ItemEntity ent = activeEntities.get(fromEntIdx).entity;
        Point2i slot = allSlots.get(fromItemIdx);
        var stack = allItems.get(fromItemIdx);

        player.setItem(slot.x, slot.y, null);
        if (player.itemInHandIdx.equals(slot)) {
            player.resetItemInHand();
        }
        pinned.remove(stack.item().key);

        var mw = input.mouseWorldPos();
        ent.preview = false;
        ent.setPosition(mw.x, mw.y);
        ent.updateLastPosition();
        ent.velocity().set(0, 0);
        ent.acceleration().set(0, 0);
        ent.selected = false;
        ent.pinned = false;

        removeActive(fromEntIdx);
        rebuild();
    }

    private static void togglePin() {
        int i = entityAtMouse();
        if (i < 0) {
            return;
        }
        int itemIdx = activeItemIdx(i);
        String key = allItems.get(itemIdx).item().key;
        if (pinned.containsKey(key)) {
            pinned.remove(key);
        } else {
            Integer candIdx = candidateByItem.get(itemIdx);
            if (candIdx == null) {
                candIdx = 0;
            }
            pinned.put(key, candIdx);
        }
        reconcile();
        updateActive();
        updateSelection();
    }

    public static void draw() { }
    public static @Nullable Point2i getFocusedItemIdx() { return null; }
    public static boolean addItem(Item item) {
        return player.addItem(new ItemStack(item)) != InventoryComponent.TransitionResult.FAILED;
    }
    public static boolean addItemStack(ItemStack item) {
        return player.addItem(item) != InventoryComponent.TransitionResult.FAILED;
    }

    private static int activeItemIdx(int index) {
        return activeEntities.get(index).itemIndex;
    }

    private static class ActiveEntity {
        final ItemEntity entity;
        int itemIndex;
        Point2i target;
        boolean leaving;
        boolean entering;
        int spawnTicks;

        ActiveEntity(ItemEntity entity, int itemIndex, Point2i target, boolean leaving, boolean entering, int spawnTicks) {
            this.entity = entity;
            this.itemIndex = itemIndex;
            this.target = target;
            this.leaving = leaving;
            this.entering = entering;
            this.spawnTicks = spawnTicks;
        }
    }
}
