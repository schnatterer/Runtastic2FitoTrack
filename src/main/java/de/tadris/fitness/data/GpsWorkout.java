/*
 * Copyright (c) 2021 Jannis Scheibe <jannis@tadris.de>
 *
 * This file is part of FitoTrack
 *
 * FitoTrack is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     FitoTrack is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.tadris.fitness.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity(tableName = "workout")
@JsonIgnoreProperties(ignoreUnknown = true)
public class GpsWorkout extends BaseWorkout {

    /**
     * Length of workout in meters
     */
    public int length;

    /**
     * Average speed (moving) of workout in m/s
     */
    public double avgSpeed;

    /**
     * Average speed (total)
     *
     * @return speed in m/s
     */
    @JsonIgnore
    public double getAvgSpeedTotal() {
        return (double) length / ((double) (end - start) / 1000);
    }

    /**
     * Top speed in m/s
     */
    public double topSpeed;

    /**
     * Average pace of workout in min/km
     */
    public double avgPace;

    /**
     * Minimum elevation over the media sea level in meters
     */
    @ColumnInfo(name = "min_elevation_msl")
    public float minElevationMSL;

    /**
     * Maximum elevation over the media sea level in meters
     */
    @ColumnInfo(name = "max_elevation_msl")
    public float maxElevationMSL;

    public float ascent;

    public float descent;

    public String toString() {
        if (comment != null && comment.length() > 2) {
            return comment;
        } else {
            return getDateString();
        }
    }
}