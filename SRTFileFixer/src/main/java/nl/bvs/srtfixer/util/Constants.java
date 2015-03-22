package nl.bvs.srtfixer.util;

public class Constants {
    /** The root directory containing the SRT file(s) to process. It will recursively collect items from sub dirs too. */
    public static final String ROOT_DIR = "[where to look for srt files]";
    public static final String SRT_EXTENSION = "srt";

    /** Flag to enable/disable the making of backups. */
    public static final boolean MAKE_BACKUPS = true;
    /** In case you do not trust the code, a backup of the original SRT file is placed here. If backup is enabled. */
    public static final String BACKUP_DIR = "[where to place backup srt files]";

    /** Flag to enable/disable the removal of italic/bold/br tags. */
    public static final boolean REMOVE_HTML_TAGS = true;
    /** Flag to enable/disable the change of '`' quotes to regular single quotes. And some other weird quotes too. */
    public static final boolean CHANGE_QUOTES = true;
}
