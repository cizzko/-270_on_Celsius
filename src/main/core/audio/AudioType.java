package core.audio;

public enum AudioType {
    //todo
    INTERFACE(100),
    MUSIC(100),
    ENVIRONMENT(100);

    private final int volume;

    AudioType(int volume) {
        this.volume = volume;
    }

    public int getVolume() {
        return volume;
    }
}
