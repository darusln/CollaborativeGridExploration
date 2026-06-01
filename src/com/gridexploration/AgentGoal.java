package com.gridexploration;

public enum AgentGoal {
    EXPLORE_ADJACENT, // an unvisited neighbour is reachable in one step with greedy local expansion
    NAVIGATE_TO_FRONTIER, // no adjacent frontier exists anf BFS finds the nearest reachable unvisited cell and the agent follows the computed path one step at a time
    IDLE // the entire grid has been explored
}
