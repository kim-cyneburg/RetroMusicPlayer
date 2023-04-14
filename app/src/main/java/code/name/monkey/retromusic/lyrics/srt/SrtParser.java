package code.name.monkey.retromusic.lyrics.srt;

import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import code.name.monkey.retromusic.lyrics.LrcEntry;

public class SrtParser {
    private static final String TAG = "SrtUtils";
    private static final Pattern PATTERN_TIME = Pattern.compile("([\\d]{2}:[\\d]{2}:[\\d]{2},[\\d]{3}).*([\\d]{2}:[\\d]{2}:[\\d]{2},[\\d]{3})");
    private static final Pattern PATTERN_NUMBERS = Pattern.compile("(\\d+)");
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private static final String REGEX_REMOVE_TAGS = "<[^>]*>";

    private static final int PATTERN_TIME_REGEX_GROUP_START_TIME = 1;
    private static final int PATTERN_TIME_REGEX_GROUP_END_TIME = 2;

    public static List<LrcEntry> parseSrt(String path) {

        ArrayList<LrcEntry> lrcList = new ArrayList<>();
        ArrayList<Subtitle> subtitles = null;
        Subtitle subtitle;
        StringBuilder srt;
        boolean keepNewlinesEscape = true;
        boolean usingNodes = false;

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(path), DEFAULT_CHARSET))) {

            subtitles = new ArrayList<>();
            subtitle = new Subtitle();
            srt = new StringBuilder();

            while (bufferedReader.ready()) {

                String line = bufferedReader.readLine();

                Matcher matcher = PATTERN_NUMBERS.matcher(line);

                if (matcher.find()) {
                    subtitle.id = Integer.parseInt(matcher.group(1)); // index
                    line = bufferedReader.readLine();
                }

                matcher = PATTERN_TIME.matcher(line);

                if (matcher.find()) {
                    subtitle.startTime = matcher.group(PATTERN_TIME_REGEX_GROUP_START_TIME); // start time
                    subtitle.timeIn = SRTUtils.textTimeToMillis(subtitle.startTime);
                    subtitle.endTime = matcher.group(PATTERN_TIME_REGEX_GROUP_END_TIME); // end time
                    subtitle.timeOut = SRTUtils.textTimeToMillis(subtitle.endTime);
                }

                String aux;
                while ((aux = bufferedReader.readLine()) != null && !aux.isEmpty()) {
                    srt.append(aux);
                    if (keepNewlinesEscape)
                        srt.append("\n");
                    else {
                        if (!line.endsWith(" ")) // for any new lines '\n' removed from BufferedReader
                            srt.append(" ");
                    }
                }

                srt.delete(srt.length()-1, srt.length()); // remove '\n' or space from end string

                line = srt.toString();
                srt.setLength(0); // Clear buffer

                if (line != null && !line.isEmpty())
                    line = line.replaceAll(REGEX_REMOVE_TAGS, ""); // clear all tags

                subtitle.text = line;
                subtitles.add(subtitle);

                if (usingNodes) {
                    subtitle.nextSubtitle = new Subtitle();
                    subtitle = subtitle.nextSubtitle;
                } else {
                    subtitle = new Subtitle();
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "parseSrt, e:"+e);
        }
        if(subtitles != null) {
            for(Subtitle sub : subtitles) {
                lrcList.add(new LrcEntry(sub.timeIn, sub.text));
            }
        }
        return lrcList;
    }
}
