package com.gridexploration;

import java.util.*;

public class ExplorerAgent implements Runnable {
    private String agentID;
    private int currentX;
    private int currentY;
    private List<Coordinate> currentBfsPath;

    public ExplorerAgent(String agentID, int startX, int startY) {
        this.agentID = agentID;
        this.currentX = startX;
        this.currentY = startY;
        this.currentBfsPath = new ArrayList<>();
    }

    public String getAgentID() { return agentID; }
    public int getCurrentX() { return currentX; }
    public int getCurrentY() { return currentY; }

    @Override
    public void run() {
        Blackboard blackboard = Blackboard.getInstance();
        
        Cell initialCell = blackboard.getCellAt(currentX, currentY);
        if (initialCell != null && initialCell.getType() == CellType.TARGET) {
            blackboard.reportTarget(currentX, currentY);
        }
        blackboard.markVisited(currentX, currentY);

        while (!blackboard.isSimulationComplete()) {
            exploreStep();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("Agent " + agentID + " terminating.");
    }

    private void exploreStep() {
        Coordinate neighbor = getUnvisitedNeighbor();
        
        if (neighbor != null) {
            currentBfsPath.clear();
            attemptMove(neighbor.x, neighbor.y);
        } else {
            if (currentBfsPath.isEmpty()) {
                currentBfsPath = computeBFS();
            }
            
            if (!currentBfsPath.isEmpty()) {
                Coordinate nextStep = currentBfsPath.remove(0);
                boolean success = attemptMove(nextStep.x, nextStep.y);
                if (!success) {
                    currentBfsPath.clear();
                }
            }
        }
    }

    private Coordinate getUnvisitedNeighbor() {
        Blackboard bb = Blackboard.getInstance();
        Set<Coordinate> visited = bb.getVisitedCellsSnapshot();
        Set<Coordinate> walls = bb.getWallCellsSnapshot();
        
        int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        
        for (int[] d : dirs) {
            int nx = currentX + d[0];
            int ny = currentY + d[1];
            Coordinate nc = new Coordinate(nx, ny);
            
            if (nx >= 0 && nx < bb.getGridWidth() && ny >= 0 && ny < bb.getGridHeight()) {
                if (!visited.contains(nc) && !walls.contains(nc)) {
                    return nc;
                }
            }
        }
        return null;
    }

    private boolean attemptMove(int targetX, int targetY) {
        Blackboard bb = Blackboard.getInstance();
        Cell targetCell = bb.getCellAt(targetX, targetY);
        
        if (targetCell == null) return false;
        
        if (targetCell.getType() == CellType.WALL) {
            bb.markWall(targetX, targetY);
            return false;
        } else {
            currentX = targetX;
            currentY = targetY;
            bb.markVisited(currentX, currentY);
            
            if (targetCell.getType() == CellType.TARGET) {
                bb.reportTarget(currentX, currentY);
            }
            return true;
        }
    }

    private List<Coordinate> computeBFS() {
        Blackboard bb = Blackboard.getInstance();
        Set<Coordinate> visitedGlobal = bb.getVisitedCellsSnapshot();
        Set<Coordinate> wallsGlobal = bb.getWallCellsSnapshot();
        
        Queue<List<Coordinate>> queue = new LinkedList<>();
        Set<Coordinate> bfsVisited = new HashSet<>();
        
        Coordinate start = new Coordinate(currentX, currentY);
        bfsVisited.add(start);
        
        int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        
        for (int[] d : dirs) {
            int nx = currentX + d[0];
            int ny = currentY + d[1];
            Coordinate neighbor = new Coordinate(nx, ny);
            
            if (isValid(nx, ny, bb.getGridWidth(), bb.getGridHeight()) && !wallsGlobal.contains(neighbor)) {
                List<Coordinate> path = new ArrayList<>();
                path.add(neighbor);
                queue.add(path);
                bfsVisited.add(neighbor);
            }
        }
        
        while (!queue.isEmpty()) {
            List<Coordinate> path = queue.poll();
            Coordinate last = path.get(path.size() - 1);
            
            if (!visitedGlobal.contains(last) && !wallsGlobal.contains(last)) {
                return path;
            }
            
            for (int[] d : dirs) {
                int nx = last.x + d[0];
                int ny = last.y + d[1];
                Coordinate neighbor = new Coordinate(nx, ny);
                
                if (isValid(nx, ny, bb.getGridWidth(), bb.getGridHeight()) 
                    && !wallsGlobal.contains(neighbor) 
                    && !bfsVisited.contains(neighbor)) {
                    
                    bfsVisited.add(neighbor);
                    List<Coordinate> newPath = new ArrayList<>(path);
                    newPath.add(neighbor);
                    queue.add(newPath);
                }
            }
        }
        
        return new ArrayList<>();
    }
    
    private boolean isValid(int x, int y, int width, int height) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }
}
