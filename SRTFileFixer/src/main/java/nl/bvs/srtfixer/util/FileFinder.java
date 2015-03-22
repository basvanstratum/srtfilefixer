package nl.bvs.srtfixer.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileFinder {
    public List<File> collect(final File dir, final String extension) {
        return find(dir, extension);
    }

    private List<File> find(final File dir, final String extension) {
        final File[] list = dir.listFiles();

        if (list == null) {
            return Collections.emptyList();
        }

        final List<File> files = new ArrayList<>();

        for (final File f : list) {
            if (f.isDirectory()) {
                files.addAll(find(f, extension));
            } else {
                // assume no files without extension exist
                final String fileExtension = f.getName().substring(f.getName().lastIndexOf('.') + 1);
                if (extension.equalsIgnoreCase(fileExtension)) {
                    files.add(f);
                }
            }
        }

        return files;
    }
}
