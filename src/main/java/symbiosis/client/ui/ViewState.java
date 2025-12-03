package symbiosis.client.ui;

import symbiosis.common.model.GameObject;
import symbiosis.common.model.PlayerRole;
import symbiosis.common.model.Position;
import symbiosis.common.model.TileType;

import java.util.ArrayList;
import java.util.List;

public class ViewState {

    private volatile Position fishPosition;
    private volatile Position crabPosition;
    private volatile PlayerRole localRole;
    private volatile String clientId;

    private volatile int mapWidth;
    private volatile int mapHeight;
    private volatile TileType[][] tiles;

    private List<GameObject> objects = new ArrayList<>();
    private volatile boolean levelCompleted;

    private volatile SkinTheme skinTheme = SkinTheme.CLASSIC;

    public Position getFishPosition() {
        return fishPosition;
    }

    public void setFishPosition(Position fishPosition) {
        this.fishPosition = fishPosition;
    }

    public Position getCrabPosition() {
        return crabPosition;
    }

    public void setCrabPosition(Position crabPosition) {
        this.crabPosition = crabPosition;
    }

    public PlayerRole getLocalRole() {
        return localRole;
    }

    public void setLocalRole(PlayerRole localRole) {
        this.localRole = localRole;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Position getLocalPlayerPosition() {
        if (localRole == PlayerRole.FISH) {
            return fishPosition;
        } else if (localRole == PlayerRole.CRAB) {
            return crabPosition;
        }
        return null;
    }

    public Position getPartnerPosition() {
        if (localRole == PlayerRole.FISH) {
            return crabPosition;
        } else if (localRole == PlayerRole.CRAB) {
            return fishPosition;
        }
        return null;
    }

    public int getMapWidth() {
        return mapWidth;
    }

    public int getMapHeight() {
        return mapHeight;
    }

    public TileType[][] getTiles() {
        return tiles;
    }

    public void setMap(TileType[][] tiles, int width, int height) {
        this.tiles = tiles;
        this.mapWidth = width;
        this.mapHeight = height;
    }

    public List<GameObject> getObjects() {
        return objects;
    }

    public void setObjects(List<GameObject> objects) {
        this.objects = objects;
    }

    public boolean isLevelCompleted() {
        return levelCompleted;
    }

    public void setLevelCompleted(boolean levelCompleted) {
        this.levelCompleted = levelCompleted;
    }

    public SkinTheme getSkinTheme() {
        return skinTheme;
    }

    public void setSkinTheme(SkinTheme skinTheme) {
        this.skinTheme = skinTheme;
    }
}
