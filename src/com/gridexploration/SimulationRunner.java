package com.gridexploration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
public class SimulationRunner {

    private static final String config_source = "resources/config.txt";
    private int width;
    private int height;
    private int totalTargets;
    private Cell[][] grid;
    private final List<Coordinate> agentStarts = new ArrayList<>();

    public static void main(String[] args) {
        new SimulationRunner().run();
    }

    public void run() {
        loadGridConfiguration();
        startSimulation();
    }

    public void loadGridConfiguration() {
        InputStream is = getClass().getClassLoader().getResourceAsStream(config_source);

        if (is == null) {
            System.err.println("confif not found, using default grid");
            buildDefaultGrid();
            return;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            parseConfig(br);
        } catch (IOException e) {
            System.err.println("error eading config: " + e.getMessage());
            buildDefaultGrid();
        }
    }

    private void parseConfig(BufferedReader br) throws IOException {
        String section = "";
        String line;

        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            if (line.startsWith("GRID_WIDTH")) {
                width = Integer.parseInt(line.split("\\s+")[1]);
            } else if (line.startsWith("GRID_HEIGHT")) {
                height = Integer.parseInt(line.split("\\s+")[1]);
            } else if (line.startsWith("NUM_AGENTS")) {
                // Informational only; actual agent count = agentStarts.size()
            } else if (line.startsWith("TOTAL_TARGETS")) {
                totalTargets = Integer.parseInt(line.split("\\s+")[1]);
            } else if (line.equals("AGENTS") || line.equals("WALLS") || line.equals("TARGETS")) {
                section = line;
                ensureGridInitialised();
            } else {
                String[] parts = line.split(",");
                if (parts.length < 2) continue;
                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());

                switch (section) {
                    case "AGENTS":
                        agentStarts.add(new Coordinate(x, y));
                        break;
                    case "WALLS":
                        if (inBounds(x, y)) grid[x][y] = new Cell(x, y, CellType.WALL);
                        break;
                    case "TARGETS":
                        if (inBounds(x, y)) grid[x][y] = new Cell(x, y, CellType.TARGET);
                        break;
                    default:
                        System.err.println("unexpected data outside section: " + line);
                }
            }
        }

        // if no agents were specified, add one at (0,0)
        if (agentStarts.isEmpty()) {
            agentStarts.add(new Coordinate(0, 0));
        }
    }

    private void ensureGridInitialised() {
        if (grid != null) return;
        if (width <= 0 || height <= 0) {
            width  = 10;
            height = 10;
        }
        grid = new Cell[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                grid[i][j] = new Cell(i, j, CellType.FREE);
            }
        }
    }

    private void buildDefaultGrid() {
        width        = 10;
        height       = 10;
        totalTargets = 1;

        grid = new Cell[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                grid[i][j] = new Cell(i, j, CellType.FREE);
            }
        }
        for (int i = 2; i < 8; i++) {
            grid[i][4] = new Cell(i, 4, CellType.WALL);
        }
        grid[5][5] = new Cell(5, 5, CellType.TARGET);

        agentStarts.add(new Coordinate(0, 0));
        agentStarts.add(new Coordinate(9, 9));
    }

    public void startSimulation() {
        int agentCount = agentStarts.size();

        Blackboard.getInstance().init(grid, totalTargets, agentCount);

        System.out.println("Grid: " + width + "x" + height + "  Targets: " + totalTargets + "  Agents: " + agentCount);

        List<ExplorerAgent> agents = new ArrayList<>();
        List<Thread> agentThreads  = new ArrayList<>();

        for (int i = 0; i < agentCount; i++) {
            Coordinate start = agentStarts.get(i);
            ExplorerAgent agent = new ExplorerAgent("Agent-" + i, start.x, start.y);
            agents.add(agent);
            Thread t = new Thread(agent, "explorer-" + i);
            agentThreads.add(t);
        }

        GridVisualizer visualizer = new GridVisualizer(width, height, totalTargets, agents);
        Thread vizThread = new Thread(visualizer, "visualizer");
        vizThread.setDaemon(true);
        vizThread.start();

        for (Thread t : agentThreads) {
            t.start();
        }

        for (Thread t : agentThreads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        try {
            vizThread.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    private boolean inBounds(int x, int y) {
        return grid != null && x >= 0 && y >= 0 && x < width && y < height;
    }
}
