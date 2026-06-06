package core.content;

import java.util.List;
import java.util.Set;

public final class Tag<C extends ContentType> implements ContentType {
    private final String id;
    private final Class<C> type;
    private final Set<String> ids;
    private final List<C> elements;

    public Tag(String id, Class<C> type, Set<String> ids, List<C> elements) {
        this.id = id;
        this.type = type;
        this.ids = ids;
        this.elements = elements;
    }

    public String id() { return id; }

    public Class<C> type() { return type; }

    public boolean contains(ContentType contentType) { return ids.contains(contentType.id()); }

    public boolean contains(String id) { return ids.contains(id); }

    public Set<String> ids() { return ids; }

    public List<C> elements() { return elements; }

    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof Tag<?> tag && id.equals(tag.id);
    }

    public int hashCode() { return id.hashCode(); }

    public String toString() {
        return "Tag<" + type.getSimpleName() + ">['" + id + "']";
    }
}
