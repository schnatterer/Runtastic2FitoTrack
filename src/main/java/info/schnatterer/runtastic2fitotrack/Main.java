package info.schnatterer.runtastic2fitotrack;

import de.tadris.fitness.data.GpsSample;
import de.tadris.fitness.data.GpsWorkout;
import de.tadris.fitness.export.BackupController;
import de.tadris.fitness.export.FitoTrackDataContainer;
import de.tadris.fitness.export.RestoreController;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.LinkedList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        //fitotrackImportExportRoundtrip();

        try {
            readSqlite();
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static void fitotrackImportExportRoundtrip() {
        try {
            FitoTrackDataContainer data = new RestoreController(new File("in.ftb")).restoreData();
            new BackupController(data, new File("out.ftb")).exportData();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("Exported file out.ftb. Unzip to see XML.");
    }

    private static void readSqlite() throws ClassNotFoundException, SQLException, IOException {
        Class.forName("org.sqlite.JDBC");
        FitoTrackDataContainer data = new FitoTrackDataContainer();
        data.setVersion(3);
        try (
                Connection connection = DriverManager.getConnection("jdbc:sqlite:db");
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT * FROM session")
        ) {
            while (resultSet.next()) {
                GpsWorkout gpsWorkout = new GpsWorkout();
                // ID: https://codeberg.org/jannis/FitoTrack/src/tag/v15.6/app/src/main/java/de/tadris/fitness/recording/gps/GpsWorkoutSaver.java#L94
                //workout.id = System.nanoTime();
                gpsWorkout.id = resultSet.getLong("endTime"); // end
                gpsWorkout.start = resultSet.getLong("startTime"); //startTime
                gpsWorkout.end = resultSet.getLong("endTime"); // endTime
                gpsWorkout.duration = resultSet.getInt("runtime"); // runtime
                gpsWorkout.pauseDuration = resultSet.getInt("pauseInMillis"); // pauseInMillis
                gpsWorkout.workoutTypeId = "other"; //TODO
                //gpsWorkout.avgHeartRate //TODO
                //gpsWorkout.maxHeartRate 
                gpsWorkout.length = resultSet.getInt("distance"); //distance
                gpsWorkout.avgSpeed = resultSet.getDouble("avgSpeed"); //avgSpeed
                gpsWorkout.topSpeed = resultSet.getDouble("maxSpeed"); //maxSpeed
                //gpsWorkout.avgPace = resultSet.getDouble("avgPace"); // TODO calculate?
                gpsWorkout.minElevationMSL = resultSet.getFloat("minElevation"); //minElevation
                gpsWorkout.maxElevationMSL = resultSet.getFloat("maxElevation"); //maxElevation
                gpsWorkout.ascent = resultSet.getFloat("elevationGain"); // elevationGain
                gpsWorkout.descent = resultSet.getFloat("elevationLoss"); // elevationLoss
                gpsWorkout.comment = resultSet.getString("note"); //note
                // TODO store in comment: Runtastic
                //  shoeId
                // Dehydration?
                // Device from sport-activities DbSportActivitiy.originFeature

                // TODO encodedTrace
                // https://valhalla.github.io/demos/polyline/
                // https://github.com/scoutant/polyline-decoder/blob/master/src/main/java/org/scoutant/polyline/PolylineDecoder.java

                List<GpsSample> gpsSamples = readGpsSamples(resultSet, gpsWorkout.id);
                
                for (GpsSample gpsSample : gpsSamples) {
                    data.getSamples().add(gpsSample);
                }
                data.getWorkouts().add(gpsWorkout);
                
                String sampleid = resultSet.getString("sampleid");
                System.out.println("SampleId: " + sampleid + ": Number of GpsSamples found: " + gpsSamples.size());

            }
        }
        new BackupController(data, new File("out.ftb")).exportData();

    }

    private static List<GpsSample> readGpsSamples(ResultSet resultSet, long workoutId) throws SQLException, IOException {
        List<GpsSample> gpsSamples = new LinkedList<>();
        int gpsTraceCount = resultSet.getInt("gpsTraceCount");
        if (gpsTraceCount == 0) {
            return gpsSamples;
        }
        try (DataInputStream dataInputStream = new DataInputStream(
                resultSet.getBinaryStream("gpsTrace"))) {
            // Unknown. Header? Version?
            dataInputStream.readInt();

            for (int i = 0; i < gpsTraceCount; i++) {
                GpsSample gpsSample = new GpsSample();
                //https://codeberg.org/jannis/FitoTrack/src/tag/v15.6/app/src/main/java/de/tadris/fitness/recording/gps/GpsWorkoutSaver.java#L71-L74                        
                gpsSample.id = workoutId + i;
                gpsSample.workoutId = workoutId;
                gpsSample.absoluteTime = dataInputStream.readLong();
                gpsSample.lon = dataInputStream.readFloat();
                gpsSample.lat = dataInputStream.readFloat();
                gpsSample.elevation = dataInputStream.readFloat();
                byte[] accuracyV = new byte[1];
                dataInputStream.read(accuracyV);
                byte[] accuracyH = new byte[1];
                dataInputStream.read(accuracyH);
                float speed = dataInputStream.readFloat();
                gpsSample.speed = speed;
                int duration = dataInputStream.readInt();
                gpsSample.relativeTime = duration;
                int distance = dataInputStream.readInt();
                int elevationGain = dataInputStream.readShort();
                int elevationLoss = dataInputStream.readShort();

                gpsSamples.add(gpsSample);
                /* JSON export contains the following data:
                    "timestamp": 1561402263180,
                    "longitude": x.123456789000000,
                    "latitude": y.123456789000000,
                    "altitude": z.0,
                    "accuracy_v": 0,
                    "accuracy_h": 3,
                    "speed": 12.0159330368042,
                    "duration": 16180,
                    "distance": 39,
                    "elevation_gain": 0,
                    "elevation_loss": 6
                */
            }
        }
        return gpsSamples;
    }
}