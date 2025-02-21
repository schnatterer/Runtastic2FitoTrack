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

import androidx.room.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.tadris.fitness.data.preferences.UserMeasurements;

@Entity(tableName = "indoor_workout")
@JsonIgnoreProperties(ignoreUnknown = true)
public class IndoorWorkout extends BaseWorkout {

    public int repetitions;

    /**
     * Average repetition frequency in Hz
     */
    public double avgFrequency;

    /**
     * Max repetition frequency in Hz
     */
    public double maxFrequency;

    /**
     * Maximum intensity
     */
    public double maxIntensity;

    /**
     * Average intensity
     */
    public double avgIntensity;

    public boolean hasIntensityValues() {
        return avgIntensity > 0;
    }

    public boolean hasEstimatedDistance() {
        return workoutTypeId.equals("treadmill");
    }

    public double estimateDistance(UserMeasurements measurements) {
        return repetitions * measurements.getStepLength();
    }

    public double estimateSpeed(UserMeasurements measurements) {
        return estimateDistance(measurements) / (duration / 1000d);
    }

}
