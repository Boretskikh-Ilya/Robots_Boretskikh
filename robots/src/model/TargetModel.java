package model;

import java.awt.Point;

public class TargetModel
{
    private int x;
    private int y;

    public TargetModel(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    public synchronized Point getPosition()
    {
        return new Point(x, y);
    }

    public synchronized void setPosition(Point position)
    {
        x = position.x;
        y = position.y;
    }
}
