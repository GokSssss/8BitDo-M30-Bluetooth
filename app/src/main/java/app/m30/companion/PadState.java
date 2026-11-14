package app.m30.companion;

import java.util.*;

/** Android-independent input state; tokens preserve the raw key/axis identity. */
public final class PadState {
    public static final String[] LABELS = {"↑", "↓", "←", "→", "A", "B", "C", "X", "Y", "Z", "L", "R", "START", "SELECT", "HOME", "★"};
    public final Set<String> down = new HashSet<>();
    public final Set<String> seen = new HashSet<>();
    public final Map<String, String> bindings = new LinkedHashMap<>();
    public final Map<String, Integer> counts = new HashMap<>();
    public int presses;
    public void bind(String label, String token) {
        String target = canonicalBinding(token);
        bindings.entrySet().removeIf(e -> canonicalBinding(e.getValue()).equals(target) && !e.getKey().equals(label));
        bindings.put(label, target);
    }
    private static String canonicalBinding(String token) {
        if(token==null)return null;
        return switch (token) {
            case "K:104", "T:17:+", "T:23:+" -> "B:L2";
            case "K:105", "T:18:+", "T:22:+" -> "B:R2";
            case "K:19", "A:1:-", "A:13:-", "A:14:-", "A:16:-" -> "D:19";
            case "K:20", "A:1:+", "A:13:+", "A:14:+", "A:16:+" -> "D:20";
            case "K:21", "A:0:-", "A:11:-", "A:12:-", "A:15:-" -> "D:21";
            case "K:22", "A:0:+", "A:11:+", "A:12:+", "A:15:+" -> "D:22";
            default -> token;
        };
    }
    public boolean set(String token, boolean active) {
        boolean changed = active ? down.add(token) : down.remove(token);
        if (active && changed) { seen.add(token); presses++; counts.put(token, counts.getOrDefault(token, 0) + 1); }
        direction("D:19", "K:19", new int[]{1, 13, 14, 16}, "-");
        direction("D:20", "K:20", new int[]{1, 13, 14, 16}, "+");
        direction("D:21", "K:21", new int[]{0, 11, 12, 15}, "-");
        direction("D:22", "K:22", new int[]{0, 11, 12, 15}, "+");
        trigger("B:L2","K:104",17,23);
        trigger("B:R2","K:105",18,22);
        return changed;
    }
    private void trigger(String alias,String key,int axis,int alternative){
        if(down.contains(key)||down.contains("T:"+axis+":+")||down.contains("T:"+alternative+":+")){down.add(alias);seen.add(alias);}else down.remove(alias);
    }
    private void direction(String alias, String key, int[] axes, String sign) {
        boolean active = down.contains(key);
        for (int axis : axes) active |= down.contains("A:" + axis + ":" + sign);
        if (active) { down.add(alias); seen.add(alias); } else down.remove(alias);
    }
    public Set<String> rawDown() { Set<String> raw = new TreeSet<>(down); raw.removeIf(s -> s.startsWith("D:")||s.startsWith("B:")); return raw; }
    public boolean active(String label) { return down.contains(canonicalBinding(bindings.get(label))); }
    public boolean tested(String label) { return seen.contains(canonicalBinding(bindings.get(label))); }
    public int testedCount() { int n = 0; for (String s : LABELS) if (tested(s)) n++; return n; }
    public void release() { down.clear(); }
    public void reset() { release(); seen.clear(); counts.clear(); presses = 0; }
    public static float normalize(float value, float flat) { return Math.abs(value) > Math.max(.15f, flat) ? value : 0f; }
}
