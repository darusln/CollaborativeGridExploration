package com.gridexploration;

import java.util.ArrayList;
import java.util.List;

public class AgentState {

    private static final int MAX_STUCK_CYCLES = 5;

    private int currentX;
    private int currentY;
    private AgentGoal currentGoal;
    private List<Coordinate> bfsPath;
    private int stuckCounter;

    public AgentState(int startX, int startY) {
        this.currentX    = startX;
        this.currentY    = startY;
        this.currentGoal = AgentGoal.EXPLORE_ADJACENT;
        this.bfsPath     = new ArrayList<>();
        this.stuckCounter = 0;
    }

    // pos
    public int getCurrentX() { return currentX; }
    public int getCurrentY() { return currentY; }

    public void setPosition(int x, int y) {
        currentX = x;
        currentY = y;
    }

    // goal
    public AgentGoal getCurrentGoal() { return currentGoal; }

    public void setCurrentGoal(AgentGoal goal) {
        this.currentGoal = goal;
    }

    public boolean hasBfsPath() {
        return !bfsPath.isEmpty();
    }

    public Coordinate pollNextStep() {
        if (bfsPath.isEmpty()) return null;
        return bfsPath.remove(0);
    }

    public void setBfsPath(List<Coordinate> path) {
        this.bfsPath = new ArrayList<>(path);
    }

    public void clearBfsPath() {
        bfsPath.clear();
    }

    public void incrementStuckCounter() { stuckCounter++; }
    public void resetStuckCounter()     { stuckCounter = 0; }

    public boolean isStuck() {
        return stuckCounter >= MAX_STUCK_CYCLES;
    }
}
