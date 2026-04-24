package io.github.pkgde;

/**
 * Represents a single cell in the A* pathfinding grid.
 */
public class Node {
    public int gridX;
    public int gridY;

    public float gCost;
    public float hCost;
    public Node parent;
    public boolean walkable;

    public Node(int gridX, int gridY) {
        this.gridX = gridX;
        this.gridY = gridY;
        this.walkable = true;
    }

    public float getFCost() {
        return gCost + hCost;
    }

    /** Resets transient pathfinding state so this node can be reused across findPath() calls. */
    public void reset() {
        gCost = Float.MAX_VALUE;
        hCost = 0f;
        parent = null;
    }
}
