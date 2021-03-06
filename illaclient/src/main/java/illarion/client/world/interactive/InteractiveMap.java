/*
 * This file is part of the Illarion project.
 *
 * Copyright © 2014 - Illarion e.V.
 *
 * Illarion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Illarion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package illarion.client.world.interactive;

import illarion.client.graphics.MapDisplayManager;
import illarion.client.world.GameMap;
import illarion.client.world.MapTile;
import illarion.client.world.World;
import illarion.common.types.Location;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static illarion.client.graphics.MapDisplayManager.TILE_PERSPECTIVE_OFFSET;

/**
 * This interactive map class is used for the user interaction with the game map.
 *
 * @author Martin Karing &lt;nitram@illarion.org&gt;
 */
public final class InteractiveMap {
    /**
     * The actual map that supplies this class with data.
     */
    private final GameMap parentMap;

    /**
     * Create a interactive map for a single map instance.
     *
     * @param map the map to interact with
     */
    public InteractiveMap(final GameMap map) {
        parentMap = map;
    }

    @Nullable
    private static InteractiveMapTile getInteractiveTile(@Nullable final MapTile tile) {
        if (tile != null) {
            return tile.getInteractive();
        }
        return null;
    }

    @Nullable
    public InteractiveMapTile getInteractiveTileOnDisplayLoc(final int displayX, final int displayY) {
        return getInteractiveTile(getTileOnDisplayLoc(displayX, displayY));
    }

    @Nullable
    public InteractiveMapTile getInteractiveTileOnMapLoc(final int locX, final int locY, final int locZ) {
        return getInteractiveTile(getTileOnMapLoc(locX, locY, locZ));
    }

    @Nullable
    public InteractiveMapTile getInteractiveTileOnMapLoc(@Nonnull final Location loc) {
        return getInteractiveTile(getTileOnMapLoc(loc));
    }

    @Nullable
    public InteractiveMapTile getInteractiveTileOnScreenLoc(final int screenX, final int screenY) {
        return getInteractiveTile(getTileOnScreenLoc(screenX, screenY));
    }

    @Nullable
    public MapTile getTileOnDisplayLoc(final int displayX, final int displayY) {
        final Location helpLoc = new Location();
        helpLoc.setDC(displayX, displayY);

        final int playerBase = World.getPlayer().getBaseLevel();
        final int base = playerBase - 2;
        final int lowX = helpLoc.getScX() - (base * TILE_PERSPECTIVE_OFFSET);
        final int lowY = helpLoc.getScY() + (base * TILE_PERSPECTIVE_OFFSET);

        for (int i = 4; i >= 0; --i) {
            final int levelOffset = TILE_PERSPECTIVE_OFFSET * i;

            final int tilePosX = lowX - levelOffset;
            final int tilePosY = lowY + levelOffset;
            final int tilePosZ = base + i;

            @Nullable final MapTile foundElevatedTile = parentMap.getMapAt(tilePosX - 1, tilePosY + 1, tilePosZ);
            if ((foundElevatedTile != null) && (foundElevatedTile.getElevation() > 0)) {
                helpLoc.setDC(displayX, displayY + foundElevatedTile.getElevation());

                final int elevatedX = helpLoc.getScX() - (tilePosZ * TILE_PERSPECTIVE_OFFSET);
                final int elevatedY = helpLoc.getScY() + (tilePosZ * TILE_PERSPECTIVE_OFFSET);

                if ((elevatedX == (tilePosX - 1)) && (elevatedY == (tilePosY + 1))) {
                    return foundElevatedTile;
                }
            }

            @Nullable final MapTile foundTile = parentMap.getMapAt(tilePosX, tilePosY, tilePosZ);
            if ((foundTile != null) && !foundTile.isHidden()) {
                return foundTile;
            }
        }

        return null;
    }

    @Nullable
    public MapTile getTileOnMapLoc(final int locX, final int locY, final int locZ) {
        return parentMap.getMapAt(locX, locY, locZ);
    }

    @Nullable
    public MapTile getTileOnMapLoc(@Nonnull final Location loc) {
        return parentMap.getMapAt(loc);
    }

    @Nullable
    public MapTile getTileOnScreenLoc(final int screenX, final int screenY) {
        final MapDisplayManager displayManager = World.getMapDisplay();

        return getTileOnDisplayLoc(displayManager.getWorldX(screenX), displayManager.getWorldY(screenY));
    }
}
