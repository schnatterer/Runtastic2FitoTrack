package info.schnatterer.runtastic2fitotrack;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Main {

    public static final String JDBC_SQLITE_DB = "jdbc:sqlite:db";
    public static final String JDBC_SQLITE_SPORT_ACTIVITIES = "jdbc:sqlite:sport-activities";

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

        Map<String, String> shoes = queryShoes();
        queryWorkouts(data, shoes);

        new BackupController(data, new File("out.ftb")).exportData();

    }

    private static Map<String, String> queryShoes() throws SQLException {
        Map<String, String> shoes = new HashMap<>();
        try (
                Connection connection = DriverManager.getConnection(JDBC_SQLITE_DB);
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT * FROM Equipment")
        ) {
            while (resultSet.next()) {
                String name = resultSet.getString("name");
                String vendor = resultSet.getString("serverVendorName");
                String model = resultSet.getString("serverEquipmentThumbnailUrl");
                shoes.put(resultSet.getString("id"), name == null ? vendor + " " + model : vendor + " " + name);
            }
        }
        return shoes;
    }

    private static void queryWorkouts(FitoTrackDataContainer data, Map<String, String> shoes) throws SQLException, IOException {
        try (
                Connection connection = DriverManager.getConnection(JDBC_SQLITE_DB);
                Statement statement = connection.createStatement();
                // order by technically unnecessary but helps debugging
                ResultSet resultSet = statement.executeQuery("SELECT * FROM session order by endTime desc");
                Connection sportActivitiesDb = DriverManager.getConnection(JDBC_SQLITE_SPORT_ACTIVITIES);
                PreparedStatement sportActivitiesDbStatement = sportActivitiesDb.prepareStatement("SELECT * FROM DbSportActivity WHERE id = ?");
        ) {
            while (resultSet.next()) {
                String sampleid = resultSet.getString("sampleid");
                if (sampleid == null) {
                    // There seem to be some rows that don't contain any valid data. Discard them for better data quality
                    continue;
                }

                /* Query matching row from sportActivities database*/
                sportActivitiesDbStatement.setString(1, sampleid);
                ResultSet sportActivitiesDbResultSet = sportActivitiesDbStatement.executeQuery();
                sportActivitiesDbResultSet.next(); // Pick the first row, if any.

                GpsWorkout gpsWorkout = new GpsWorkout();
                // ID: https://codeberg.org/jannis/FitoTrack/src/tag/v15.6/app/src/main/java/de/tadris/fitness/recording/gps/GpsWorkoutSaver.java#L94
                // workout.id = System.nanoTime();
                // We use millis here, even though FitoTrack uses nanos. For both, it's highly unlikely to get interference.
                gpsWorkout.id = resultSet.getLong("endTime");
                gpsWorkout.start = resultSet.getLong("startTime");
                gpsWorkout.end = resultSet.getLong("endTime");
                gpsWorkout.duration = resultSet.getInt("runtime");
                gpsWorkout.pauseDuration = resultSet.getInt("pauseInMillis");
                    
                // gpsWorkout.avgHeartRate // Never tracked in my data...
                //gpsWorkout.maxHeartRate 

                gpsWorkout.length = resultSet.getInt("distance");
                // km/h -> m/s
                gpsWorkout.avgSpeed = resultSet.getDouble("avgSpeed") / 3.6;
                if (gpsWorkout.avgSpeed <= 0 && gpsWorkout.length > 0) {
                    // In my data runtastic stopped providing avgSpeed around 2019/01
                    // See WorkoutBuilder
                    gpsWorkout.avgSpeed = (double) gpsWorkout.length / (double) (gpsWorkout.duration / 1000);
                }
                gpsWorkout.topSpeed = resultSet.getDouble("maxSpeed") / 3.6;
                if (gpsWorkout.length > 0) {
                    // Division by zero causes result to be infinite, which likely is the reason for not null constraint to hit on import
                    // See WorkoutBuilder
                    gpsWorkout.avgPace = ((double) gpsWorkout.duration / 1000 / 60) / ((double) gpsWorkout.length / 1000);
                }
                gpsWorkout.minElevationMSL = resultSet.getFloat("minElevation");
                gpsWorkout.maxElevationMSL = resultSet.getFloat("maxElevation");
                gpsWorkout.ascent = resultSet.getFloat("elevationGain");
                gpsWorkout.descent = resultSet.getFloat("elevationLoss");
                gpsWorkout.calorie = resultSet.getInt("calories");
                
                // Stitch all additional data into the comment field
                StringBuilder comment = new StringBuilder("Runtastic");
                String originalComment = resultSet.getString("note");
                String shoe = shoes.get(resultSet.getString("shoeId"));
                if (shoe == null) {
                    shoe = tryToFindShoeInSportActivitiesDb(shoes, sportActivitiesDbResultSet);
                }
                String weather = tryToFindWeatherInSportActivitiesDb(sportActivitiesDbResultSet);
                Integer dehydration = sportActivitiesDbResultSet.getInt("dehydrationVolume");
                String subjectiveFeeling = sportActivitiesDbResultSet.getString("subjectiveFeeling");
                String surface = tryToFindSurfaceInSportActivitiesDb(sportActivitiesDbResultSet);
                String device = tryToFindDeviceInSportActivitiesDb(sportActivitiesDbResultSet);
                
                if (originalComment != null && !originalComment.isBlank()) {
                    comment.append(";\nNote: ").append(originalComment);
                }
                if (shoe != null && !shoe.isBlank()) {
                    comment.append(";\nShoe: ").append(shoe);
                }
                if (!weather.isBlank()) {
                    comment.append(";\nWheather: ").append(weather);
                }
                if (dehydration > 0) {
                    comment.append(";\nDehydration: ").append(dehydration);
                }
                if (subjectiveFeeling != null) {
                    comment.append(";\nSubjective Feeling: ").append(subjectiveFeeling);
                }
                if (surface != null && !surface.isBlank()) {
                    comment.append(";\nSurface: ").append(surface);
                }
                if (!device.isBlank()) {
                    comment.append(";\nDevice: ").append(device);
                }
                gpsWorkout.comment = comment.toString();
                
                RuntasticSportType sportType = RuntasticSportType.fromId(resultSet.getInt("sportType"));
                gpsWorkout.workoutTypeId = sportType.toFitoTrack();

                /*if (sportType != RuntasticSportType.RUNNING) {
                    continue; // Use this instance of FitoTrack only for running
                } else {
                    if (shoe != null && !shoe.isBlank()) {
                        String workoutTypeId = shoe
                                .replace(" ", "-")
                                .replaceAll("[^a-zA-Z0-9-]", "")
                                .replaceAll("-$", ""); // Trailing dashes
                        gpsWorkout.workoutTypeId = workoutTypeId;
                        if (workoutTypes.get(workoutTypeId) == null) {
                            WorkoutType workoutType = new WorkoutType();
                            workoutType.id = workoutTypeId;
                            workoutType.title = shoe;
                            // 7 mph = 11 kmh seems like a sensible average -> MET of 11
                            // See METFunctionsProvider
                            // Unfortunately, using shoe as workaround for activity causes this fixed MET to be used 
                            // instead of the MET depending on the avg speed for WORKOUT_TYPE_ID_RUNNING 🙁
                            // We could track as running and then change retrospectively
                            workoutType.MET = 11; 
                            data.getWorkoutTypes().add(workoutType);
                            workoutTypes.put(workoutTypeId, workoutType);
                        }
                    }
                }*/

                List<GpsSample> gpsSamples = readGpsSamples(resultSet, gpsWorkout.id);
                setMSLElevation(gpsSamples);

                for (GpsSample gpsSample : gpsSamples) {
                    data.getSamples().add(gpsSample);
                }
                data.getWorkouts().add(gpsWorkout);
                //String sampleid = resultSet.getString("sampleid");
/*                if (sportType != RuntasticSportType.RUNNING) {
                    System.out.println("SampleId: " + sampleid + ": SPort type: " + sportType);
                }*/
                //System.out.println("SampleId: " + sampleid + ": Number of GpsSamples found: " + gpsSamples.size());

            }
        }
    }

    private static String tryToFindSurfaceInSportActivitiesDb(ResultSet sportActivitiesDbResultSet) throws SQLException, JsonProcessingException {
        String surface = null;
        
        String trackFeatureJson = sportActivitiesDbResultSet.getString("trackMetricsFeature");
        if (trackFeatureJson != null) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(trackFeatureJson);

            JsonNode surfaceNode = rootNode.path("surface");
            if (!surfaceNode.isMissingNode()) {
                surface = surfaceNode.asText();
            }
        }
        return surface;
    }

    private static String tryToFindWeatherInSportActivitiesDb(ResultSet sportActivitiesDbResultSet) throws SQLException, JsonProcessingException {
        StringBuilder weatherDetails = new StringBuilder();

        String weatherFeatureJson = sportActivitiesDbResultSet.getString("weatherFeature");
        if (weatherFeatureJson != null) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(weatherFeatureJson);

            JsonNode conditionsNode = rootNode.path("conditions");
            if (!conditionsNode.isMissingNode()) {
                weatherDetails.append("Conditions: ").append(conditionsNode.asText()).append(". ");
            }

            JsonNode humidityNode = rootNode.path("humidity");
            if (!humidityNode.isMissingNode()) {
                weatherDetails.append("Humidity: ").append(humidityNode.asDouble()).append("%").append(". ");
            }

            JsonNode temperatureNode = rootNode.path("temperature");
            if (!temperatureNode.isMissingNode()) {
                weatherDetails.append("Temperature: ").append(temperatureNode.asDouble()).append("°C").append(". ");
            }

            JsonNode windDirectionNode = rootNode.path("windDirection");
            if (!windDirectionNode.isMissingNode()) {
                weatherDetails.append("Wind Direction: ").append(windDirectionNode.asDouble()).append("°").append(". ");
            }

            JsonNode windSpeedNode = rootNode.path("windSpeed");
            if (!windSpeedNode.isMissingNode()) {
                weatherDetails.append("Wind Speed: ").append(windSpeedNode.asDouble()).append(" m/s").append(". ");
            }
        }
        return weatherDetails.toString().trim();
    }

    private static String tryToFindDeviceInSportActivitiesDb(ResultSet sportActivitiesDbResultSet) throws SQLException, JsonProcessingException {
        StringBuilder deviceDetails = new StringBuilder();

        String deviceFeatureJson = sportActivitiesDbResultSet.getString("originFeature");
        if (deviceFeatureJson != null) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(deviceFeatureJson);

            JsonNode deviceNode = rootNode.path("device");
            if (!deviceNode.isMissingNode()) {
                JsonNode nameNode = deviceNode.path("name");
                JsonNode osVersionNode = deviceNode.path("osVersion");
                JsonNode vendorNode = deviceNode.path("vendor");

                if (!nameNode.isMissingNode()) {
                    deviceDetails.append("Name: ").append(nameNode.asText()).append(". ");
                }
                if (!osVersionNode.isMissingNode()) {
                    deviceDetails.append("OS Version: ").append(osVersionNode.asText()).append(". ");
                }
                if (!vendorNode.isMissingNode()) {
                    deviceDetails.append("Vendor: ").append(vendorNode.asText()).append(". ");
                }
            }
        }
        return deviceDetails.toString().trim();
    }

    private static String tryToFindShoeInSportActivitiesDb(Map<String, String> shoes, ResultSet sportActivitiesDbResultSet) throws SQLException, JsonProcessingException {
        String shoe = null;

        String equipmentFeatureJson = sportActivitiesDbResultSet.getString("equipmentFeature");
        if (equipmentFeatureJson != null) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(equipmentFeatureJson);
            JsonNode idNode = rootNode.path("userEquipment").get(0).path("id");
            if (!idNode.isMissingNode()) {
                shoe = shoes.get(idNode.asText());
                //System.out.println("Found missing shoe in SportActivity DB. For sampleId=" + sampleid + ". Shoe:" + shoe);
            }
        }
        return shoe;
    }

    private static List<GpsSample> readGpsSamples(ResultSet resultSet, long workoutId) throws
            SQLException, IOException {
        List<GpsSample> gpsSamples = new LinkedList<>();
        int gpsTraceCount = resultSet.getInt("gpsTraceCount");

        if (gpsTraceCount == 0) {

            // Try reading from runtastic export, Sport-sessions/GPS-Data json files
            String sampleid = resultSet.getString("sampleid");
            File sampleFile = findSampleFile(sampleid);
            if (sampleFile != null) {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(sampleFile);

                int i = 0;
                for (JsonNode node : rootNode) {
                    GpsSample gpsSample = new GpsSample();
                    gpsSample.id = workoutId + i++;
                    gpsSample.workoutId = workoutId;
                    gpsSample.absoluteTime = node.get("timestamp").asLong();
                    gpsSample.lon = node.get("longitude").floatValue();
                    gpsSample.lat = node.get("latitude").floatValue();
                    gpsSample.elevation = node.get("altitude").floatValue();
                    // km/h -> m/s 
                    gpsSample.speed = node.get("speed").floatValue() / 3.6;
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
                    System.out.println("We could fall back to encodedTrace for sampleid: " + sampleid);
                    // fall back to encodedTrace? -> Only points but no time, speed, elevation
                    // This was never necessary in my data
                    // https://valhalla.github.io/demos/polyline/
                    // https://github.com/scoutant/polyline-decoder/blob/master/src/main/java/org/scoutant/polyline/PolylineDecoder.java
                } 
/*                else {
                    System.out.println("Workout without gps sampleid: " + sampleid);
                }*/
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
                gpsSample.speed = speed / 3.6;
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