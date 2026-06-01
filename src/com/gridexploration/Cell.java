package com.gridexploration;

public class Cell {
    private int x;
    private int y;
    private CellType type;

    public Cell(int x, int y, CellType type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public CellType getType() { return type; }

    @Override
    public String toString() {
        return "Cell[" + x + "," + y + "," + type + "]";
    }

}
