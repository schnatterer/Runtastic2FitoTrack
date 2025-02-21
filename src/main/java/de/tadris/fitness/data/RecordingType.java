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

import de.tadris.fitness.ui.record.RecordGpsWorkoutActivity;
import de.tadris.fitness.ui.record.RecordIndoorWorkoutActivity;
import de.tadris.fitness.ui.record.RecordWorkoutActivity;
import de.tadris.fitness.ui.workout.ShowGpsWorkoutActivity;
import de.tadris.fitness.ui.workout.ShowIndoorWorkoutActivity;
import de.tadris.fitness.ui.workout.WorkoutActivity;

public enum RecordingType {

    INDOOR("indoor", RecordIndoorWorkoutActivity.class, ShowIndoorWorkoutActivity.class),
    GPS("gps", RecordGpsWorkoutActivity.class, ShowGpsWorkoutActivity.class);

    public final String id;
    public final Class<? extends RecordWorkoutActivity> recorderActivityClass;
    public final Class<? extends WorkoutActivity> showDetailsActivityClass;

    RecordingType(String id, Class<? extends RecordWorkoutActivity> recorderActivityClass, Class<? extends WorkoutActivity> showDetailsActivityClass) {
        this.id = id;
        this.recorderActivityClass = recorderActivityClass;
        this.showDetailsActivityClass = showDetailsActivityClass;
    }

    public static RecordingType findById(String id) {
        for (RecordingType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return GPS;
    }
}
