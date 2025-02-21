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

package de.tadris.fitness.export;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.ArrayList;
import java.util.List;

import de.tadris.fitness.data.GpsSample;
import de.tadris.fitness.data.GpsWorkout;
import de.tadris.fitness.data.IndoorSample;
import de.tadris.fitness.data.IndoorWorkout;
import de.tadris.fitness.data.WorkoutType;

@JacksonXmlRootElement(localName = "fito-track")
@JsonIgnoreProperties(ignoreUnknown = true)
class FitoTrackDataContainer {

    private int version;
    private List<GpsWorkout> workouts = new ArrayList<>();
    private List<GpsSample> samples = new ArrayList<>();
    private List<IndoorWorkout> indoorWorkouts = new ArrayList<>();
    private List<IndoorSample> indoorSamples = new ArrayList<>();
    private List<IntervalSetContainer> intervalSets = new ArrayList<>();
    private List<WorkoutType> workoutTypes = new ArrayList<>();

    public FitoTrackDataContainer() {
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public List<GpsWorkout> getWorkouts() {
        return workouts;
    }

    public void setWorkouts(List<GpsWorkout> workouts) {
        this.workouts = workouts;
    }

    public List<GpsSample> getSamples() {
        return samples;
    }

    public void setSamples(List<GpsSample> samples) {
        this.samples = samples;
    }

    public List<IntervalSetContainer> getIntervalSets() {
        return intervalSets;
    }

    public void setIntervalSets(List<IntervalSetContainer> intervalSets) {
        this.intervalSets = intervalSets;
    }

    public List<WorkoutType> getWorkoutTypes() {
        return workoutTypes;
    }

    public void setWorkoutTypes(List<WorkoutType> workoutTypes) {
        this.workoutTypes = workoutTypes;
    }

    public List<IndoorWorkout> getIndoorWorkouts() {
        return indoorWorkouts;
    }

    public void setIndoorWorkouts(List<IndoorWorkout> indoorWorkouts) {
        this.indoorWorkouts = indoorWorkouts;
    }

    public List<IndoorSample> getIndoorSamples() {
        return indoorSamples;
    }

    public void setIndoorSamples(List<IndoorSample> indoorSamples) {
        this.indoorSamples = indoorSamples;
    }
}
