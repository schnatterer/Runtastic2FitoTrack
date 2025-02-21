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
import androidx.room.PrimaryKey;

/**
 * Data point for workouts
 */
public abstract class BaseSample {

    /**
     * Unique id
     */
    @PrimaryKey
    public long id;

    /**
     * Absolute time in ms UNIX time
     */
    public long absoluteTime;

    /**
     * Time since workout start in ms
     */
    public long relativeTime;

    /**
     * Heart rate in bpm
     * -1 if no heart rate is available
     */
    @ColumnInfo(name = "heart_rate")
    public int heartRate = -1;

    /**
     * -1 -> No interval was triggered
     * greater than 0 -> Interval with this id was triggered at this sample
     */
    @ColumnInfo(name = "interval_triggered")
    public long intervalTriggered = -1;

}
