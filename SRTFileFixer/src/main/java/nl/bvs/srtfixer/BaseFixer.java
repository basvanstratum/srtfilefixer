package nl.bvs.srtfixer;

import nl.bvs.srtfixer.util.BackupUtil;

import java.io.*;

public abstract class BaseFixer {
    /** Helps backing up files in case you're not feeling very confident. */
    private static final BackupUtil BACKUP_UTIL = new BackupUtil();

    protected abstract void process();
    protected abstract void fixFile(final BufferedReader reader, final BufferedWriter writer) throws Exception;

    protected final void fix(final File fileToFix) {
        if (!fileToFix.exists()) {
            System.err.println("File does not exist :: " + fileToFix.getPath());
            return;
        }

        // initialise
        final String rootDir = fileToFix.getParent();
        final String oldFileName = rootDir + File.separator + fileToFix.getName();
        final String tmpFileName = rootDir + File.separator + "tmp_" + fileToFix.getName();

        try (BufferedReader reader = new BufferedReader(new FileReader(oldFileName));
             BufferedWriter writer = new BufferedWriter(new FileWriter(tmpFileName))) {
            fixFile(reader, writer);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // Once everything is complete, delete old file..
        final File oldFile = new File(oldFileName);
        final boolean deleted = oldFile.delete();
        if (!deleted) {
            System.err.println("Error deleting the old file.");
        }

        // And rename tmp file's name to old file name
        final File newFile = new File(tmpFileName);
        final boolean renamed = newFile.renameTo(oldFile);
        if (!renamed) {
            System.err.println("Error renaming the temp file.");
        }
    }

    protected final void backupFile(final File fileToBackup) {
        BACKUP_UTIL.makeBackup(fileToBackup);
    }
}
