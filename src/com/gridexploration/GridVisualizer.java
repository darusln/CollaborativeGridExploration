package com.gridexploration;

import java.util.List;
import java.util.Set;

/**
 *   A  — agent present at this cell
 *   W  — known wall / obstacle
 *   T  — discovered target
 *   .  — visited
 *   ?  — unknown
 */
public class GridVisualizer implements Runnable {

    private static final int REFRESH_MS = 200;
    private final int gridWidth;
    private final int gridHeight;
    private final int totalTargets;
    private final List<ExplorerAgent> agents;

    public GridVisualizer(int gridWidth, int gridHeight, int totalTargets, List<ExplorerAgent> agents) {
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
        this.totalTargets = totalTargets;
        this.agents = agents;
    }

    @Override
    public void run() {
        Blackboard bb = Blackboard.getInstance();

        while (!bb.isSimulationComplete()) {
            render(bb);
            try {
                Thread.sleep(REFRESH_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        render(bb);
        System.out.println("\nsimulation complete");
    }


    private void render(Blackboard bb) {
        Set<Coordinate> visited = bb.getVisitedCellsSnapshot();
        Set<Coordinate> walls = bb.getWallCellsSnapshot();
        List<Coordinate> targets = bb.getDiscoveredTargetsSnapshot();

        System.out.print("\033[H\033[2J");
        System.out.flush();

        StringBuilder sb = new StringBuilder();
        sb.append("Collaborative grid exploration\n");

        for (ExplorerAgent a : agents) {
            sb.append(String.format("  %-10s @ (%2d,%2d)  goal: %s%n", a.getAgentId(), a.getCurrentX(), a.getCurrentY(), a.getCurrentGoal()));}
        sb.append('\n');

        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                Coordinate c = new Coordinate(x, y);

                // check if any agent is here
                boolean agentHere = false;
                for (ExplorerAgent a : agents) {
                    if (a.getCurrentX() == x && a.getCurrentY() == y) {
                        agentHere = true;
                        break;
                    }
                }

                if (agentHere)
                    sb.append("A ");
                else if (walls.contains(c))
                    sb.append("W ");
                else if (targets.contains(c))
                    sb.append("T ");
                else if (visited.contains(c))
                    sb.append(". ");
                else
                    sb.append("? ");
            }
            sb.append('\n');
        }

        sb.append("\nTargets: ")
                .append(targets.size())
                .append(" / ")
                .append(totalTargets)
                .append(" Visited: ")
                .append(visited.size())
                .append(" cells\n");

        System.out.print(sb);
    }
}
