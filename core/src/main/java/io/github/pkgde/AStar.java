package io.github.pkgde;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Grid-based A* pathfinding. A single instance is shared by all enemies.
 * The grid is built once from the map's collision rectangles.
 */
public class AStar {

    private final Node[][] grid;
    private final int cols, rows;
    private final float cellSize;

    /** Max nodes to explore per findPath call — prevents frame stalls on huge/impossible paths. */
    private static final int MAX_ITERATIONS = 2000;

    public AStar(float mapWidth, float mapHeight, float cellSize, List<Rectangle> boundaries) {
        this.cellSize = cellSize;

        this.cols = Math.min(1000, (int) Math.ceil(mapWidth / cellSize));
        this.rows = Math.min(1000, (int) Math.ceil(mapHeight / cellSize));

        grid = new Node[cols][rows];

        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                grid[x][y] = new Node(x, y);

                // Slightly inset the test rect so touching-edge walls don't falsely block
                Rectangle cellRect = new Rectangle(
                    x * cellSize + 1f, y * cellSize + 1f,
                    cellSize - 2f, cellSize - 2f
                );

                if (boundaries != null) {
                    for (Rectangle wall : boundaries) {
                        if (cellRect.overlaps(wall)) {
                            grid[x][y].walkable = false;
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Find a path between two world-space points (both should be hitbox centers).
     * Returns a list of world-space cell-center Vector2 waypoints, or an empty list if no path.
     * The path is automatically smoothed to remove redundant waypoints.
     */
    public List<Vector2> findPath(Vector2 startWorld, Vector2 targetWorld) {
        // Reset all nodes before each search
        resetNodes();

        int startX = worldToGridX(startWorld.x);
        int startY = worldToGridY(startWorld.y);
        int targetX = worldToGridX(targetWorld.x);
        int targetY = worldToGridY(targetWorld.y);

        Node startNode = grid[startX][startY];
        Node targetNode = grid[targetX][targetY];

        // If start is blocked, find nearest walkable
        if (!startNode.walkable) {
            startNode = findNearestWalkable(startX, startY);
            if (startNode == null) return new ArrayList<>();
        }

        // If target is blocked, find nearest walkable
        if (!targetNode.walkable) {
            targetNode = findNearestWalkable(targetX, targetY);
            if (targetNode == null) return new ArrayList<>();
        }

        // Same cell — no path needed
        if (startNode == targetNode) return new ArrayList<>();

        ArrayList<Node> openSet = new ArrayList<>();
        ArrayList<Node> closedSet = new ArrayList<>();
        int iterations = 0;

        startNode.gCost = 0;
        startNode.hCost = getDistance(startNode, targetNode);
        openSet.add(startNode);

        while (!openSet.isEmpty() && iterations < MAX_ITERATIONS) {
            iterations++;

            // Find node with lowest fCost
            Node current = openSet.get(0);
            for (int i = 1; i < openSet.size(); i++) {
                Node candidate = openSet.get(i);
                if (candidate.getFCost() < current.getFCost() ||
                    (candidate.getFCost() == current.getFCost() && candidate.hCost < current.hCost)) {
                    current = candidate;
                }
            }

            openSet.remove(current);
            closedSet.add(current);

            // Reached the target
            if (current == targetNode) {
                return smoothPath(retracePath(startNode, targetNode));
            }

            for (Node neighbor : getNeighbors(current)) {
                if (!neighbor.walkable || closedSet.contains(neighbor)) continue;

                float newG = current.gCost + getDistance(current, neighbor);
                if (newG < neighbor.gCost) {
                    neighbor.gCost = newG;
                    neighbor.hCost = getDistance(neighbor, targetNode);
                    neighbor.parent = current;

                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor);
                    }
                }
            }
        }

        return new ArrayList<>(); // No path found
    }

    /**
     * Checks if a grid cell at the given world position is walkable.
     */
    public boolean isWalkable(float worldX, float worldY) {
        int gx = worldToGridX(worldX);
        int gy = worldToGridY(worldY);
        return grid[gx][gy].walkable;
    }

    // ===== Internal helpers =====

    private void resetNodes() {
        for (int x = 0; x < cols; x++) {
            for (int y = 0; y < rows; y++) {
                grid[x][y].reset();
            }
        }
    }

    private int worldToGridX(float worldX) {
        return Math.max(0, Math.min(cols - 1, (int) (worldX / cellSize)));
    }

    private int worldToGridY(float worldY) {
        return Math.max(0, Math.min(rows - 1, (int) (worldY / cellSize)));
    }

    private Node findNearestWalkable(int cx, int cy) {
        for (int r = 1; r <= 5; r++) {
            for (int x = cx - r; x <= cx + r; x++) {
                for (int y = cy - r; y <= cy + r; y++) {
                    if (x >= 0 && x < cols && y >= 0 && y < rows && grid[x][y].walkable) {
                        return grid[x][y];
                    }
                }
            }
        }
        return null;
    }

    private List<Node> getNeighbors(Node node) {
        List<Node> neighbors = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int nx = node.gridX + dx;
                int ny = node.gridY + dy;
                if (nx < 0 || nx >= cols || ny < 0 || ny >= rows) continue;

                // Block diagonal movement if either adjacent cardinal is a wall (prevent corner-cutting)
                if (Math.abs(dx) == 1 && Math.abs(dy) == 1) {
                    if (!grid[node.gridX + dx][node.gridY].walkable ||
                        !grid[node.gridX][node.gridY + dy].walkable) {
                        continue;
                    }
                }

                neighbors.add(grid[nx][ny]);
            }
        }
        return neighbors;
    }

    /** Octile distance heuristic (diagonal = 14, cardinal = 10). */
    private float getDistance(Node a, Node b) {
        int dx = Math.abs(a.gridX - b.gridX);
        int dy = Math.abs(a.gridY - b.gridY);
        return (dx > dy) ? 14f * dy + 10f * (dx - dy) : 14f * dx + 10f * (dy - dx);
    }

    /** Retrace from target back to start and return cell-center world positions. */
    private List<Vector2> retracePath(Node startNode, Node endNode) {
        List<Node> path = new ArrayList<>();
        Node current = endNode;
        while (current != startNode && current != null) {
            path.add(current);
            current = current.parent;
        }
        Collections.reverse(path);

        List<Vector2> worldPath = new ArrayList<>();
        for (Node node : path) {
            // Cell center in world space
            float wx = node.gridX * cellSize + cellSize / 2f;
            float wy = node.gridY * cellSize + cellSize / 2f;
            worldPath.add(new Vector2(wx, wy));
        }
        return worldPath;
    }

    /**
     * Smooth a path by removing redundant intermediate waypoints.
     * Uses Bresenham-style grid line-of-sight checks: if you can walk in a straight
     * line from waypoint A to waypoint C (all grid cells along the line are walkable),
     * then waypoint B is unnecessary and is removed.
     * This eliminates the stair-step zigzag that raw A* produces on a grid.
     */
    private List<Vector2> smoothPath(List<Vector2> path) {
        if (path.size() <= 2) return path;

        List<Vector2> smoothed = new ArrayList<>();
        smoothed.add(path.get(0));

        int current = 0;
        while (current < path.size() - 1) {
            // Try to skip as far ahead as possible while maintaining line-of-sight
            int farthest = current + 1;
            for (int check = path.size() - 1; check > current + 1; check--) {
                if (hasLineOfSight(path.get(current), path.get(check))) {
                    farthest = check;
                    break;
                }
            }
            smoothed.add(path.get(farthest));
            current = farthest;
        }

        return smoothed;
    }

    /**
     * Checks grid-based line-of-sight between two world-space points using
     * Bresenham's line algorithm on the grid. Returns true if every cell
     * along the line is walkable.
     */
    private boolean hasLineOfSight(Vector2 from, Vector2 to) {
        int x0 = worldToGridX(from.x);
        int y0 = worldToGridY(from.y);
        int x1 = worldToGridX(to.x);
        int y1 = worldToGridY(to.y);

        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            if (x0 < 0 || x0 >= cols || y0 < 0 || y0 >= rows) return false;
            if (!grid[x0][y0].walkable) return false;
            if (x0 == x1 && y0 == y1) break;

            int e2 = 2 * err;
            // For diagonal steps, also check the two cardinal neighbors to prevent
            // line-of-sight through wall corners
            if (e2 > -dy && e2 < dx) {
                // Diagonal step — check both adjacent cardinals
                if (!grid[x0 + sx][y0].walkable || !grid[x0][y0 + sy].walkable) return false;
            }
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx)  { err += dx; y0 += sy; }
        }
        return true;
    }

    public float getCellSize() {
        return cellSize;
    }
}
