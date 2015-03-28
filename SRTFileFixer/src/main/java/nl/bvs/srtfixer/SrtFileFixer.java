package nl.bvs.srtfixer;

import nl.bvs.srtfixer.util.Constants;
import nl.bvs.srtfixer.util.FileFinder;

import java.io.*;
import java.util.Arrays;
import java.util.List;

/**
 * Rather hacky code to fix subtitle files, but it works well enough. It is
 * tailored to my TV, streaming apps and personal preference. It performs fine,
 * so I can't be bothered with optimising the code.
 *
 * SRT files are often ripped from images by OCR software. A lot of the time,
 * the upper case 'I' and lower case 'l' are mixed up by such processes. This
 * can cause annoyance when reading the subtitles.
 *
 * This little class tries to fix many of these issues. And usually it fixes way
 * more than it breaks, so yay!
 */
public class SrtFileFixer extends BaseFixer {
    /** Lines can start with various things such as dashes or quotes. */
    private static final String[] LINE_STARTS = {"", "\"", "-", "--", " -", " --", "- ", "-- ", " - ", " -- ",
            "-\"", "--\"", " -\"", " --\"", "- \"", "-- \"", " - \"", " -- \"", "[", " [", " [ ", "(", " (", " ( "};

    /** Some words to ignore while fixing. First 4 are the only English words that start with a double l. Wtf is a llano? */
    private static final List<String> IGNORE_LIST = Arrays.asList("llama", "llamas", "llano", "llanos", "llorar");

    /** Some words are wrongfully fixed. Starting with some Roman numerals. */
    private static final String[][] FIX_LIST = {{"ll", "II"}, {"Il", "II"}, {"IlI", "III"}, {"IlI:", "III:"}, {"Vll", "VII"}, {"VIlI", "VIII"},
            {"VllI", "VIII"}, {"Xll", "XII"}, {"XIlI", "XIII"}, {"XVll", "XVII"}, {"XVIlI", "XVIII"}, {"Nll", "NII"}, {"XXllI", "XXIII"},
            {"d'lsere", "d'Isere"}, {"lemand", "Iemand"}, {"Iets", "lets"}, {"ledere", "Iedere"}, {"Gls", "GIs"},
            {"KEllCHI", "KEIICHI"}, {"leyasu", "Ieyasu"}};

    /**
     * Go go gadget SrtFileFixer.
     * @param args if you enjoy passing about arguments that are never used, this is the place to do it!
     */
    public static void main(final String[] args) {
        System.out.println("Processing directory :: " + Constants.FILEFIXER_ROOT_DIR);
        new SrtFileFixer().process();
    }

    @Override
    protected void process() {
        // collect all srt files in the root dir
        final List<File> srtFiles = new FileFinder().collect(new File(Constants.FILEFIXER_ROOT_DIR), Constants.SRT_EXTENSION);

        // process each file, making a backup first if it is enabled
        for (final File srtFile : srtFiles) {
            System.out.println("Processing subtitle file :: " + srtFile.getName());

            if (Constants.MAKE_BACKUPS) {
                backupFile(srtFile);
            }

            fix(srtFile);
        }
    }

    /**
     * Tries to fix as many issues in the file, hopefully without introducing any new problems.
     */
    @Override
    protected void fixFile(final BufferedReader reader, final BufferedWriter writer) throws Exception {
        // init
        int lineCounter = 1;
        boolean lastLineWasEmpty = false;

        String line;
        while ((line = reader.readLine()) != null) {
            final String fixedLine = fixLine(line);

            if (fixedLine != null && !"".equals(fixedLine.trim())) {
                if (lastLineWasEmpty && lineIsNumberAboveZero(fixedLine)) {
                    writer.newLine();
                    lineCounter++;
                    writer.write("" + lineCounter);
                } else {
                    lastLineWasEmpty = false;
                    writer.write(fixedLine);
                }
                writer.newLine();
            } else {
                lastLineWasEmpty = true;
            }
        }
    }

    /**
     * I've seen SRT files with lots of useless empty lines. Only
     * allow one single empty line between time codes. Do NOT add one
     * before the first one.
     *
     * @param line if the line is one that only contains a number
     * @return true if it is, false if it isn't
     */
    private boolean lineIsNumberAboveZero(final String line) {
        try {
            return Integer.parseInt(line) > 1;
        } catch(final NumberFormatException nfe) {
            return false;
        }
    }

    /**
     * 'Fixes' the given line. Some examples of fixes are:
     * 'wiIIow'   --> 'willow'
     * 'lnk'      --> 'Ink'
     * 'ls'       --> 'Is'
     * ' l '      --> ' I '
     *
     * If enabled:
     * '<i>x</i>' --> 'x'
     * 'I`ve'     --> 'I've'
     *
     * @param line the line of text to attempt to fix
     * @return the line fixed as much as possible
     */
    private String fixLine(final String line) {
        // nothing to fix
        if (line == null || "".equals(line.trim())) {
            return null;
        }

        String fixedLine = line.trim();

        // more weird shit - NUL characters, escape stuff
//        fixedLine = fixedLine.replaceAll("[\uFEFF-\uFFFF]", "");
        fixedLine = fixedLine.replaceAll("[\\x00]", "");

        // Also get rid of the bold/italic/etc tags that might be there
        if (Constants.REMOVE_HTML_TAGS) {
            fixedLine = removeHTMLTags(fixedLine);
        }

        // Fix l -> I in lines that only consist of caps (besides the l's) and special characters, like [](): etc.
        fixedLine = fixCapsOnlyLines(fixedLine);

        // Replace ` with ' because they just look weird
        if (Constants.CHANGE_QUOTES) {
            fixedLine = changeQuotes(fixedLine);
        }

        // most of the fixing happens here
        fixedLine = fixCapsedI(fixedLine);

        // fix 'l ', lf, ln, ls, lt, l'm l've etc at start of the line
        if (shouldFixLineStart(fixedLine, "l")) {
            // this may not cover everything, but it will cover 99%
            fixedLine = fixLineStart(fixedLine, "l ", "ln ", "ls ", "lt ", "lf ", "l'", "lc", "lsn't ", "lt'", "lr", "ld", "lb", "lc",
                    "lg", "lh", "lj", "lk", "ll", "lm", "lp", "lq", "lr", "lv", "lw", "lx", "lz");
        }

        // fix l --> I
        fixedLine = fixedLine.replaceAll(" l ", " I ");
        fixedLine = fixedLine.replaceAll(" l'", " I'");

        // if for some reason there is a capital I in the middle of a line (e.g. 2 sentences on one line, a name/place etc.)
        fixedLine = fixWordStarts(fixedLine);

        // last round, sometimes mistakes are made, this method tries to fix some of them
        fixedLine = fixMistakes(fixedLine);

        // just to be sure, trim again
        fixedLine = fixedLine.trim();

        // logging
        if (!fixedLine.equals(line)) {
            System.out.println("  Changed <[" + line + "]> --> to --> <[" + fixedLine + "]>.");
        }

        return fixedLine;
    }

    /**
     * If the line is something like '[SHOUTlNG]', mostly for the hearing impaired, correct the l to an I.
     * Also covers a few other cases like: 'MlKE:' and allows for single/double quotes, dashes and
     * a few other special characters.
     *
     * @param line the line to fix
     * @return the fixed line
     */
    private String fixCapsOnlyLines(final String line) {
        if (line.matches("^[0-9A-Zl\\[\\]:'\"\\(\\)\\- ]*$")) {
            return line.replaceAll("l", "I");
        }
        return line;
    }

    /**
     * Kind of a brute force fix all for capital I's that are in places
     * they most likely shouldn't be. But since this is a rather crude
     * approach, the resulting line will have to go through some more
     * rounds of fixing to both fix things this method failed to fix, or
     * to repair things this method broke.
     *
     * @param line the line to fix
     * @return the fixed line
     */
    private String fixCapsedI(final String line) {
        String fixedLine = line;

        // I in a word, not first letter
        fixedLine = fixedLine.replaceAll("II", "ll"); // except this one, but what word starts with II? Yeah, Roman numbers... sigh
        fixedLine = fixedLine.replaceAll("aI", "al");
        fixedLine = fixedLine.replaceAll("bI", "bl");
        fixedLine = fixedLine.replaceAll("cI", "cl");
        fixedLine = fixedLine.replaceAll("dI", "dl");
        fixedLine = fixedLine.replaceAll("eI", "el");
        fixedLine = fixedLine.replaceAll("fI", "fl");
        fixedLine = fixedLine.replaceAll("gI", "gl");
        fixedLine = fixedLine.replaceAll("hI", "hl");
        fixedLine = fixedLine.replaceAll("iI", "il");
        fixedLine = fixedLine.replaceAll("jI", "jl");
        fixedLine = fixedLine.replaceAll("kI", "kl");
        fixedLine = fixedLine.replaceAll("mI", "ml");
        fixedLine = fixedLine.replaceAll("nI", "nl");
        fixedLine = fixedLine.replaceAll("oI", "ol");
        fixedLine = fixedLine.replaceAll("pI", "pl");
        fixedLine = fixedLine.replaceAll("qI", "ql");
        fixedLine = fixedLine.replaceAll("rI", "rl");
        fixedLine = fixedLine.replaceAll("sI", "sl");
        fixedLine = fixedLine.replaceAll("tI", "tl");
        fixedLine = fixedLine.replaceAll("uI", "ul");
        fixedLine = fixedLine.replaceAll("vI", "vl");
        fixedLine = fixedLine.replaceAll("wI", "wl");
        fixedLine = fixedLine.replaceAll("xI", "xl");
        fixedLine = fixedLine.replaceAll("yI", "yl");
        fixedLine = fixedLine.replaceAll("zI", "zl");

        // I in a word, could be [If|In|Is|It] so it breaks that, fix it later
        fixedLine = fixedLine.replaceAll("Ia", "la");
        fixedLine = fixedLine.replaceAll("Ib", "lb");
        fixedLine = fixedLine.replaceAll("Ic", "lc");
        fixedLine = fixedLine.replaceAll("Id", "ld");
        fixedLine = fixedLine.replaceAll("Ie", "le");
        fixedLine = fixedLine.replaceAll("If", "lf");
        fixedLine = fixedLine.replaceAll("Ig", "lg");
        fixedLine = fixedLine.replaceAll("Ih", "lh");
        fixedLine = fixedLine.replaceAll("Ii", "li");
        fixedLine = fixedLine.replaceAll("Ij", "lj");
        fixedLine = fixedLine.replaceAll("Ik", "lk");
        fixedLine = fixedLine.replaceAll("Im", "lm");
        fixedLine = fixedLine.replaceAll("In", "ln");
        fixedLine = fixedLine.replaceAll("Io", "lo");
        fixedLine = fixedLine.replaceAll("Ip", "lp");
        fixedLine = fixedLine.replaceAll("Iq", "lq");
        fixedLine = fixedLine.replaceAll("Ir", "lr");
        fixedLine = fixedLine.replaceAll("Is", "ls");
        fixedLine = fixedLine.replaceAll("It", "lt");
        fixedLine = fixedLine.replaceAll("Iu", "lu");
        fixedLine = fixedLine.replaceAll("Iv", "lv");
        fixedLine = fixedLine.replaceAll("Iw", "lw");
        fixedLine = fixedLine.replaceAll("Ix", "lx");
        fixedLine = fixedLine.replaceAll("Iy", "ly");
        fixedLine = fixedLine.replaceAll("Iz", "lz");

        return fixedLine;
    }

    /**
     * Changes the quotes in the given line
     * @param line the line to fix
     * @return the line with fixed quotes
     */
    private String changeQuotes(final String line) {
        String fixedLine = line.replaceAll("`", "'");
        fixedLine = fixedLine.replaceAll("’", "'");
        fixedLine = fixedLine.replaceAll("“", "\"");
        fixedLine = fixedLine.replaceAll("”", "\"");
        return fixedLine;
    }

    /**
     * Some lines start not with an 'l' that needs to become and 'I', but
     * with things like '-l think' in which case a fix is still required.
     * This method tries to identify these odd cases as well as the regular case.
     *
     * @param line the line to check
     * @return true if the line start needs to be fixed, false if not
     */
    public boolean shouldFixLineStart(final String line, final String startValue) {
        for (final String lineStarter : LINE_STARTS) {
            if (line.startsWith(lineStarter + startValue)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fixes a line start. See shouldFixLineStart for more details.
     *
     * @param line the line to fix
     * @param possibleValues the values the line could start with
     * @return the fixed line
     */
    private String fixLineStart(final String line, final String... possibleValues) {
        for (final String possibleValue : possibleValues) {
            for (final String lineStarter : LINE_STARTS) {
                if (line.startsWith(lineStarter + possibleValue)) {
                    return line.replaceFirst("l", "I");
                }
            }
        }
        return line;
    }

    /**
     * Removes certain HTML tags from the given line.
     *
     * @param lineToFix the line to fix
     * @return the fixed line, without the removed HTML tags
     */
    private String removeHTMLTags(final String lineToFix) {
        String fixedLine = lineToFix;

        fixedLine = fixedLine.replaceAll("<b>", "");
        fixedLine = fixedLine.replaceAll("</b>", "");
        fixedLine = fixedLine.replaceAll("<i>", "");
        fixedLine = fixedLine.replaceAll("</i>", "");
        fixedLine = fixedLine.replaceAll("<br>", "");
        fixedLine = fixedLine.replaceAll("<br />", "");

        return fixedLine;
    }

    /**
     * Fixes 'l' characters at the start of a word. Generally speaking, most
     * if not all words beginning with an 'l' followed by another consonant,
     * are not real words. Those can be fixed easily.
     *
     * Some words start with an 'l' followed by a vowel. Most likely, these 'l's
     * are actual 'l's. There is an off-chance that they are not, but I can't be
     * bothered with that.
     *
     * @param line the the line to fix
     * @return the fixed line
     */
    private String fixWordStarts(final String line) {
        final String[] parts = line.split(" ");

        final StringBuilder lineBuilder = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            // I prefer a space between a - and the word, when the word starts with a dash
            if (part.matches("-[0-9a-zA-Z']+")) {
                final String word = part.substring(1);
                part = "- " + word;
            }

            // yes this can be done in 1 if, no I'm not doing it
            if (startsWithAny(part, "lb", "lc", "ld", "lf", "lg", "lh", "lj", "lk", "ll", "lm", "ln", "lp", "lq", "lr",
                    "ls", "lt", "lv", "lw", "lx", "lz")) {
                // some words are incorrectly fixed (llama for instance, and some Spanish stuff)
                if (startsWithAny(part, "ll") && isOnIgnoreList(part)) {
                    lineBuilder.append(part);
                } else {
                    // I starting a word
                    part = part.replaceFirst("l", "I");
                    lineBuilder.append(part);
                }
            } else if ("l.".equals(part)) {
                // I at the end of a sentence.
                lineBuilder.append("I.");
            } else if ("l,".equals(part)) {
                // I, just before a comma
                lineBuilder.append("I,");
            } else if ("l?".equals(part)) {
                // I? Wut? Me? Moi?
                lineBuilder.append("I?");
            } else if ("l!".equals(part)) {
                // I! 't-was me!
                lineBuilder.append("I!");
            } else if ("l..".equals(part)) {
                // I.. think?
                lineBuilder.append("I..");
            } else if ("l...".equals(part)) {
                // I... like dots.
                lineBuilder.append("I...");
            } else if ("i".equals(part)) {
                // i suck at spelling.
                lineBuilder.append("I");
            } else if (part.startsWith("i'")) {
                // i also suck at spelling.
                part = part.replaceFirst("i", "I");
                lineBuilder.append(part);
            } else {
                // nothing special to do
                lineBuilder.append(part);
            }

            // add trailing space if it is not the last part
            if (i != parts.length - 1) {
                lineBuilder.append(" ");
            }
        }

        return lineBuilder.toString();
    }

    /**
     * Checks if the given word is on the global ignore list.
     *
     * @param part the word to check
     * @return true if it is, false if it isn't
     */
    private boolean isOnIgnoreList(final String part) {
        return IGNORE_LIST.contains(part);
    }

    /**
     * Checks if the given anyString starts with any of the given startValues.
     *
     * @param anyString the string to check
     * @param startValues the possible start values
     * @return true if anyString starts with one of the given startValues, false if not
     */
    private boolean startsWithAny(final String anyString, final String... startValues) {
        for (final String startValue : startValues) {
            if (shouldFixLineStart(anyString, startValue)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fixes incorrectly fixed words in a line.
     *
     * @param line the line to fix
     * @return the fixed line
     */
    private String fixMistakes(final String line) {
        final String[] parts = line.split(" ");
        final StringBuilder lineBuilder = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            for (final String[] fixWord : FIX_LIST) {
                if (fixWord[0].equals(part)) {
                    part = fixWord[1];
                    break;
                }
            }

            lineBuilder.append(part);

            // add trailing space if it is not the last part
            if (i != parts.length - 1) {
                lineBuilder.append(" ");
            }
        }

        return lineBuilder.toString();
    }
}
