package com.gridexploration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 *  Percepts  — AgentPercept assembled in getPercepts(). Pulls a consistent
 *              snapshot from the Blackboard (visited, walls, targets) plus
 *              the agent's own position. Defensive copies ensure the percept
 *              is stable for the entire decision cycle.
 *
 *  Actions   — Movement encoded in moveTowards(). Writes back to the
 *              Blackboard (markVisited, markWall, reportTarget) so that every
 *              discovery immediately becomes shared knowledge.
 *
 *  Goals     — AgentGoal enum selected in selectGoal(). Three possible goals:
 *                EXPLORE_ADJACENT     : greedy local expansion
 *                NAVIGATE_TO_FRONTIER : BFS-guided traversal
 *                IDLE                 : no reachable frontiers remain
 *
 *  Environment — Accessed read-only via Blackboard.getCellAt(). The agent
 *                never holds a direct reference to the Cell[][] grid.
 *
 *  State     — AgentState holds position, current goal, BFS path queue, and
 *              a stuck counter that triggers re-planning when the agent is
 *              blocked for several consecutive cycles.
*/
public class ExplorerAgent implements Runnable {

    // cardinal directions N, S, W, E
    private static final int[][] cardinal = { {0, -1}, {0, 1}, {-1, 0}, {1, 0} };

    private static final int cycle_sleep = 50;

    private final String agentId;

    //private memory that persists across decision cycles
    private final AgentState state;
    private boolean exhaustedDeclared = false;
    public ExplorerAgent(String agentId, int startX, int startY) {
        this.agentId = agentId;
        this.state   = new AgentState(startX, startY);
    }

    @Override
    public void run() {
        Blackboard bb = Blackboard.getInstance();

        // bootstrap
        Cell startCell = bb.getCellAt(state.getCurrentX(), state.getCurrentY());
        if (startCell != null && startCell.getType() == CellType.TARGET) {
            bb.reportTarget(state.getCurrentX(), state.getCurrentY());
        }
        bb.markVisited(state.getCurrentX(), state.getCurrentY());

        System.out.println("[" + agentId + "] Started at " + new Coordinate(state.getCurrentX(), state.getCurrentY()));

        while (!bb.isSimulationComplete()) {
            decisionCycle();

            try {
                Thread.sleep(cycle_sleep);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("[" + agentId + "] Terminating at " + new Coordinate(state.getCurrentX(), state.getCurrentY()) + " | Goal was: " + state.getCurrentGoal());
    }

    private void decisionCycle() {

        AgentPercept percept = getPercepts();

        // update internal State from percepts
        updateState(percept);

        AgentGoal goal = selectGoal(percept);
        state.setCurrentGoal(goal);

        Coordinate target = planAction(goal, percept);
        if (target != null) {
            executeAction(target);
        }
    }

    // percepts
    private AgentPercept getPercepts() {
        Blackboard bb = Blackboard.getInstance();
        return new AgentPercept(
                state.getCurrentX(),
                state.getCurrentY(),
                bb.getVisitedCellsSnapshot(),
                bb.getWallCellsSnapshot(),
                bb.getDiscoveredTargetsSnapshot(),
                bb.getGridWidth() * bb.getGridHeight(), // not used here but useful for display
                bb.getGridWidth(),
                bb.getGridHeight()
        );
    }

    // state
    private void updateState(AgentPercept percept) {
        if (state.hasBfsPath()) { }

        // agent marked stuck, force BFS
        if (state.isStuck()) {
            state.clearBfsPath();
            state.resetStuckCounter();
        }
    }

    // goal
    private AgentGoal selectGoal(AgentPercept percept) {

        if (hasAdjacentUnvisited(percept)) {
            return AgentGoal.EXPLORE_ADJACENT;
        }
        if (state.hasBfsPath()) {
            return AgentGoal.NAVIGATE_TO_FRONTIER;
        }

        List<Coordinate> path = computeBfsFrontierPath(percept);
        if (!path.isEmpty()) {
            state.setBfsPath(path);
            return AgentGoal.NAVIGATE_TO_FRONTIER;
        }

        if (!exhaustedDeclared) {
            Blackboard.getInstance().declareExhausted(agentId);
            exhaustedDeclared = true;
        }
        return AgentGoal.IDLE;
    }

    // action

    private Coordinate planAction(AgentGoal goal, AgentPercept percept) {
        switch (goal) {
            case EXPLORE_ADJACENT:
                state.clearBfsPath();
                return pickAdjacentUnvisited(percept);

            case NAVIGATE_TO_FRONTIER:
                Coordinate next = state.pollNextStep();
                if (next == null) {
                    return null;
                }
                return next;

            case IDLE:
            default:
                return null;
        }
    }

    private boolean executeAction(Coordinate targetCoord) {
        Blackboard bb = Blackboard.getInstance();
        Cell targetCell = bb.getCellAt(targetCoord.x, targetCoord.y);

        if (targetCell == null) {
            state.clearBfsPath();
            state.incrementStuckCounter();
            return false;
        }

        if (targetCell.getType() == CellType.WALL) {
            // discovered a wall
            bb.markWall(targetCoord.x, targetCoord.y);
            state.clearBfsPath();
            state.incrementStuckCounter();
            return false;
        }

        // successful move
        state.setPosition(targetCoord.x, targetCoord.y);
        state.resetStuckCounter();
        bb.markVisited(targetCoord.x, targetCoord.y);

        if (targetCell.getType() == CellType.TARGET) {
            bb.reportTarget(targetCoord.x, targetCoord.y);
        }
        return true;
    }

    //  BFS
    private List<Coordinate> computeBfsFrontierPath(AgentPercept percept) {
        Set<Coordinate> visited = percept.visitedCells;
        Set<Coordinate> walls   = percept.knownWalls;
        int width               = percept.gridWidth;
        int height              = percept.gridHeight;

        Queue<List<Coordinate>> queue = new LinkedList<>();
        Set<Coordinate> bfsVisited    = new HashSet<>();

        Coordinate start = new Coordinate(percept.currentX, percept.currentY);
        bfsVisited.add(start);

        for (int[] dir : cardinal) {
            int nx = percept.currentX + dir[0];
            int ny = percept.currentY + dir[1];
            Coordinate nb = new Coordinate(nx, ny);

            if (inBounds(nx, ny, width, height) && !walls.contains(nb) && !bfsVisited.contains(nb)) {
                bfsVisited.add(nb);
                List<Coordinate> path = new ArrayList<>();
                path.add(nb);
                queue.add(path);
            }
        }

        while (!queue.isEmpty()) {
            List<Coordinate> path = queue.poll();
            Coordinate last = path.get(path.size() - 1);

            if (!visited.contains(last) && !walls.contains(last)) {
                return path;
            }

            for (int[] dir : cardinal) {
                int nx = last.x + dir[0];
                int ny = last.y + dir[1];
                Coordinate nb = new Coordinate(nx, ny);

                if (inBounds(nx, ny, width, height) && !walls.contains(nb) && !bfsVisited.contains(nb)) {
                    bfsVisited.add(nb);
                    List<Coordinate> newPath = new ArrayList<>(path);
                    newPath.add(nb);
                    queue.add(newPath);
                }
            }
        }

        return new ArrayList<>();
    }

    private boolean hasAdjacentUnvisited(AgentPercept percept) {

        return pickAdjacentUnvisited(percept) != null;
    }

    private Coordinate pickAdjacentUnvisited(AgentPercept percept) {
        for (int[] dir : cardinal) {
            int nx = percept.currentX + dir[0];
            int ny = percept.currentY + dir[1];
            Coordinate nb = new Coordinate(nx, ny);

            if (inBounds(nx, ny, percept.gridWidth, percept.gridHeight) && !percept.visitedCells.contains(nb) && !percept.knownWalls.contains(nb)) {
                return nb;
            }
        }
        return null;
    }

    private boolean inBounds(int x, int y, int width, int height) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public String getAgentId()    { return agentId; }
    public int    getCurrentX()  { return state.getCurrentX(); }
    public int    getCurrentY()  { return state.getCurrentY(); }
    public AgentGoal getCurrentGoal() { return state.getCurrentGoal(); }
}
