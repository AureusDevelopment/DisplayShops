package xzot1k.plugins.ds.core.utils;

public class Ps {
    String name;
    String value;

    public Ps(String name, String value) {
        this.name = "%" + name + "%";
        this.value = value;
    }

    public static Ps of(String name, String value) {
        return new Ps(name, value);
    }

    public String replace(String text) {
        return text.replace(name, value);
    }
}
