/*
 * Copyright (c) 2023 Jannis Scheibe <jannis@tadris.de>
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

import android.content.Context;

import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Model class representing a workout
 */
public abstract class BaseWorkout {

    /**
     * Unique ID
     */
    @PrimaryKey
    public long id;

    /**
     * Timestamp of start
     */
    public long start;

    /**
     * Timestamp of end
     */
    public long end;

    /**
     * Active duration in ms
     */
    public long duration;

    /**
     * Summed duration of pauses in ms
     */
    public long pauseDuration;

    /**
     * User-defined comment
     */
    public String comment;

    /**
     * Type of workout
     *
     * @see WorkoutType
     * @see WorkoutTypeManager
     */
    @ColumnInfo(name = "workoutType")
    @JsonProperty(value = "workoutType")
    public String workoutTypeId = WorkoutTypeManager.WORKOUT_TYPE_ID_OTHER;

    /**
     * Average heart rate bpm
     * -1 if not available
     */
    @ColumnInfo(name = "avg_heart_rate")
    public int avgHeartRate = -1;

    /**
     * Maximum heart rate in bpm
     * -1 if not available
     */
    @ColumnInfo(name = "max_heart_rate")
    public int maxHeartRate = -1;

    /**
     * Burned calories in kcal
     */
    public int calorie;

    /**
     * Stores if distance/duration/... have been manually edited and can differ from the calculated
     * values.
     */
    public boolean edited;

    /**
     * Used interval set
     * -1 or 0 if none was chosen
     */
    // No usage of an SQL foreign key is intended
    @ColumnInfo(name = "interval_set_used_id")
    public long intervalSetUsedId = 0;

    // returns a file name string without suffix that can be used for exports
    public String getExportFileName() {
        if (!getSafeComment().isEmpty()) {
            return String.format("workout-%s-%s", getSafeDateString(), getSafeComment());
        } else {
            return String.format("workout-%s", getSafeDateString());
        }
    }

    @JsonIgnore
    public String getDateString() {
        return SimpleDateFormat.getDateTimeInstance().format(new Date(start));
    }

    @JsonIgnore
    public String getSafeDateString() {
        return new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(new Date(start));
    }

    @JsonIgnore
    public String getSafeComment() {
        if (comment == null) return "";
        String safeComment = this.comment.replaceAll("[^0-9a-zA-Z-_]+", "_"); // replace all unwanted chars by `_`
        return safeComment.substring(0, Math.min(safeComment.length(), 50)); // cut the comment after 50 Chars
    }

    @JsonIgnore
    public WorkoutType getWorkoutType(Context context) {
        return WorkoutTypeManager.getInstance().getWorkoutTypeById(context, workoutTypeId);
    }

    @JsonIgnore
    public void setWorkoutType(WorkoutType workoutType) {
        this.workoutTypeId = workoutType.id;
    }

    @JsonIgnore
    public boolean hasHeartRateData() {
        return avgHeartRate > 0;
    }

}
