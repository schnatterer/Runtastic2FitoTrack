/*
 * Copyright (c) 2022 Jannis Scheibe <jannis@tadris.de>
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
import androidx.room.ForeignKey;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import static androidx.room.ForeignKey.CASCADE;

@Entity(tableName = "indoor_sample",
        foreignKeys = @ForeignKey(
                entity = IndoorWorkout.class,
                parentColumns = "id",
                childColumns = "workout_id",
                onDelete = CASCADE))
@JsonIgnoreProperties(ignoreUnknown = true)
public class IndoorSample extends BaseSample {

    @ColumnInfo(name = "workout_id", index = true)
    public long workoutId;

    /**
     * Count of repetitions collected withing this sample
     */
    public int repetitions;

    /**
     * Absolute end timestamp of the sample
     */
    public long absoluteEndTime;

    /**
     * Average intensity
     */
    public double intensity;

    /**
     * Current frequency in Hz
     */
    public double frequency;

    @JsonIgnore
    public long getSampleDuration() {
        return absoluteEndTime - absoluteTime;
    }

}
