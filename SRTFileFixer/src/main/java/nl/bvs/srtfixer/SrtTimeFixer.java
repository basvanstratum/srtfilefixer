package nl.bvs.srtfixer;

import nl.bvs.srtfixer.util.Constants;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sometimes timing is a bit off. This fixes that, although it does
 * require some tinkering with the amount of time you want to shift.
 */
public class SrtTimeFixer extends BaseFixer {
    /** Regex to find timecode lines and put the [from] and [to] in a separate group. */
    private static final String TIMECODE_REGEX = "^([0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}) --> ([0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3})$";

    /** The amount of milliseconds to add or subtract from all time codes. */
    private static final int NEW_OFFSET_MILLIS = 0;

    public static void main(final String[] args) {
        System.out.println("Processing directory :: " + Constants.TIMEFIXER_FILE_PATH);
        new SrtTimeFixer().process();
    }

    @Override
    protected void process() {
        final File fileToFix = new File(Constants.TIMEFIXER_FILE_PATH);

        if (Constants.MAKE_BACKUPS) {
            backupFile(fileToFix);
        }

        fix(fileToFix);
    }

    @Override
    protected void fixFile(final BufferedReader reader, final BufferedWriter writer) throws Exception {
        String line;
        String fixedLine;
        while ((line = reader.readLine()) != null) {
            if (isTimecodeLine(line)) {
                fixedLine = fixTimecode(line);
            } else {
                fixedLine = line;
            }
            writer.write(fixedLine);
            writer.newLine();
        }
    }

    private boolean isTimecodeLine(final String line) {
        return line.matches(TIMECODE_REGEX);
    }

    private String fixTimecode(final String line) {
        final Matcher matcher = Pattern.compile(TIMECODE_REGEX).matcher(line);
        if (matcher.find()) {
            return modifyTimestamp(matcher.group(1)) + " --> " + modifyTimestamp(matcher.group(2));
        } else {
            return line;
        }
    }

    private String modifyTimestamp(final String timestamp) {
        try {
            final DateFormat format = new SimpleDateFormat("HH:mm:ss,SSS");
            final Date date = format.parse(timestamp);
            final Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.MILLISECOND, NEW_OFFSET_MILLIS);
            return format.format(calendar.getTime());
        } catch (final Exception ex) {
            ex.printStackTrace();
            throw new IllegalStateException("Couldn't modify timestamp :: " + timestamp);
        }
    }
}
