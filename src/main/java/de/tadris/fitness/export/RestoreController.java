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

package de.tadris.fitness.export;

import android.content.Context;
import android.net.Uri;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

import de.tadris.fitness.Instance;
import de.tadris.fitness.R;
import de.tadris.fitness.data.AppDatabase;
import de.tadris.fitness.data.GpsSample;
import de.tadris.fitness.data.GpsWorkout;
import de.tadris.fitness.data.IndoorSample;
import de.tadris.fitness.data.IndoorWorkout;
import de.tadris.fitness.data.Interval;
import de.tadris.fitness.data.IntervalSet;
import de.tadris.fitness.data.WorkoutType;
import de.tadris.fitness.data.migration.Migration;
import de.tadris.fitness.data.migration.Migration12IntervalSets;

public class RestoreController {

    private final Context context;
    private final Uri input;
    private final ImportStatusListener listener;
    private final boolean replace;
    private FitoTrackDataContainer dataContainer;
    private final AppDatabase database;

    public RestoreController(Context context, Uri input, boolean replace, ImportStatusListener listener) {
        this.context = context;
        this.input = input;
        this.replace = replace;
        this.listener = listener;
        this.database = Instance.getInstance(context).db;
    }

    public void restoreData() throws IOException, UnsupportedVersionException {
        listener.onStatusChanged(0, context.getString(R.string.loadingFile));
        loadDataFromFile();
        checkVersion();
        restoreDatabase();
        listener.onStatusChanged(100, context.getString(R.string.finished));
    }

    private void loadDataFromFile() throws IOException {
        InputStream stream = context.getContentResolver().openInputStream(input);
        boolean isZIP = stream.read() == 0x50; // Zip Magic number
        stream.close();
        stream = context.getContentResolver().openInputStream(input);
        InputStream decompressedInput;
        if (isZIP) {
            ZipInputStream zipIn = new ZipInputStream(stream);
            zipIn.getNextEntry();
            decompressedInput = zipIn;
        } else {
            decompressedInput = stream;
        }
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure(JsonParser.Feature.IGNORE_UNDEFINED, true);
        dataContainer = xmlMapper.readValue(decompressedInput, FitoTrackDataContainer.class);
        decompressedInput.close();
        stream.close();
    }

    private void checkVersion() throws UnsupportedVersionException {
        if (dataContainer.getVersion() > BackupController.VERSION) {
            throw new UnsupportedVersionException("Version Code" + dataContainer.getVersion() + " is unsupported!");
        }
    }

    private void restoreDatabase() {
        database.runInTransaction(() -> {
            if (replace) {
                resetDatabase();
            }
            listener.onStatusChanged(40, context.getString(R.string.workoutRecordingTypeGps));
            restoreGpsWorkouts();
            listener.onStatusChanged(50, context.getString(R.string.locationData));
            restoreGpsSamples();
            listener.onStatusChanged(65, context.getString(R.string.workoutRecordingTypeIndoor));
            restoreIndoorWorkouts();
            listener.onStatusChanged(70, context.getString(R.string.workoutRecordingTypeIndoor));
            restoreIndoorSamples();
            listener.onStatusChanged(80, context.getString(R.string.intervalSets));
            restoreIntervalSets();
            listener.onStatusChanged(85, context.getString(R.string.customWorkoutTypesTitle));
            restoreWorkoutTypes();
            listener.onStatusChanged(90, context.getString(R.string.runningMigrations));
            runMigrations();
        });
    }

    private void resetDatabase() {
        database.clearAllTables();
    }

    private void restoreGpsWorkouts() {
        if (dataContainer.getWorkouts() != null) {
            for (GpsWorkout workout : dataContainer.getWorkouts()) {
                // Only Import Unknown Workouts on merge
                if (replace || database.gpsWorkoutDao().findById(workout.id) == null) {
                    database.gpsWorkoutDao().insertWorkout(workout);
                }
            }
        }
    }

    private void restoreGpsSamples() {
        if (dataContainer.getSamples() != null) {
            for (GpsSample sample : dataContainer.getSamples()) {
                // Only import unknown samples with known workout on merge
                // Query not necessary on replace because data was cleared
                if (replace || (database.gpsWorkoutDao().findById(sample.workoutId) != null &&
                        database.gpsWorkoutDao().findSampleById(sample.id) == null)) {
                    database.gpsWorkoutDao().insertSample(sample);
                }
            }
        }
    }

    private void restoreIndoorWorkouts() {
        if (dataContainer.getIndoorWorkouts() != null) {
            for (IndoorWorkout workout : dataContainer.getIndoorWorkouts()) {
                // Only Import Unknown Workouts on merge
                if (replace || database.indoorWorkoutDao().findById(workout.id) == null) {
                    database.indoorWorkoutDao().insertWorkout(workout);
                }
            }
        }
    }

    private void restoreIndoorSamples() {
        if (dataContainer.getIndoorSamples() != null) {
            for (IndoorSample sample : dataContainer.getIndoorSamples()) {
                // Only import unknown samples with known workout on merge
                // Query not necessary on replace because data was cleared
                if (replace || (database.indoorWorkoutDao().findById(sample.workoutId) != null &&
                        database.indoorWorkoutDao().findSampleById(sample.id) == null)) {
                    database.indoorWorkoutDao().insertSample(sample);
                }
            }
        }
    }

    private void restoreIntervalSets() {
        if (dataContainer.getIntervalSets() != null) {
            for (IntervalSetContainer container : dataContainer.getIntervalSets()) {
                restoreIntervalSet(container);
            }
        }
    }

    private void restoreIntervalSet(IntervalSetContainer container) {
        IntervalSet set = container.getSet();
        // Only Import unknownInterval Sets
        if(database.intervalDao().getSet(set.id) == null) {
            database.intervalDao().insertIntervalSet(set);
        }
        if (container.getIntervals() != null) {
            for (Interval interval : container.getIntervals()) {
                // Only Import Unknown Intervals
                if (database.intervalDao().findById(interval.id) == null) {
                    database.intervalDao().insertInterval(interval);
                }
            }
        }
    }

    private void restoreWorkoutTypes() {
        if (dataContainer.getWorkoutTypes() != null) {
            for (WorkoutType type : dataContainer.getWorkoutTypes()) {
                // Only import unknown workout types
                if (database.workoutTypeDao().findById(type.id) == null) {
                    database.workoutTypeDao().insert(type);
                }
            }
        }
    }

    private void runMigrations() {
        if (dataContainer.getVersion() <= 1) {
            for (GpsWorkout workout : dataContainer.getWorkouts()) {
                float minHeight = 0f;
                float maxHeight = 0f;
                for (GpsSample sample : database.gpsWorkoutDao().getAllSamplesOfWorkout(workout.id)) {
                    if (minHeight == 0) {
                        minHeight = (float) sample.elevationMSL;
                        maxHeight = (float) sample.elevationMSL;
                    }
                    minHeight = Math.min(minHeight, (float) sample.elevationMSL);
                    maxHeight = Math.max(maxHeight, (float) sample.elevationMSL);
                }
                workout.minElevationMSL = minHeight;
                workout.maxElevationMSL = maxHeight;
                database.gpsWorkoutDao().updateWorkout(workout);
            }
        }
        if (dataContainer.getVersion() <= 2) {
            Migration12IntervalSets migration = new Migration12IntervalSets(context, Migration.DUMMY_LISTENER);
            for (GpsWorkout workout : dataContainer.getWorkouts()) {
                migration.migrateWorkout(workout);
            }
        }
    }

    public interface ImportStatusListener {
        void onStatusChanged(int progress, String action);
    }

    static class UnsupportedVersionException extends Exception {
        UnsupportedVersionException(String message) {
            super(message);
        }
    }

}
