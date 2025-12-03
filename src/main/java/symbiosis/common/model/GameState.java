package symbiosis.common.model;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    private CaveMap map;
    private Player fish;
    private Player crab;
    private List<GameObject> objects = new ArrayList<>();
    private boolean levelCompleted;

    public GameState(CaveMap map) {
        this.map = map;
    }

    public CaveMap getMap() {
        return map;
    }

    public Player getFish() {
        return fish;
    }

    public void setFish(Player fish) {
        this.fish = fish;
    }

    public Player getCrab() {
        return crab;
    }

    public void setCrab(Player crab) {
        this.crab = crab;
    }

    public List<GameObject> getObjects() {
        return objects;
    }

    public boolean isLevelCompleted() {
        return levelCompleted;
    }

    public void setLevelCompleted(boolean levelCompleted) {
        this.levelCompleted = levelCompleted;
    }
}
