package core.audio;

public final class AudioBuffer {
    private final int bufferId;
    private final String name; //для отладки

    public AudioBuffer(int bufferId, String name) {
        this.bufferId = bufferId;
        this.name = name;
    }

    public int bufferId() {
        return bufferId;
    }

    @Override
    public String toString() {
        return "AudioBuffer{" + name + ", id=" + bufferId + "}";
    }
}