package core.content;

public sealed interface TagReference<C extends ContentType> {

    boolean matches(C type);
    C any(); // возвращает первый предмет

    record OfUnresolved<C extends ContentType>(Class<C> type, String key) implements TagReference<C> {
        public boolean matches(C type) {
            return false;
        }

        public C any() {
            throw new UnsupportedOperationException();
        }
    }

    record OfType<C extends ContentType>(C type) implements TagReference<C> {
        public boolean matches(C type) {
            return this.type.equals(type);
        }

        public C any() {
            return type;
        }
    }
    record OfTag<C extends ContentType>(Tag<C> tag) implements TagReference<C> {
        public boolean matches(C type) {
            return this.tag.contains(type);
        }

        public C any() {
            return tag.elements().getFirst();
        }
    }
}
