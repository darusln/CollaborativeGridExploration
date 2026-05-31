package com.gridexploration;

import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

public class Blackboard {
    private static Blackboard instance;

    private Cell[][] cellGrid;
    private Set<Coordinate> visitedCells;
    private Set<Coordinate> wallCells;
    private List<Coordinate> discoveredTargets;
    private int discoveryCount;
    private int totalTargets;

    private Blackboard() {
        visitedCells = new HashSet<>();
        wallCells = new HashSet<>();
        discoveredTargets = new ArrayList<>();
        discoveryCount = 0;
    }

    public static synchronized Blackboard getInstance() {
        if (instance == null) {
            instance = new Blackboard();
        }
        return instance;
    }

    public void init(Cell[][] grid, int totalTargets) {
        this.cellGrid = grid;
        this.totalTargets = totalTargets;
    }

    public Cell getCellAt(int x, int y) {
        if (x < 0 || y < 0 || x >= cellGrid.length || y >= cellGrid[0].length) {
            return null;
        }
        return cellGrid[x][y];
    }

    public synchronized Set<Coordinate> getVisitedCellsSnapshot() {
        return new HashSet<>(visitedCells);
    }

    public synchronized Set<Coordinate> getWallCellsSnapshot() {
        return new HashSet<>(wallCells);
    }
    
    public synchronized List<Coordinate> getDiscoveredTargetsSnapshot() {
        return new ArrayList<>(discoveredTargets);
    }

    public synchronized void markVisited(int x, int y) {
        visitedCells.add(new Coordinate(x, y));
    }

    public synchronized void markWall(int x, int y) {
        wallCells.add(new Coordinate(x, y));
    }

    public synchronized void reportTarget(int x, int y) {
        Coordinate c = new Coordinate(x, y);
        if (!discoveredTargets.contains(c)) {
            discoveredTargets.add(c);
            discoveryCount++;
            System.out.println("Target discovered at " + c + "! Total found: " + discoveryCount + "/" + totalTargets);
        }
    }

    public synchronized boolean isSimulationComplete() {
        return discoveryCount >= totalTargets;
    }
    
    public int getGridWidth() {
        return cellGrid != null ? cellGrid.length : 0;
    }
    
    public int getGridHeight() {
        return cellGrid != null && cellGrid.length > 0 ? cellGrid[0].length : 0;
    }
}
