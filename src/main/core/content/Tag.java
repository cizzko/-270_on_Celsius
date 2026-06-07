package core.content;

import java.util.List;
import java.util.Set;

public final class Tag<C extends ContentType> implements ContentType {
    private final String key;
    private final Class<C> type;
    private final Set<String> ids;
    private final List<C> elements;

    private short id;

    public Tag(String key, Class<C> type, Set<String> ids, List<C> elements) {
        this.key = key;
        this.type = type;
        this.ids = ids;
        this.elements = elements;
    }

    public short id() { return id; }

    public void setId(short id) { this.id = id; }

    public String key() { return key; }

    public Class<C> type() { return type; }

    public boolean contains(ContentType contentType) { return ids.contains(contentType.key()); }

    public boolean contains(String id) { return ids.contains(id); }

    public Set<String> ids() { return ids; }

    public List<C> elements() { return elements; }

    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof Tag<?> tag && key.equals(tag.key);
    }

    public int hashCode() { return key.hashCode(); }

    public String toString() {
        return "Tag<" + type.getSimpleName() + ">['" + key + "']";
    }
}
