package com.gridexploration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Blackboard {
    private static Blackboard instance; // singleton

    private Cell[][] cellGrid;
    private final Set<Coordinate> visitedCells;
    private final Set<Coordinate> wallCells;
    private final List<Coordinate> discoveredTargets;
    private int discoveryCount;
    private int totalTargets;
    private int exhaustedAgents;
    private int totalAgents;
    private Blackboard() {
        visitedCells = new HashSet<>();
        wallCells = new HashSet<>();
        discoveredTargets = new ArrayList<>();
        discoveryCount = 0;
        exhaustedAgents = 0;
        totalAgents = 0;
    }

    public static synchronized Blackboard getInstance() {
        if (instance == null) {
            instance = new Blackboard();
        }
        return instance;
    }

     // init the Blackboard with the grid and simulation param

    public synchronized void init(Cell[][] grid, int totalTargets, int totalAgents) {
        this.cellGrid    = grid;
        this.totalTargets = totalTargets;
        this.totalAgents  = totalAgents;
        visitedCells.clear();
        wallCells.clear();
        discoveredTargets.clear();
        discoveryCount  = 0;
        exhaustedAgents = 0;
    }

    // environment
    public synchronized Cell getCellAt(int x, int y) {
        if (cellGrid == null) return null;
        if (x < 0 || y < 0 || x >= cellGrid.length || y >= cellGrid[0].length) return null;
        return cellGrid[x][y];
    }

    public synchronized int getGridWidth() {
        return cellGrid != null ? cellGrid.length : 0;
    }

    public synchronized int getGridHeight() {
        return cellGrid != null && cellGrid.length > 0 ? cellGrid[0].length : 0;
    }

     // agent writes
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
            System.out.println("Target found at " + c + " — total: " + discoveryCount + "/" + totalTargets);
        }
    }
    public synchronized void declareExhausted(String agentId) {
        exhaustedAgents++;
        System.out.println("Agent " + agentId + " exhausted — " + exhaustedAgents + "/" + totalAgents + " idle.");
    }

    // percept assembly
    // thread-safe copy of visited cells
    public synchronized Set<Coordinate> getVisitedCellsSnapshot() {
        return new HashSet<>(visitedCells);
    }
    public synchronized Set<Coordinate> getWallCellsSnapshot() {
        return new HashSet<>(wallCells);
    }
    public synchronized List<Coordinate> getDiscoveredTargetsSnapshot() {
        return new ArrayList<>(discoveredTargets);
    }

    public synchronized boolean isSimulationComplete() {
        return discoveryCount >= totalTargets || exhaustedAgents >= totalAgents;
    }
}
