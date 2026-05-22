package core.util;

import core.content.entity.HitboxComponent;
import core.math.Rectangle;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.function.Consumer;

public final class QuadTree<T extends HitboxComponent> {

    private static final int MAX_OBJECTS_PER_NODE = 4;

    private final Rectangle tmp = new Rectangle();
    private final int objectsPerNode;

    public Rectangle bounds;
    public Nodes<T> objects = new Nodes<>();
    public QuadTree<T> botLeft, botRight, topLeft, topRight;
    public boolean leaf = true;
    public int totalObjects;

    public static final class Nodes<T> extends ObjectArrayList<T> {
        void exchange(int i) {
            size--;
            a[i] = a[size];
            a[size] = null;
        }
    }

    public QuadTree(Rectangle bounds) {
        this(bounds, MAX_OBJECTS_PER_NODE);
    }

    public QuadTree(Rectangle bounds, int objectsPerNode) {
        this.bounds = bounds;
        this.objectsPerNode = objectsPerNode;
    }

    private void split() {
        if (!leaf) {
            return;
        }

        float subW = bounds.width / 2;
        float subH = bounds.height / 2;

        if (botLeft == null) {
            botLeft = newChild(new Rectangle(bounds.x, bounds.y, subW, subH));
            botRight = newChild(new Rectangle(bounds.x + subW, bounds.y, subW, subH));
            topLeft = newChild(new Rectangle(bounds.x, bounds.y + subH, subW, subH));
            topRight = newChild(new Rectangle(bounds.x + subW, bounds.y + subH, subW, subH));
        }
        leaf = false;

        for (int i = 0; i < objects.size(); i++) {
            T object = objects.get(i);
            getHitbox(object);
            var child = getFittingChild(tmp);
            if (child != null) {
                child.insert(object);
                objects.exchange(i);
                i--;
            }
        }
    }

    private void unsplit() {
        if (leaf) {
            return;
        }
        objects.addAll(botLeft.objects);
        objects.addAll(botRight.objects);
        objects.addAll(topLeft.objects);
        objects.addAll(topRight.objects);
        botLeft.clear();
        botRight.clear();
        topLeft.clear();
        topRight.clear();
        leaf = true;
    }

    public void insert(T obj) {
        getHitbox(obj);
        if (!bounds.overlaps(tmp)) {
            return;
        }

        totalObjects++;

        if (leaf && objects.size() + 1 > objectsPerNode) {
            split();
        }

        if (leaf) {
            objects.add(obj);
        } else {
            getHitbox(obj);
            var child = getFittingChild(tmp);
            if (child != null) {
                child.insert(obj);
            } else {
                objects.add(obj);
            }
        }
    }

    public boolean remove(T obj) {
        boolean result;
        if (leaf) {
            result = objects.remove(obj);
        } else {
            getHitbox(obj);
            var child = getFittingChild(tmp);
            if (child != null) {
                result = child.remove(obj);
            } else {
                result = objects.remove(obj);
            }

            if (totalObjects <= objectsPerNode) {
                unsplit();
            }
        }
        if (result) {
            totalObjects--;
        }
        return result;
    }

    public void clear() {
        objects.clear();
        totalObjects = 0;
        if (!leaf) {
            topLeft.clear();
            topRight.clear();
            botLeft.clear();
            botRight.clear();
        }
        leaf = true;
    }

    private QuadTree<T> getFittingChild(Rectangle boundingBox) {
        float verticalMidpoint = bounds.x + (bounds.width / 2);
        float horizontalMidpoint = bounds.y + (bounds.height / 2);

        boolean topQuadrant = boundingBox.y > horizontalMidpoint;
        boolean bottomQuadrant = boundingBox.y < horizontalMidpoint && (boundingBox.y + boundingBox.height) < horizontalMidpoint;

        if (boundingBox.x < verticalMidpoint && boundingBox.x + boundingBox.width < verticalMidpoint) {
            if (topQuadrant) {
                return topLeft;
            } else if (bottomQuadrant) {
                return botLeft;
            }
        } else if (boundingBox.x > verticalMidpoint) {
            if (topQuadrant) {
                return topRight;
            } else if (bottomQuadrant) {
                return botRight;
            }
        }
        return null;
    }

    public void intersect(T object, Consumer<T> out) {
        getHitbox(object);
        intersect(tmp.x, tmp.y, tmp.width, tmp.height, out);
    }

    public void intersect(float x, float y, float width, float height, Consumer<T> out) {
        if (!leaf) {
            if (topLeft.bounds.overlaps(x, y, width, height)) {
                topLeft.intersect(x, y, width, height, out);
            }
            if (topRight.bounds.overlaps(x, y, width, height)) {
                topRight.intersect(x, y, width, height, out);
            }
            if (botLeft.bounds.overlaps(x, y, width, height)) {
                botLeft.intersect(x, y, width, height, out);
            }
            if (botRight.bounds.overlaps(x, y, width, height)) {
                botRight.intersect(x, y, width, height, out);
            }
        }

        for (T object : objects) {
            getHitbox(object);
            if (tmp.overlaps(x, y, width, height)) {
                out.accept(object);
            }
        }
    }

    public void eachNode(float x, float y, float width, float height, Consumer<QuadTree<T>> out) {
        if (!leaf) {
            if (topLeft.bounds.overlaps(x, y, width, height)) {
                out.accept(topLeft);
            }
            if (topRight.bounds.overlaps(x, y, width, height)) {
                out.accept(topRight);
            }
            if (botLeft.bounds.overlaps(x, y, width, height)) {
                out.accept(botLeft);
            }
            if (botRight.bounds.overlaps(x, y, width, height)) {
                out.accept(botRight);
            }
        }
        if (bounds.overlaps(x, y, width, height)) {
            out.accept(this);
        }
    }

    public boolean any(float x, float y, float width, float height) {
        if (!leaf) {
            if (topLeft.bounds.overlaps(x, y, width, height) && topLeft.any(x, y, width, height)) {
                return true;
            }
            if (topRight.bounds.overlaps(x, y, width, height) && topRight.any(x, y, width, height)) {
                return true;
            }
            if (botLeft.bounds.overlaps(x, y, width, height) && botLeft.any(x, y, width, height)) {
                return true;
            }
            if (botRight.bounds.overlaps(x, y, width, height) && botRight.any(x, y, width, height)) {
                return true;
            }
        }

        for (T item : objects) {
            getHitbox(item);
            if (tmp.overlaps(x, y, width, height)) {
                return true;
            }
        }
        return false;
    }

    private QuadTree<T> newChild(Rectangle Rectangle) {
        return new QuadTree<>(Rectangle, objectsPerNode);
    }

    private void getHitbox(T t) {
        t.getHitboxTo(tmp);
    }
}
