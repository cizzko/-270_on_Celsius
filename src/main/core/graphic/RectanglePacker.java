package core.graphic;

import java.util.ArrayList;

public final class RectanglePacker {
    private final ArrayList<RectanglePacker.Region> regions;

    public int w, h;

    public RectanglePacker(int w, int h) {
        this(0, w, h);
    }

    public RectanglePacker(int regionCount, int w, int h) {
        this.regions = new ArrayList<>(regionCount);
        resize(w, h);
    }

    private int rectFits(int idx, int w, int h) {
        var rg = regions.get(idx);
        if (rg.x + w > this.w) {
            return -1;
        }
        int y = rg.y;
        int spaceLeft = w;
        int i = idx;
        while (spaceLeft > 0) {
            if (i == regions.size()) {
                return -1;
            }
            var th = regions.get(i);
            y = Math.max(y, th.y);
            if (y + h > this.h) {
                return -1;
            }
            spaceLeft -= th.w;
            i++;
        }
        return y;
    }

    private void addRegion(int idx, int x, int y, int w, int h) {
        regions.add(idx, new Region(x, y + h, w));
        for (int i = idx + 1; i < regions.size(); ) {
            var prev = regions.get(i - 1);
            var curr = regions.get(i);
            if (curr.x < prev.x + prev.w) {
                int shrink = prev.x - curr.x + prev.w;
                curr.x += shrink;
                curr.w -= shrink;
                if (curr.w <= 0) {
                    regions.remove(i);
                    i--;
                } else {
                    break;
                }
            } else {
                break;
            }
            i++;
        }

        int i = 0;
        while (i + 1 < regions.size()) {
            var next = regions.get(i + 1);
            var curr = regions.get(i);
            if (curr.y == next.y) {
                curr.w += next.w;
                regions.remove(i + 1);
                i--;
            }
            i++;
        }
    }

    public void resize(int w, int h) {
        regions.add(new Region(this.w, 0, w - this.w));
        this.w = w;
        this.h = h;
    }

    public Position pack(int w, int h) {
        int bestIDX = -1, bestX = -1, bestY = -1;
        int bestH = this.h, bestW = this.w;
        for (int i = 0; i < regions.size(); i++) {
            int y = rectFits(i, w, h);
            if (y != -1) {
                var rg = regions.get(i);
                if (y + h < bestH && rg.w < bestW) {
                    bestIDX = i;
                    bestW = rg.w;
                    bestH = y + h;
                    bestX = rg.x;
                    bestY = y;
                }
            }
        }

        if (bestIDX != -1) {
            addRegion(bestIDX, bestX, bestY, w, h);
            return new Position(bestX, bestY);
        }
        return new Position(-1, -1);
    }

    private static class Region {
        public int x, y, w;

        Region(int x, int y, int w) {
            this.x = x;
            this.y = y;
            this.w = w;
        }
    }

    public record Position(int x, int y) {
        public boolean isInvalid() {
            return x < 0 || y < 0;
        }
    }
}
