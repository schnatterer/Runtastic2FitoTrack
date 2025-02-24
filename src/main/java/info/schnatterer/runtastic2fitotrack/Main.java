package info.schnatterer.runtastic2fitotrack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.tadris.fitness.data.GpsSample;
import de.tadris.fitness.data.GpsWorkout;
import de.tadris.fitness.export.BackupController;
import de.tadris.fitness.export.FitoTrackDataContainer;
import de.tadris.fitness.export.RestoreController;
import de.tadris.fitness.util.AltitudeCorrection;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
                // workout.id = System.nanoTime();
                // We use millis here, even though FitoTrack uses nanos. For both, it's highly unlikely to get interference.
                gpsWorkout.id = resultSet.getLong("endTime"); 
                gpsWorkout.start = resultSet.getLong("startTime"); 
                gpsWorkout.end = resultSet.getLong("endTime"); 
                gpsWorkout.duration = resultSet.getInt("runtime"); 
                gpsWorkout.pauseDuration = resultSet.getInt("pauseInMillis");
                gpsWorkout.workoutTypeId = "running"; // We could match Runtastic's workout types to those defined in WorkoutTypeManager, but since my tracks are only running, I'll skip this step.                //gpsWorkout.avgHeartRate // Never tracked in my data...
                //gpsWorkout.maxHeartRate 
                
                gpsWorkout.length = resultSet.getInt("distance"); 
                // km/h -> m/s 
                gpsWorkout.avgSpeed = resultSet.getDouble("avgSpeed") / 3.6;
                gpsWorkout.topSpeed = resultSet.getDouble("maxSpeed") / 3.6;
                // See WorkoutBuilder
                gpsWorkout.avgPace = ((double) gpsWorkout.duration / 1000 / 60) / ((double) gpsWorkout.length / 1000);
                gpsWorkout.minElevationMSL = resultSet.getFloat("minElevation");
                gpsWorkout.maxElevationMSL = resultSet.getFloat("maxElevation"); 
                gpsWorkout.ascent = resultSet.getFloat("elevationGain"); 
                gpsWorkout.descent = resultSet.getFloat("elevationLoss"); 
                gpsWorkout.comment = resultSet.getString("note"); 
                gpsWorkout.calorie = resultSet.getInt("calories"); 
                // TODO store in comment: Runtastic
                //  shoeId
                // Dehydration?
                // Device from sport-activities DbSportActivitiy.originFeature

                List<GpsSample> gpsSamples = readGpsSamples(resultSet, gpsWorkout.id);
                setMSLElevation(gpsSamples);
                        
                for (GpsSample gpsSample : gpsSamples) {
                    data.getSamples().add(gpsSample);
                }
                data.getWorkouts().add(gpsWorkout);
                
                //String sampleid = resultSet.getString("sampleid");
                //System.out.println("SampleId: " + sampleid + ": Number of GpsSamples found: " + gpsSamples.size());

            }
        }
        new BackupController(data, new File("out.ftb")).exportData();

    }

    private static List<GpsSample> readGpsSamples(ResultSet resultSet, long workoutId) throws SQLException, IOException {
        List<GpsSample> gpsSamples = new LinkedList<>();
        int gpsTraceCount = resultSet.getInt("gpsTraceCount");
        
        if (gpsTraceCount == 0) {

            // Try reading from runtastic export, Sport-sessions/GPS-Data json files
            String sampleid = resultSet.getString("sampleid");
            File sampleFile = findSampleFile(sampleid);
            if (sampleFile != null) {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(sampleFile);

                int i=0;
                for (JsonNode node : rootNode) {
                    GpsSample gpsSample = new GpsSample();
                    gpsSample.id = workoutId + i++;
                    gpsSample.workoutId = workoutId;
                    gpsSample.absoluteTime = node.get("timestamp").asLong();
                    gpsSample.lon = node.get("longitude").floatValue();
                    gpsSample.lat = node.get("latitude").floatValue();
                    gpsSample.elevation = node.get("altitude").floatValue();
                    gpsSample.speed = node.get("speed").floatValue();
                    gpsSample.relativeTime = node.get("duration").asInt();
                    //gpsSample.distance = node.get("distance").asInt();
                    //gpsSample.elevationGain = node.get("elevation_gain").asInt();
                    //gpsSample.elevationLoss = node.get("elevation_loss").asInt();
                    //gpsSample.accuracyV = (byte) node.get("accuracy_v").asInt();
                    //gpsSample.accuracyH = (byte) node.get("accuracy_h").asInt();

                    gpsSamples.add(gpsSample);
                }
            } else {

                String encodedTrace = resultSet.getString("encodedTrace");
                if (encodedTrace != null) {
                    System.out.println("Falling back to encodedTrace for sampleid: " + sampleid);
                    // TODO fall back to encodedTrace? -> Only points but no time, speed, elevation
                    // https://valhalla.github.io/demos/polyline/
                    // https://github.com/scoutant/polyline-decoder/blob/master/src/main/java/org/scoutant/polyline/PolylineDecoder.java
                } else {
                    System.out.println("Workout without gps sampleid: " + sampleid);
                }
            }

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
                // We don't seem to have pressure values

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

    private static File findSampleFile(String sampleid) throws IOException {
        Path dir = Paths.get("Sport-sessions/GPS-data");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*" + sampleid + ".json")) {
            for (Path entry : stream) {
                return entry.toFile();
            }
        }
        return null;
    }
    
    // From GpsWorkoutSaver
    static void setMSLElevation(List<GpsSample> samples) {
        if (samples.size() == 0) return;
        // Set the median sea level elevation value for all samples
        // Please see the AltitudeCorrection.java for more information
        try {
            int lat = (int) Math.round(samples.get(0).lat);
            int lon = (int) Math.round(samples.get(0).lon);
            AltitudeCorrection correction = new AltitudeCorrection(lat, lon);
            for (GpsSample sample : samples) {
                sample.elevationMSL = correction.getHeightOverSeaLevel(sample.elevation);
            }
        } catch (IOException e) {
            // If we can't read the file, we cannot correct the values
            e.printStackTrace();
        }
    }
}