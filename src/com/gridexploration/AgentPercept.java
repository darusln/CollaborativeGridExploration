package com.gridexploration;

import java.util.Set;
import java.util.List;
public final class AgentPercept {

    // curent pos
    public final int currentX;
    public final int currentY;

    public final Set<Coordinate> visitedCells;
    public final Set<Coordinate> knownWalls;
    public final List<Coordinate> discoveredTargets;

    public final int totalTargets;

    // grid dim
    public final int gridWidth;
    public final int gridHeight;

    public AgentPercept(
            int currentX, int currentY,
            Set<Coordinate> visitedCells,
            Set<Coordinate> knownWalls,
            List<Coordinate> discoveredTargets,
            int totalTargets,
            int gridWidth, int gridHeight) {

        this.currentX= currentX;
        this.currentY = currentY;
        this.visitedCells = visitedCells;
        this.knownWalls = knownWalls;
        this.discoveredTargets = discoveredTargets;
        this.totalTargets = totalTargets;
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
    }
}
