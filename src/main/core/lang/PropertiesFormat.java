package core.lang;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

// Для тестов и просто потыкаться
enum PropertiesFormat implements core.lang.Format {
    INSTANCE;

    @Override
    public String ext() {
        return "properties";
    }

    @Override
    public void write(Path file, TreeMap<String, TrLine> trMap, Context ctx) throws IOException {
        try (var wr = Files.newBufferedWriter(file)) {
            for (var entry : trMap.entrySet()) {
                String key = entry.getKey();
                TrLine line = entry.getValue();

                for (String s : line.comments()) {
                    wr.write("# ");
                    wr.write(s);
                    wr.newLine();
                }

                wr.write(key);
                wr.write(" = ");
                wr.write(line.text());
                wr.newLine();

                wr.newLine();
            }
        }
    }

    @Override
    public TreeMap<String, TrLine> read(Path file, Context ctx) throws IOException {
        var result = TrMap.makeTrMap();
        try (var reader = Files.newBufferedReader(file)) {
            load(reader, result);
        }
        return result;
    }

    @Override
    public HashMap<String, String> readTable(Path file, Context ctx) throws IOException {
        throw new UnsupportedOperationException(); // TODO не приоритет на реализацию
    }

    public static void load(Reader reader, Map<String, TrLine> out) throws IOException {
        load0(new LineReader(reader), out);
    }

    private static void load0(LineReader lr, Map<String, TrLine> out) throws IOException {
        var outBuffer = new StringBuilder();
        int limit;
        int keyLen;
        int valueStart;
        boolean hasSep;
        boolean precedingBackslash;

        while ((limit = lr.readLine()) >= 0) {
            keyLen = 0;
            valueStart = limit;
            hasSep = false;

            precedingBackslash = false;
            while (keyLen < limit) {
                char c = lr.lineBuf[keyLen];
                if ((c == '=' || c == ':') && !precedingBackslash) {
                    valueStart = keyLen + 1;
                    hasSep = true;
                    break;
                } else if ((c == ' ' || c == '\t' || c == '\f') && !precedingBackslash) {
                    valueStart = keyLen + 1;
                    break;
                }
                if (c == '\\') {
                    precedingBackslash = !precedingBackslash;
                } else {
                    precedingBackslash = false;
                }
                keyLen++;
            }
            while (valueStart < limit) {
                char c = lr.lineBuf[valueStart];
                if (c != ' ' && c != '\t' && c != '\f') {
                    if (!hasSep && (c == '=' || c == ':')) {
                        hasSep = true;
                    } else {
                        break;
                    }
                }
                valueStart++;
            }
            String key = loadConvert(lr.lineBuf, 0, keyLen, outBuffer);
            String value = loadConvert(lr.lineBuf, valueStart, limit - valueStart, outBuffer);

            var old = out.put(key, new TrLine(value, new ArrayList<>(lr.comments)));
            if (old != null) {
                // report dup keys?
            }

            lr.comments.clear();
        }
    }

    /* Read in a "logical line" from an InputStream/Reader, skip all comment
     * and blank lines and filter out those leading whitespace characters
     * (\u0020, \u0009 and \u000c) from the beginning of a "natural line".
     * Method returns the char length of the "logical line" and stores
     * the line in "lineBuf".
     */
    private static class LineReader {
        LineReader(Reader reader) {
            this.reader = reader;
        }

        final char[] inCharBuf = new char[8192];
        final Reader reader;
        final ArrayList<String> comments = new ArrayList<String>();

        char[] lineBuf = new char[1024];
        int inLimit = 0;
        int inOff = 0;

        int readLine() throws IOException {
            // use locals to optimize for interpreted performance
            int len = 0;
            int off = inOff;
            int limit = inLimit;

            boolean skipWhiteSpace = true;
            boolean appendedLineBegin = false;
            boolean precedingBackslash = false;
            char[] charBuf = inCharBuf;
            char[] lineBuf = this.lineBuf;
            char c;

            while (true) {
                if (off >= limit) {
                    inLimit = limit = reader.read(charBuf);
                    if (limit <= 0) {
                        if (len == 0) {
                            return -1;
                        }
                        return precedingBackslash ? len - 1 : len;
                    }
                    off = 0;
                }

                c = charBuf[off++];
                if (skipWhiteSpace) {
                    if (c == ' ' || c == '\t' || c == '\f') {
                        continue;
                    }
                    if (!appendedLineBegin && (c == '\r' || c == '\n')) {
                        continue;
                    }
                    skipWhiteSpace = false;
                    appendedLineBegin = false;

                }
                if (len == 0) { // Still on a new logical line
                    if (c == '#' || c == '!') {
                        // Comment, quickly consume the rest of the line
                        int commentBegin = off;

                        commentLoop:
                        while (true) {
                            while (off < limit) {
                                c = charBuf[off++];
                                if ((c == '\r' || c == '\n')) {
                                    String comment = new String(charBuf, commentBegin, off - commentBegin - 1).trim();
                                    if (!comment.isEmpty()) {
                                        this.comments.add(comment);
                                    }
                                    break commentLoop;
                                }
                            }
                            if (off == limit) {
                                inLimit = limit = reader.read(charBuf);
                                if (limit <= 0) { // EOF
                                    String comment = new String(charBuf, commentBegin, off - commentBegin).trim();
                                    if (!comment.isEmpty()) {
                                        this.comments.add(comment);
                                    }
                                    return -1;
                                }
                                off = 0;
                            }
                        }
                        skipWhiteSpace = true;
                        continue;
                    }
                }

                if (c != '\n' && c != '\r') {
                    lineBuf[len++] = c;
                    if (len == lineBuf.length) {
                        lineBuf = new char[newLength(len, 1, len)];
                        System.arraycopy(this.lineBuf, 0, lineBuf, 0, len);
                        this.lineBuf = lineBuf;
                    }
                    // flip the preceding backslash flag
                    precedingBackslash = c == '\\' && !precedingBackslash;
                } else {
                    // reached EOL
                    if (len == 0) {
                        skipWhiteSpace = true;
                        continue;
                    }
                    if (off >= limit) {
                        inLimit = limit = reader.read(charBuf);
                        off = 0;
                        if (limit <= 0) { // EOF
                            return precedingBackslash ? len - 1 : len;
                        }
                    }
                    if (precedingBackslash) {
                        // backslash at EOL is not part of the line
                        len -= 1;
                        // skip leading whitespace characters in the following line
                        skipWhiteSpace = true;
                        appendedLineBegin = true;
                        precedingBackslash = false;
                        // take care not to include any subsequent \n
                        if (c == '\r' && charBuf[off] == '\n') {
                            off++;
                        }
                    } else {
                        inOff = off;
                        return len;
                    }
                }
            }
        }
    }

    private static int newLength(int oldLength, int minGrowth, int prefGrowth) {
        int prefLength = oldLength + Math.max(minGrowth, prefGrowth); // might overflow
        if (0 < prefLength && prefLength <= Integer.MAX_VALUE - 8) {
            return prefLength;
        } else {
            return hugeLength(oldLength, minGrowth);
        }
    }

    private static int hugeLength(int oldLength, int minGrowth) {
        int minLength = oldLength + minGrowth;
        if (minLength < 0) { // overflow
            throw new OutOfMemoryError("Required array length " + oldLength + " + " + minGrowth + " is too large");
        } else if (minLength <= Integer.MAX_VALUE - 8) {
            return Integer.MAX_VALUE - 8;
        } else {
            return minLength;
        }
    }

    /*
     * Converts encoded &#92;uxxxx to unicode chars
     * and changes special saved chars to their original forms
     */
    private static String loadConvert(char[] in, int off, int len, StringBuilder out) {
        char aChar;
        int end = off + len;
        int start = off;
        while (off < end) {
            aChar = in[off++];
            if (aChar == '\\') {
                break;
            }
        }
        if (off == end) { // No backslash
            return new String(in, start, len);
        }

        // backslash found at off - 1, reset the shared buffer, rewind offset
        out.setLength(0);
        off--;
        out.append(in, start, off - start);

        while (off < end) {
            aChar = in[off++];
            if (aChar == '\\') {
                // No need to bounds check since LineReader::readLine excludes
                // unescaped \s at the end of the line
                aChar = in[off++];
                if (aChar == 'u') {
                    // Read the xxxx
                    if (off > end - 4)
                        throw new IllegalArgumentException(
                                "Malformed \\uxxxx encoding.");
                    int value = 0;
                    for (int i = 0; i < 4; i++) {
                        aChar = in[off++];
                        value = switch (aChar) {
                            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> (value << 4) + aChar - '0';
                            case 'a', 'b', 'c', 'd', 'e', 'f' -> (value << 4) + 10 + aChar - 'a';
                            case 'A', 'B', 'C', 'D', 'E', 'F' -> (value << 4) + 10 + aChar - 'A';
                            default -> throw new IllegalArgumentException("Malformed \\uxxxx encoding.");
                        };
                    }
                    out.append((char) value);
                } else {
                    switch (aChar) {
                        case 't' -> aChar = '\t';
                        case 'r' -> aChar = '\r';
                        case 'n' -> aChar = '\n';
                        case 'f' -> aChar = '\f';
                    }
                    out.append(aChar);
                }
            } else {
                out.append(aChar);
            }
        }
        return out.toString();
    }
}
