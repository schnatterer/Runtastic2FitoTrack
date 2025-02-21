package info.schnatterer.runtastic2fitotrack;

import de.tadris.fitness.export.BackupController;
import de.tadris.fitness.export.FitoTrackDataContainer;
import de.tadris.fitness.export.RestoreController;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        try {
            FitoTrackDataContainer data = new RestoreController(new File("in.ftb")).restoreData();
            new BackupController(data, new File("out.ftb")).exportData();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("Exported file out.ftb. Unzip to see XML.");
    }
}