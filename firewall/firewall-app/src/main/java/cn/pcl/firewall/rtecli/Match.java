package cn.pcl.firewall.rtecli;

public class Match {
    public enum Type {
        EXACT,
        TERNARY,
        LPM;
    }

    private Type type;

    private String key;

    private String value;

    private String mask;

    private Match(Type type, String key, String value) {
        this.type = type;
        this.key = key;
        this.value = value;
    }

    private Match(Type type, String key, String value, String mask) {
        this.type = type;
        this.key = key;
        this.value = value;
        this.mask = mask;
    }

    public Type getType() {
        return type;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public String getMask() {
        return mask;
    }

    public static class MatchFactory {
        public Match createExactMatch(String key, String value) {
            return new Match(Type.EXACT, key, value);
        }

        public Match createTernaryMatch(String key, String value, String mask) {
            return new Match(Type.TERNARY, key, value, mask);
        }

        public Match createLpmMatch(String key, String value) {
            return new Match(Type.LPM, key, value);
        }
    }

}
