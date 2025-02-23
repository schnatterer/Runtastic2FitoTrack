package info.schnatterer.runtastic2fitotrack;

import de.tadris.fitness.data.GpsSample;
import de.tadris.fitness.export.BackupController;
import de.tadris.fitness.export.FitoTrackDataContainer;
import de.tadris.fitness.export.RestoreController;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.sql.*;

public class Main {
    public static void main(String[] args) throws SQLException, IOException, ClassNotFoundException {
        //fitotrackImportExportRoundtrip();

        readSqlite();
        // ID: https://codeberg.org/jannis/FitoTrack/src/tag/v15.6/app/src/main/java/de/tadris/fitness/recording/gps/GpsWorkoutSaver.java#L94

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
        try (
                Connection connection = DriverManager.getConnection("jdbc:sqlite:db");
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT * FROM session where gpsTraceCount > 1 limit 1")
        ) {
            while (resultSet.next()) {
                
                String sampleid = resultSet.getString("sampleid");
                int gpsTraceCount = resultSet.getInt("gpsTraceCount");

                System.out.println("SampleId: " + sampleid + ": Number of GPS traces found: " + gpsTraceCount);
                try (DataInputStream dataInputStream = new DataInputStream(
                        resultSet.getBinaryStream("gpsTrace"))) {
                    // Unknown. Header? Version?
                    dataInputStream.readInt();

                    for (int i = 0; i < gpsTraceCount; i++) {
                        // TODO ID
                        GpsSample gpsSample = new GpsSample();
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
                        
                        System.out.println("Timestamp: " + gpsSample.absoluteTime);
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
            }
        }
    }
}