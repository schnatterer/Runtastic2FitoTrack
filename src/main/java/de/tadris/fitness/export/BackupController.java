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

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import de.tadris.fitness.Instance;
import de.tadris.fitness.R;
import de.tadris.fitness.data.AppDatabase;
import de.tadris.fitness.data.Interval;
import de.tadris.fitness.data.IntervalSet;

public class BackupController {

    static final int VERSION = 3;

    private final Context context;
    private final File output;
    private final ExportStatusListener listener;
    private AppDatabase database;

    private FitoTrackDataContainer dataContainer;

    public BackupController(Context context, File output, ExportStatusListener listener) {
        this.context = context;
        this.output = output;
        this.listener = listener;
    }

    public void exportData() throws IOException {
        listener.onStatusChanged(0, context.getString(R.string.initialising));
        init();
        listener.onStatusChanged(5, context.getString(R.string.workoutRecordingTypeGps));
        saveGpsWorkoutsToContainer();
        listener.onStatusChanged(20, context.getString(R.string.locationData));
        saveGpsSamplesToContainer();
        listener.onStatusChanged(40, context.getString(R.string.workoutRecordingTypeIndoor));
        saveIndoorWorkoutsToContainer();
        listener.onStatusChanged(45, context.getString(R.string.workoutRecordingTypeIndoor));
        saveIndoorSamplesToContainer();
        listener.onStatusChanged(50, context.getString(R.string.intervalSets));
        saveIntervalsToContainer();
        listener.onStatusChanged(55, context.getString(R.string.customWorkoutTypesTitle));
        saveWorkoutTypes();
        listener.onStatusChanged(60, context.getString(R.string.converting));
        writeContainerToOutputFile();
        listener.onStatusChanged(100, context.getString(R.string.finished));
    }

    private void init(){
        database= Instance.getInstance(context).db;
        newContainer();
    }

    private void newContainer(){
        dataContainer= new FitoTrackDataContainer();
        dataContainer.setVersion(VERSION);
    }

    private void saveGpsWorkoutsToContainer() {
        dataContainer.getWorkouts().addAll(Arrays.asList(database.gpsWorkoutDao().getWorkouts()));
    }

    private void saveGpsSamplesToContainer() {
        dataContainer.getSamples().addAll(Arrays.asList(database.gpsWorkoutDao().getSamples()));
    }

    private void saveIndoorWorkoutsToContainer() {
        dataContainer.getIndoorWorkouts().addAll(Arrays.asList(database.indoorWorkoutDao().getWorkouts()));
    }

    private void saveIndoorSamplesToContainer() {
        dataContainer.getIndoorSamples().addAll(Arrays.asList(database.indoorWorkoutDao().getSamples()));
    }

    private void saveIntervalsToContainer() {
        for (IntervalSet set : database.intervalDao().getAllSets()) {
            saveIntervalToContainer(set);
        }
    }

    private void saveIntervalToContainer(IntervalSet set) {
        List<Interval> intervals = Arrays.asList(database.intervalDao().getAllIntervalsOfSet(set.id));
        dataContainer.getIntervalSets().add(new IntervalSetContainer(set, intervals));
    }

    private void saveWorkoutTypes() {
        dataContainer.getWorkoutTypes().addAll(Arrays.asList(database.workoutTypeDao().findAll()));
    }

    private void writeContainerToOutputFile() throws IOException {
        XmlMapper mapper = new XmlMapper();
        FileOutputStream out = new FileOutputStream(output);
        ZipOutputStream zipOut = new ZipOutputStream(out);
        zipOut.putNextEntry(new ZipEntry("data.xml"));
        mapper.writeValue(zipOut, dataContainer);
        zipOut.close();
        out.close();
    }

    public interface ExportStatusListener {

        ExportStatusListener DUMMY = (progress, action) -> {
        };

        void onStatusChanged(int progress, String action);

    }

}
