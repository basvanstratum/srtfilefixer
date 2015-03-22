package nl.bvs.srtfixer.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class BackupUtil {
    public BackupUtil() {
        if (Constants.MAKE_BACKUPS) {
            final File backupDir = getBackupDir();
            final boolean tempDirMade = backupDir.mkdirs();
            if (!tempDirMade && !backupDir.exists()) {
                System.err.println("Failed to make the temp dir.");
                throw new IllegalStateException("I wanted backups. Not getting any, so kaboom!");
            }
        }
    }

    private File getBackupDir() {
        return new File(Constants.BACKUP_DIR);
    }

    /**
     * Makes a backup of the given file.
     * @param fileToBackup the file to backup
     */
    public void makeBackup(final File fileToBackup) {
        final File backupDir = getBackupDir();

        // make a backup
        final Path source = Paths.get(fileToBackup.getAbsolutePath());
        final Path destination = Paths.get(backupDir.getAbsolutePath() + File.separator + fileToBackup.getName());
        try {
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // stuff may go wrong sometimes - e.g. locked files - tough luck
            e.printStackTrace();
        }
    }
}
