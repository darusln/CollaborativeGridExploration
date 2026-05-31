package com.gridexploration;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SimulationRunner {

    private int width;
    private int height;
    private int numAgents;
    private int totalTargets;
    private Cell[][] grid;
    private List<Coordinate> agentStarts = new ArrayList<>();
    private List<ExplorerAgent> agents = new ArrayList<>();

    public static void main(String[] args) {
        SimulationRunner runner = new SimulationRunner();
        runner.loadGridConfiguration("config.txt");
        runner.startSimulation();
    }

    public void loadGridConfiguration(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            String section = "";
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                if (line.startsWith("GRID_WIDTH")) {
                    width = Integer.parseInt(line.split(" ")[1]);
                } else if (line.startsWith("GRID_HEIGHT")) {
                    height = Integer.parseInt(line.split(" ")[1]);
                } else if (line.startsWith("NUM_AGENTS")) {
                    numAgents = Integer.parseInt(line.split(" ")[1]);
                } else if (line.startsWith("TOTAL_TARGETS")) {
                    totalTargets = Integer.parseInt(line.split(" ")[1]);
                } else if (line.equals("AGENTS") || line.equals("WALLS") || line.equals("TARGETS")) {
                    section = line;
                    if (grid == null) {
                        grid = new Cell[width][height];
                        for (int i = 0; i < width; i++) {
                            for (int j = 0; j < height; j++) {
                                grid[i][j] = new Cell(i, j, CellType.FREE);
                            }
                        }
                    }
                } else {
                    String[] parts = line.split(",");
                    int x = Integer.parseInt(parts[0].trim());
                    int y = Integer.parseInt(parts[1].trim());

                    if (section.equals("AGENTS")) {
                        agentStarts.add(new Coordinate(x, y));
                    } else if (section.equals("WALLS")) {
                        grid[x][y] = new Cell(x, y, CellType.WALL);
                    } else if (section.equals("TARGETS")) {
                        grid[x][y] = new Cell(x, y, CellType.TARGET);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading config: " + e.getMessage());
            width = 10;
            height = 10;
            totalTargets = 1;
            grid = new Cell[width][height];
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    grid[i][j] = new Cell(i, j, CellType.FREE);
                }
            }
            grid[5][5] = new Cell(5, 5, CellType.TARGET);
            agentStarts.add(new Coordinate(0, 0));
        }
    }

    public void startSimulation() {
        Blackboard.getInstance().init(grid, totalTargets);
        System.out.println("Starting simulation with " + totalTargets + " targets.");

        List<Thread> agentThreads = new ArrayList<>();
        for (int i = 0; i < agentStarts.size(); i++) {
            Coordinate start = agentStarts.get(i);
            ExplorerAgent agent = new ExplorerAgent("Agent-" + i, start.x, start.y);
            agents.add(agent);
            Thread t = new Thread(agent);
            agentThreads.add(t);
            t.start();
        }

        Thread monitor = new Thread(() -> {
            Blackboard bb = Blackboard.getInstance();
            while (!bb.isSimulationComplete()) {
                printGrid();
                try {
                    Thread.sleep(150); // Faster refresh rate for better animation
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            printGrid();
            System.out.println("Simulation Complete!");
        });
        monitor.start();

        for (Thread t : agentThreads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            monitor.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void printGrid() {
        Blackboard bb = Blackboard.getInstance();
        Set<Coordinate> visited = bb.getVisitedCellsSnapshot();
        Set<Coordinate> walls = bb.getWallCellsSnapshot();
        List<Coordinate> targets = bb.getDiscoveredTargetsSnapshot();

        // ANSI escape codes to clear the screen and move the cursor to top-left
        System.out.print("\033[H\033[2J");
        System.out.flush();

        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Collaborative Grid Exploration ===\n");
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Coordinate c = new Coordinate(x, y);
                
                boolean agentHere = false;
                for (ExplorerAgent agent : agents) {
                    if (agent.getCurrentX() == x && agent.getCurrentY() == y) {
                        agentHere = true;
                        break;
                    }
                }

                if (agentHere) {
                    sb.append("A ");
                } else if (walls.contains(c)) {
                    sb.append("W ");
                } else if (targets.contains(c)) {
                    sb.append("T ");
                } else if (visited.contains(c)) {
                    sb.append("O ");
                } else {
                    sb.append("V ");
                }
            }
            sb.append("\n");
        }
        sb.append("Targets Found: ").append(targets.size()).append(" / ").append(totalTargets).append("\n");
        System.out.println(sb.toString());
    }
}
