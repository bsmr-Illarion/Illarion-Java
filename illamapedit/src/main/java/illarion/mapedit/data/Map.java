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
package illarion.mapedit.data;

import illarion.common.types.Location;
import illarion.mapedit.events.HistoryPasteCutEvent;
import illarion.mapedit.history.CopyPasteAction;
import illarion.mapedit.history.GroupAction;
import illarion.mapedit.history.ItemPlacedAction;
import org.bushe.swing.event.EventBus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * This class represents a whole map, including name, path, dimensions, and data.
 *
 * @author Tim
 */
public class Map implements Iterable<MapTile> {
    /**
     * The map name
     */
    private final String name;
    /**
     * The path to save this map
     */
    private final Path path;
    /**
     * The width of the map
     */
    private final int width;
    /**
     * The height of the map
     */
    private final int height;
    /**
     * The x coordinate of the origin of the map
     */
    private final int x;
    /**
     * The y coordinate
     */
    private final int y;
    /**
     * The map level
     */
    private final int z;

    /**
     * The tiles.
     */
    @Nonnull
    private final MapTile[] mapTileData;
    private int activeX = Integer.MIN_VALUE;
    private int activeY = Integer.MIN_VALUE;
    private boolean isFillDragging;
    private int positionX;
    private int positionY;
    private boolean visible;
    private int fillStartX;
    private int fillStartY;
    private int fillX;
    private int fillY;
    @Nonnull
    private final SelectionManager selectionManager;

    /**
     * Creates a new map
     *
     * @param name the map name
     * @param path the map path
     * @param w the with of the map
     * @param h the height of the map
     * @param x the x coordinate of the origin of the map
     * @param y the y coordinate of the origin of the map
     * @param z the map level (= z coordinate)
     */
    public Map(final String name, final Path path, final int w, final int h, final int x, final int y, final int z) {
        this.name = name;
        this.path = path;
        width = w;
        height = h;
        this.x = x;
        this.y = y;
        this.z = z;
        mapTileData = new MapTile[w * h];
        visible = true;
        selectionManager = new SelectionManager();
    }

    @Nullable
    public MapTile getActiveTile() {
        return getTileAt(activeX, activeY);
    }

    @Nullable
    public List<MapItem> getItemsOnActiveTile() {
        List<MapItem> items = null;
        final MapTile tile = getTileAt(activeX, activeY);
        if (tile != null) {
            items = tile.getMapItems();
        }
        return items;
    }

    @Nonnull
    public Set<MapPosition> getSelectedTiles() {
        return selectionManager.getSelection();
    }

    public boolean isActiveTile(final int x, final int y) {
        return (activeX == x) && (activeY == y);
    }

    public boolean isFillDragging() {
        return isFillDragging;
    }

    public boolean isPositionAtTile(final int x, final int y) {
        return (positionX == x) && (positionY == y);
    }

    @Nullable
    public ItemPlacedAction removeItemOnActiveTile(final int index) {
        ItemPlacedAction action = null;
        final MapTile tile = getTileAt(activeX, activeY);
        if (tile != null) {

            action = new ItemPlacedAction(activeX, activeY, tile.getMapItemAt(index), null, this);
            tile.removeMapItem(index);
        }
        return action;
    }

    public void replaceItemOnActiveTile(final int index, final int newIndex) {
        final MapTile tile = getTileAt(activeX, activeY);
        if (tile != null) {
            final List<MapItem> items = tile.getMapItems();
            if (items != null) {
                final MapItem item = items.get(index);
                items.set(index, items.get(newIndex));
                items.set(newIndex, item);
            }
        }
    }

    public void setActiveTile(final int x, final int y) {
        activeX = x;
        activeY = y;
    }

    public void setFillingArea(final int x, final int y, final int startX, final int startY) {
        fillX = x;
        fillY = y;
        fillStartX = startX;
        fillStartY = startY;
        isFillDragging = true;
    }

    public void setMapPosition(final int mapX, final int mapY) {
        positionX = mapX - x;
        positionY = mapY - y;
    }

    /**
     * Sets a tile at a specified position.
     *
     * @param mapTile the tile to add.
     */
    public void setTileAt(final int x, final int y, @Nonnull final MapTile mapTile) {
        setTileAtIndex(mapToIndex(x, y), mapTile);
    }

    /**
     * Sets a tile at a specified position.
     *
     * @param index the index where the new tile is set
     * @param mapTile the tile to add.
     */
    private void setTileAtIndex(final int index, @Nonnull final MapTile mapTile) {
        mapTileData[index] = mapTile;
    }

    /**
     * Get a tile located at a specific internal index value.
     */
    MapTile getTileAtIndex(final int index) {
        return mapTileData[index];
    }

    /**
     * Convert map coordinates to the internal index value.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @return the internal index value
     * @throws IllegalArgumentException in case x or y is out of range
     */
    private int mapToIndex(final int x, final int y) {
        if (x < 0 || x >= getWidth()) {
            throw new IllegalArgumentException("X is out of range. 0 <= " + x + " < " + getWidth());
        }
        if (y < 0 || y >= getHeight()) {
            throw new IllegalArgumentException("Y is out of range. 0 <= " + y + " < " + getHeight());
        }
        return (y * width) + x;
    }

    int indexToMapX(final int index) {
        if (index < 0 || index >= mapTileData.length) {
            throw new IllegalArgumentException("Index is out of range. 0 <= " + index + " < " + mapTileData.length);
        }
        return index % width;
    }

    int indexToMapY(final int index) {
        if (index < 0 || index >= mapTileData.length) {
            throw new IllegalArgumentException("Index is out of range. 0 <= " + index + " < " + mapTileData.length);
        }
        return index / width;
    }

    public void setTileAt(@Nonnull final Location loc, @Nonnull final MapTile mapTile) {
        setTileAt(loc.getScX(), loc.getScY(), mapTile);
    }

    /**
     * Adds an item to a specified position.
     *
     * @param x the x coordinate relative to the origin
     * @param y the y coordinate relative to the origin
     * @param mapItem the item  <- u don't sayy ;)
     */
    public void addItemAt(final int x, final int y, final MapItem mapItem) {
        mapTileData[mapToIndex(x, y)].addMapItem(mapItem);
    }

    public void setVisible(final boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    /**
     * Sets a warp point at to specified tile
     *
     * @param x the x coordinate of the warp point
     * @param y the y coordinate of the warp point
     * @param warpPoint the warp point <- u don't sayy ;)
     */
    public void setWarpAt(final int x, final int y, final MapWarpPoint warpPoint) {
        mapTileData[mapToIndex(x, y)].setMapWarpPoint(warpPoint);
    }

    /**
     * Return a tile at a specified position.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @return the tile
     */
    @Nullable
    public MapTile getTileAt(final int x, final int y) {
        if (!contains(x, y)) {
            return null;
        }
        final int i = mapToIndex(x, y);
        if (mapTileData[i] != null) {
            return mapTileData[i];
        }

        final MapTile tile = MapTile.MapTileFactory.createNew(0, 0, 0, 0);
        setTileAtIndex(i, tile);
        return tile;
    }

    @Nullable
    public MapTile getTileAt(@Nonnull final Location loc) {
        return getTileAt(loc.getScX(), loc.getScY());
    }

    /**
     * @return the map width
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return the map height
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return the x coordinate of the origin in global coordinates
     */
    public int getX() {
        return x;
    }

    /**
     * @return the y coordinate of the origin in gobal coordinates
     */
    public int getY() {
        return y;
    }

    /**
     * @return the map level
     */
    public int getZ() {
        return z;
    }

    /**
     * @return the path to save the map
     */
    public Path getPath() {
        return path;
    }

    /**
     * @return the map name
     */
    public String getName() {
        return name;
    }

    public int getFillStartX() {
        return fillStartX;
    }

    public int getFillStartY() {
        return fillStartY;
    }

    public int getFillX() {
        return fillX;
    }

    public int getFillY() {
        return fillY;
    }

    public void setFillDragging(final boolean dragging) {
        isFillDragging = dragging;
    }

    /**
     * Checks if the map contains the following coordinates
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @return {@code true} if the map contains x and y
     */
    public boolean contains(final int x, final int y) {
        return (x >= 0) && (y >= 0) && (x < getWidth()) && (y < getHeight());
    }

    @Override
    public boolean equals(@Nullable final Object obj) {
        if (super.equals(obj)) {
            return true;
        }

        if (obj instanceof Map) {
            final Map otherMap = (Map) obj;
            return name.equals(otherMap.name) && path.equals(otherMap.path);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * Checks if the is selected
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @return {@code true} if x and y is a selected tile
     */
    public boolean isSelected(final int x, final int y) {
        return selectionManager.isSelected(x, y);
    }

    /**
     * Set the selected state of a tile on the map
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @param selected the selected state the tile should have
     */
    public void setSelected(final int x, final int y, final boolean selected) {
        if (selected) {
            selectionManager.select(x, y);
        } else {
            selectionManager.deselect(x, y);
        }
    }

    /**
     * Copies the selected tiles
     *
     * @return a MapSelection with the selected tiles
     */
    public MapSelection copySelectedTiles() {
        return selectionManager.copy(this);
    }

    /**
     * Cuts the selected tiles
     *
     * @return a MapSelection with the selected tiles
     */
    public MapSelection cutSelectedTiles() {
        return selectionManager.cut(this);
    }

    @Nonnull
    @Override
    public String toString() {
        return name;
    }

    /**
     * Pastes the tiles from the MapSelection
     *
     * @param startX starting x coordinate
     * @param startY starting y coordinate
     * @param mapSelection tiles to paste
     */
    public void pasteTiles(final int startX, final int startY, @Nonnull final MapSelection mapSelection) {
        final GroupAction action = new GroupAction();
        for (final MapPosition position : mapSelection.getSelectedPositions()) {
            final int newX = startX + (position.getX() - mapSelection.getOffsetX());
            final int newY = startY + (position.getY() - mapSelection.getOffsetY());

            if (contains(newX, newY)) {
                final MapTile oldTile = getTileAt(newX, newY);
                final MapTile newTile = MapTile.MapTileFactory.copy(mapSelection.getMapTileAt(position));
                action.addAction(new CopyPasteAction(newX, newY, oldTile, newTile, this));
                setTileAt(newX, newY, newTile);
            }
        }
        if (!action.isEmpty()) {
            EventBus.publish(new HistoryPasteCutEvent(action));
        }
    }

    @Override
    public MapIterator iterator() {
        return new MapIterator(this, mapTileData.length);
    }
}

