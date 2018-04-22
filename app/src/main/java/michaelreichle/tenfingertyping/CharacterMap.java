package michaelreichle.tenfingertyping;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * By michaelreichle on 15.04.2018 at 13:52.
 */
public class CharacterMap {
    private static final Map<Character, Integer> mapping = getMapping();
    private static final String supported = "qwertasdfgyxcv";
    private static final Random r = new Random();

    public static final int THUMB = 4;
    public static final int INDEX_FINGER = 3;
    public static final int MIDDLE_FINGER = 2;
    public static final int RING_FINGER = 1;
    public static final int LITTLE_FINGER = 0;
    public static final int NO_FINGER = -1;

    private static Map<Character, Integer> getMapping() {
        Map<Character, Integer> result = new HashMap<>();
        result.put('q', RING_FINGER);
        result.put('w', MIDDLE_FINGER);
        result.put('e', MIDDLE_FINGER);
        result.put('r', INDEX_FINGER);
        result.put('t', INDEX_FINGER);
        result.put('a', RING_FINGER);
        result.put('s', MIDDLE_FINGER);
        result.put('d', INDEX_FINGER);
        result.put('f', INDEX_FINGER);
        result.put('g', INDEX_FINGER);
        result.put('y', RING_FINGER);
        result.put('x', MIDDLE_FINGER);
        result.put('c', INDEX_FINGER);
        result.put('v', INDEX_FINGER);
        // result.put(' ', THUMB);

        return Collections.unmodifiableMap(result);
    }

    public static int getFingerIndex(char c) {
        if (mapping.containsKey(c)) {
            return mapping.get(c);
        } else {
            return NO_FINGER;
        }
    }

    public static String filterSupported(String str) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (mapping.containsKey(c)) {
                res.append(c);
            }
        }
        return res.toString();
    }

    public static String getRandomSupportedText(int monitorCount, int length) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int random = r.nextInt(monitorCount + 1);
            res.append(String.valueOf(supported.charAt(random)));
        }
        return res.toString();
    }
}
