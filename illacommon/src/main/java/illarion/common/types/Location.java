/*
 * This file is part of the Illarion project.
 *
 * Copyright © 2015 - Illarion e.V.
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
package illarion.common.types;

import illarion.common.graphics.Layers;
import illarion.common.graphics.MapConstants;
import illarion.common.net.NetCommReader;
import illarion.common.util.FastMath;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.io.Serializable;

/**
 * Storage for the server map and all recalculation function for the Client screen representations.
 *
 * @author Nop
 * @author Martin Karing &lt;nitram@illarion.org&gt;
 */
@NotThreadSafe
@SuppressWarnings({"unused", "OverlyComplexClass"})
public class Location implements Serializable {
    /**
     * Modifier used at the calculation of the display coordinates in case its a tile above or below the level 0.
     */
    public static final int DISPLAY_Z_OFFSET_MOD = 6;

    /**
     * Modifier of the X-Coordinate of the server coordinates to calculate a key of this position.
     */
    private static final long KEY_MOD_X = 65536L;

    /**
     * Modifier of the Y-Coordinate of the server coordinates to calculate a key of this position.
     */
    private static final long KEY_MOD_Y = 1L;

    /**
     * Modifier of the Z-Coordinate of the server coordinates to calculate a key of this position.
     */
    private static final long KEY_MOD_Z = 4294967296L;

    /**
     * The serialization UID of this location class.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Column of map tiles on the client map.
     */
    private int col;

    /**
     * X-Coordinate of the Display coordinates of the tile. Means on what position of the game window the marked
     * position is shown.
     */
    private int dcX;

    /**
     * Y-Coordinate of the Display coordinates of the tile. Means on what position of the game window the marked
     * position is shown.
     */
    private int dcY;

    /**
     * Z-Coordinate of the Display coordinates of the tile. Means on what position of the game window the marked
     * position is shown.
     */
    private int dcZ;

    /**
     * True if the display coordinates need to be calculated.
     */
    private boolean dirtyDC;

    /**
     * True if the map coordinates need to be calculated.
     */
    private boolean dirtyMC;

    /**
     * True if the server coordinates need to be calculated.
     */
    private boolean dirtySC;

    /**
     * Row of map tiles on the client map.
     */
    private int row;

    /**
     * X-Coordinate on the Server map.
     */
    private int scX;

    /**
     * Y-Coordinate on the Server map.
     */
    private int scY;

    /**
     * Z-Coordinate on the Server map.
     */
    private int scZ;

    /**
     * Constructor of a new location object pointing to 0, 0, 0 on the server coordinates.
     */
    public Location() {
        reset();
    }

    /**
     * Create a new instance of the location that points to a specified location on the map.
     *
     * @param c the column of the map coordinates
     * @param r the row of the map coordinates
     */
    public Location(int c, int r) {
        this();
        setMC(c, r);
    }

    /**
     * Create a new instance of the location that points to a specified location on the map.
     *
     * @param x the x coordinate of the server coordinates of the target position
     * @param y the y coordinate of the server coordinates of the target position
     * @param z the z coordinate of the server coordinates of the target position
     */
    public Location(int x, int y, int z) {
        this();
        setSC(x, y, z);
    }

    /**
     * Copy constructor. This constructor creates a copy of the location instance set here.
     *
     * @param org the original Location instance
     */
    public Location(@Nonnull Location org) {
        this();
        set(org);
    }

    /**
     * Copy constructor. This constructor creates a copy of the location instance set here and moves the new instance
     * to a specified direction.
     *
     * @param org the original Location instance
     * @param direction the direction to move the location to
     */
    public Location(@Nonnull Location org, @Nullable Direction direction) {
        this();
        set(org);
        moveSC(direction);
    }

    /**
     * Read the location from the network interface.
     *
     * @param reader the reader for the network interface
     * @throws IOException in case reading the location fails
     */
    public Location(@Nonnull NetCommReader reader) throws IOException {
        this();
        int x = reader.readShort();
        int y = reader.readShort();
        int z = reader.readShort();
        setSC(x, y, z);
    }

    /**
     * Calculate the display coordinates from floating server coordinates. This function returns the X part of the
     * display coordinates where a object with this coordinates needs to be displayed.
     *
     * @param x the x coordinate of the server location that shall be converted
     * @param y the y coordinate of the server location that shall be converted
     * @param z the z coordinate of the server location that shall be converted
     * @return the x coordinate of the display coordinates where the object needs to be displayed
     */
    @Contract(pure = true)
    public static int displayCoordinateX(float x, float y, float z) {
        return (int) ((x + y) * MapConstants.STEP_X);
    }

    /**
     * Calculate the display coordinates from floating server coordinates. This function returns the Y part of the
     * display coordinates where a object with this coordinates needs to be displayed.
     *
     * @param x the x coordinate of the server location that shall be converted
     * @param y the y coordinate of the server location that shall be converted
     * @param z the z coordinate of the server location that shall be converted
     * @return the y coordinate of the display coordinates where the object needs to be displayed
     */
    @Contract(pure = true)
    public static int displayCoordinateY(float x, float y, float z) {
        return (int) (((x - y) * MapConstants.STEP_Y) + (DISPLAY_Z_OFFSET_MOD * z * MapConstants.STEP_Y));
    }

    /**
     * Calculate the display coordinates from floating server coordinates. This function returns the Z part of the
     * display coordinates where a object with this coordinates needs to be displayed.
     *
     * @param x the x coordinate of the server location that shall be converted
     * @param y the y coordinate of the server location that shall be converted
     * @param z the z coordinate of the server location that shall be converted
     * @return the z coordinate of the display coordinates where the object needs to be displayed
     */
    @Contract(pure = true)
    public static int displayCoordinateZ(float x, float y, float z) {
        return (int) ((x - y - (z * Layers.LEVEL)) * Layers.DISTANCE);
    }

    /**
     * Create a key that identifies a position exactly. Can be used for collection classes. The key calculated using
     * the
     * server position.
     *
     * @param x the X-Coordinate of the server coordinates used to calculate the key
     * @param y the Y-Coordinate of the server coordinates used to calculate the key
     * @param z the Z-Coordinate of the server coordinates used to calculate the key
     * @return the key of this position
     */
    @Contract(pure = true)
    public static long getKey(int x, int y, int z) {
        return (z * KEY_MOD_Z) + (x * KEY_MOD_X) + (y * KEY_MOD_Y);
    }

    /**
     * Add an offset to the display location. The calculation to map and server coordinates is triggered automatically.
     *
     * @param x Value to add to the X-Coordinate of the display location
     * @param y Value to add to the Y-Coordinate of the display location
     * @param z Value to add to the Z-Coordinate of the display location
     */
    public void addDC(int x, int y, int z) {
        if (dirtyDC) {
            toDisplayCoordinates();
        }
        dcX += x;
        dcY += y;
        dcZ += z;

        dirtySC = true;
        dirtyMC = true;
        dirtyDC = false;
    }

    /**
     * Add an offset to the map location. The calculation to Server and Display coordinates is triggered automatically.
     *
     * @param c Value to add to the column of the map coordinates
     * @param r Value to add to the row of the map coordinates
     */
    public void addMC(int c, int r) {
        if (dirtyMC) {
            toMapCoordinates();
        }
        col += c;
        row += r;

        dirtySC = true;
        dirtyMC = false;
        dirtyDC = true;
    }

    /**
     * Add an offset to the server location. The calculation to Map and Display coordinates is triggered automatically.
     *
     * @param x Value to add to the X-Coordinate of the server location
     * @param y Value to add to the Y-Coordinate of the server location
     * @param z Value to add to the Z-Coordinate of the server location
     */
    public void addSC(int x, int y, int z) {
        if (dirtySC) {
            toServerCoordinates();
        }
        scX += x;
        scY += y;
        scZ += z;

        dirtySC = false;
        dirtyMC = true;
        dirtyDC = true;
    }

    /**
     * Check this location and a second one for equality. Two locations are considered equal in case the server
     * coordinates fit.
     *
     * @param obj the second location
     * @return true in case the server coordinates of this location and the second location are the same.
     */
    @Override
    @Contract(value = "null -> false", pure = true)
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Location)) {
            return false;
        }
        Location loc = (Location) obj;
        if (dirtySC) {
            toServerCoordinates();
        }
        if (loc.dirtySC) {
            loc.toServerCoordinates();
        }
        return (scX == loc.scX) && (scY == loc.scY) && (scZ == loc.scZ);
    }

    /**
     * Test if the position is identical with the server coordinates handed over to this function.
     *
     * @param x X-Coordinate of the server coordinates
     * @param y Y-Coordinate of the server coordinates
     * @param z Y-Coordinate of the server coordinates
     * @return true if the coordinates set as parameters of this function are identical with the current position.
     */
    @Contract(pure = true)
    public boolean equalsSC(int x, int y, int z) {
        if (dirtySC) {
            toServerCoordinates();
        }
        return (scX == x) && (scY == y) && (scZ == z);
    }

    /**
     * Get the column on the client map.
     *
     * @return the column on the client map
     */
    @Contract(pure = true)
    public int getCol() {
        if (dirtyMC) {
            toMapCoordinates();
        }
        return col;
    }

    /**
     * Get the X-Coordinate of the Display Coordinates.
     *
     * @return X-Coordinate of the Display Coordinates
     */
    @Contract(pure = true)
    public int getDcX() {
        if (dirtyDC) {
            toDisplayCoordinates();
        }
        return dcX;
    }

    /**
     * Get the Y-Coordinate of the Display Coordinates.
     *
     * @return Y-Coordinate of the Display Coordinates
     */
    @Contract(pure = true)
    public int getDcY() {
        if (dirtyDC) {
            toDisplayCoordinates();
        }
        return dcY;
    }

    /**
     * Get the Z-Coordinate of the Display Coordinates.
     *
     * @return Z-Coordinate of the Display Coordinates
     */
    @Contract(pure = true)
    public int getDcZ() {
        if (dirtyDC) {
            toDisplayCoordinates();
        }
        return dcZ;
    }

    /**
     * Determine the direction to get from the current location to the coordinates handed over to this function, using
     * the 8 directions system. The coordinates used are the server coordinates.
     *
     * @param x X-Coordinate of the target location
     * @param y Y-Coordinate of the target location
     * @return the direction needed to get from the current location to the target location
     */
    @Nullable
    @Contract(pure = true)
    public Direction getDirection(int x, int y) {
        if (dirtySC) {
            toServerCoordinates();
        }
        // Calculate relative movement
        int dirX = FastMath.sign(x - scX);
        int dirY = FastMath.sign(y - scY);

        //noinspection ConstantConditions
        for (Direction dir : Direction.values()) {
            if ((dir.getDirectionVectorX() == dirX) && (dir.getDirectionVectorY() == dirY)) {
                return dir;
            }
        }
        return null;
    }

    /**
     * Determine the direction needed to change the current location to the location that is handed over to this
     * function using the 8 direction system. The coordinates used are the server coordinates.
     *
     * @param loc The target location
     * @return the direction needed to get from the current location to the target location
     */
    @Nullable
    @Contract(pure = true)
    public Direction getDirection(@Nonnull Location loc) {
        if (loc.dirtySC) {
            loc.toServerCoordinates();
        }
        return getDirection(loc.scX, loc.scY);
    }

    /**
     * Get the distance in needed steps from the current position to the target position.
     *
     * @param loc the target position
     * @return the amount of steps needed to get from the current position to the target position in case there are not
     * blocked tiles on the way
     */
    @Contract(pure = true)
    public int getDistance(@Nonnull Location loc) {
        if (dirtySC) {
            toServerCoordinates();
        }
        if (loc.dirtySC) {
            loc.toServerCoordinates();
        }
        int diffX = Math.abs(loc.scX - scX);
        int diffY = Math.abs(loc.scY - scY);
        return Math.max(diffX, diffY);
    }

    /**
     * Create a key that identifies this position exactly. Can be used for collection classes. The key calculated using
     * the server position.
     *
     * @return the key of this position
     */
    @Contract(pure = true)
    public long getKey() {
        if (dirtySC) {
            toServerCoordinates();
        }

        return getKey(scX, scY, scZ);
    }

    /**
     * Get the row on the client map.
     *
     * @return the row on the client map
     */
    @Contract(pure = true)
    public int getRow() {
        if (dirtyMC) {
            toMapCoordinates();
        }
        return row;
    }

    /**
     * Get the X-Coordinate of the Server Coordinates.
     *
     * @return X-Coordinate of the Server Coordinates
     */
    @Contract(pure = true)
    public int getScX() {
        if (dirtySC) {
            toServerCoordinates();
        }
        return scX;
    }

    /**
     * Get the Y-Coordinate of the Server Coordinates.
     *
     * @return Y-Coordinate of the Server Coordinates
     */
    @Contract(pure = true)
    public int getScY() {
        if (dirtySC) {
            toServerCoordinates();
        }
        return scY;
    }

    /**
     * Get the Z-Coordinate of the Server Coordinates.
     *
     * @return Z-Coordinate of the Server Coordinates
     */
    @Contract(pure = true)
    public int getScZ() {
        if (dirtySC) {
            toServerCoordinates();
        }
        return scZ;
    }

    /**
     * Get the square root distance between two locations.
     *
     * @param loc the target location
     * @return the square root distance between the two locations. So the length of a straight line between this
     * location and the target location.
     */
    @Contract(pure = true)
    public float getSqrtDistance(@Nonnull Location loc) {
        if (dirtySC) {
            toServerCoordinates();
        }
        if (loc.dirtySC) {
            loc.toServerCoordinates();
        }
        return FastMath.sqrt(FastMath.pow(loc.scX - scX, 2) + FastMath.pow(loc.scY - scY, 2));
    }

    /**
     * Generate the hash code of this location object.
     *
     * @return the hash code
     */
    @Override
    @Contract(pure = true)
    public int hashCode() {
        if (dirtySC) {
            toServerCoordinates();
        }
        int hash = 23;
        hash = (hash * 31) + scX;
        hash = (hash * 31) + scY;
        hash = (hash * 31) + scZ;
        return hash;
    }

    /**
     * Determine if a location is empty or at the origin.
     *
     * @return true if all 3 components of the server coordinate are 0
     */
    @Contract(pure = true)
    public boolean isEmpty() {
        if (dirtySC) {
            toServerCoordinates();
        }
        return (scX == 0) && (scY == 0) && (scZ == 0);
    }

    /**
     * Determine if this location and a second one are direct neighbours. So they have to touch each other.
     *
     * @param loc the second location
     * @return true in case this location and the second one are touching each other
     */
    @Contract(pure = true)
    public boolean isNeighbour(@Nonnull Location loc) {
        if (dirtySC) {
            toServerCoordinates();
        }
        if (loc.dirtySC) {
            loc.toServerCoordinates();
        }
        return (FastMath.abs(loc.scX - scX) < 2) && (FastMath.abs(loc.scY - scY) < 2);
    }

    /**
     * Move location one step into a direction using the 4 direction system.
     *
     * @param dir The direction the Server coordinates are moved by
     */
    public void moveSC(@Nullable Direction dir) {
        if (dir == null) {
            return;
        }
        if (dirtySC) {
            toServerCoordinates();
        }

        scX += dir.getDirectionVectorX();
        scY += dir.getDirectionVectorY();

        dirtySC = false;
        dirtyMC = true;
        dirtyDC = true;
    }

    public void reset() {
        dirtyDC = true;
        dirtyMC = true;
        dirtySC = true;
    }

    /**
     * Set location from given location.
     *
     * @param loc The source location that is copied to this location
     */
    public void set(@Nonnull Location loc) {
        scX = loc.scX;
        scY = loc.scY;
        scZ = loc.scZ;

        col = loc.col;
        row = loc.row;

        dcX = loc.dcX;
        dcY = loc.dcY;
        dcZ = loc.dcZ;

        dirtySC = loc.dirtySC;
        dirtyDC = loc.dirtyDC;
        dirtyMC = loc.dirtyMC;
    }

    /**
     * Set the location to some display coordinates. The calculations to map and server coordinates is done
     * automatically. Z coordinate is used as 0.
     *
     * @param x X-Coordinate of the display coordinates
     * @param y Y-Coordinate of the display coordinates
     */
    public void setDC(int x, int y) {
        setDC(x, y, 0);
    }

    /**
     * Set the location to some display coordinates. The calculations to map and server coordinates is done
     * automatically.
     *
     * @param x X-Coordinate of the display coordinates
     * @param y Y-Coordinate of the display coordinates
     * @param z Z-Coordinate of the display coordinates
     */
    public void setDC(int x, int y, int z) {
        dcX = x;
        dcY = y;
        dcZ = z;

        dirtySC = true;
        dirtyMC = true;
        dirtyDC = false;
    }

    /**
     * Set the server coordinates over a key that was created by the {@link #getKey()} or the
     * {@link #getKey(int, int, int)} method.
     *
     * @param key the key used to set the server coordinates of the location
     */
    public void setKey(long key) {
        setSC((int) (((key % KEY_MOD_Z) / KEY_MOD_X) - (KEY_MOD_X / 2)), (int) (key % KEY_MOD_Z % KEY_MOD_X),
              (int) ((key / KEY_MOD_Z) - (KEY_MOD_Z / 2)));
    }

    /**
     * Set a location to some map coordinates. The server and display coordinates are calculated automatically.
     *
     * @param c Column of the map coordinates
     * @param r Row of the map coordinates
     */
    public void setMC(int c, int r) {
        col = c;
        row = r;

        dirtySC = true;
        dirtyMC = false;
        dirtyDC = true;
    }

    /**
     * Set the location to some server coordinates. The calculations to map and display coordinates is done
     * automatically. Z is used as 0.
     *
     * @param x X-Coordinate of the server coordinates
     * @param y Y-Coordinate of the server coordinates
     */
    public void setSC(int x, int y) {
        setSC(x, y, 0);
    }

    /**
     * Set the location to some server coordinates. The calculations to map and display coordinates is done
     * automatically.
     *
     * @param x X-Coordinate of the server coordinates
     * @param y Y-Coordinate of the server coordinates
     * @param z Z-Coordinate of the server coordinates
     */
    public void setSC(int x, int y, int z) {
        scX = x;
        scY = y;
        scZ = z;

        dirtySC = false;
        dirtyMC = true;
        dirtyDC = true;
    }

    /**
     * Create a string with the server coordinates of this position.
     *
     * @return the string of the server coordinates
     */
    @Nonnull
    @Override
    public String toString() {
        if (dirtySC) {
            toServerCoordinates();
        }
        return "Location: " + scX + ',' + scY + ',' + scZ;
    }

    /**
     * Use the server coordinates and the map coordinates to calculate the display coordinates.
     */
    private void toDisplayCoordinates() {
        if (!dirtyDC) {
            return;
        }
        if (!dirtySC) {
            dcX = (scX + scY) * MapConstants.STEP_X;
            dcY = -(((scX - scY) * MapConstants.STEP_Y) + (DISPLAY_Z_OFFSET_MOD * scZ * MapConstants.STEP_Y));
            dcZ = (scX - scY - (scZ * Layers.LEVEL)) * Layers.DISTANCE;

            dirtyDC = false;
        } else if (!dirtyMC) {
            dcX = col * MapConstants.STEP_X;
            dcY = -row * MapConstants.STEP_Y;
            dcZ = row * Layers.DISTANCE;

            dirtyDC = false;
        }
    }

    /**
     * Use the server coordinates to calculate the map coordinates.
     */
    private void toMapCoordinates() {
        if (!dirtyMC) {
            return;
        }
        if (!dirtySC) {
            col = scX + scY;
            row = scX - scY;

            dirtyMC = false;
        } else if (!dirtyDC) {
            col = FastMath.round(dcX / (float) MapConstants.STEP_X);
            row = FastMath.round(-dcY / (float) MapConstants.STEP_Y);

            dirtyMC = false;
        }
    }

    /**
     * Use the map coordinates to calculate the server coordinates.
     */
    private void toServerCoordinates() {
        if (!dirtySC) {
            return;
        }
        if (!dirtyMC) {
            scX = (row + col) / 2;
            scY = (col - row) / 2;
            scZ = 0;

            dirtySC = false;
        } else if (!dirtyDC) {
            scX = FastMath.round(((-dcY / (float) MapConstants.STEP_Y) +
                    (dcX / (float) MapConstants.STEP_X)) / 2.f);
            scY = FastMath.round(((dcX / (float) MapConstants.STEP_X) -
                    (-dcY / (float) MapConstants.STEP_Y)) / 2.f);
            scZ = 0;

            dirtySC = false;
        }
    }
}
