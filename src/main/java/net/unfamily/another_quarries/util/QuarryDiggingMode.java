package net.unfamily.another_quarries.util;

public enum QuarryDiggingMode {
    VOLUME(0),
    CHUNK(1);

    private final int id;

    QuarryDiggingMode(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public QuarryDiggingMode toggle() {
        return this == VOLUME ? CHUNK : VOLUME;
    }

    public static QuarryDiggingMode fromId(int id) {
        return id == CHUNK.id ? CHUNK : VOLUME;
    }
}
