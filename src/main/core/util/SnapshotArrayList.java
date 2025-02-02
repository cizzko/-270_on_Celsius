package core.util;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectList;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class SnapshotArrayList<T> extends ObjectArrayList<T> {
    private int snapshots;

    public SnapshotArrayList(T[] a, boolean wrapped) {
        super(a, wrapped);
    }

    public SnapshotArrayList(int capacity) {
        super(capacity);
    }

    public SnapshotArrayList() {
        super();
    }

    public SnapshotArrayList(Collection<? extends T> c) {
        super(c);
    }

    public SnapshotArrayList(ObjectCollection<? extends T> c) {
        super(c);
    }

    public SnapshotArrayList(ObjectList<? extends T> l) {
        super(l);
    }

    public SnapshotArrayList(T[] a) {
        super(a);
    }

    public SnapshotArrayList(T[] a, int offset, int length) {
        super(a, offset, length);
    }

    public SnapshotArrayList(Iterator<? extends T> i) {
        super(i);
    }

    public SnapshotArrayList(ObjectIterator<? extends T> i) {
        super(i);
    }

    public T[] begin() {
        snapshots++;
        return a;
    }

    public void end() {
        snapshots --;
        if (snapshots < 0)
            throw new IllegalStateException();
    }

    private void modified() {
        if (snapshots > 0) {
            a = a.clone();
        }
    }

    @Override
    public void add(int index, T t) {
        modified();
        super.add(index, t);
    }

    @Override
    public boolean add(T t) {
        modified();
        return super.add(t);
    }

    @Override
    public T remove(int index) {
        modified();
        return super.remove(index);
    }

    @Override
    public boolean remove(Object k) {
        modified();
        return super.remove(k);
    }

    @Override
    public T set(int index, T t) {
        modified();
        return super.set(index, t);
    }

    @Override
    public void clear() {
        modified();
        super.clear();
    }

    @Override
    public void size(int size) {
        modified();
        super.size(size);
    }

    @Override
    public void trim() {
        modified();
        super.trim();
    }

    @Override
    public void trim(int n) {
        modified();
        super.trim(n);
    }

    @Override
    public void removeElements(int from, int to) {
        modified();
        super.removeElements(from, to);
    }

    @Override
    public void addElements(int index, T[] a, int offset, int length) {
        modified();
        super.addElements(index, a, offset, length);
    }

    @Override
    public void setElements(int index, T[] a, int offset, int length) {
        modified();
        super.setElements(index, a, offset, length);
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        super.forEach(action);
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        modified();
        return super.addAll(index, c);
    }

    @Override
    public boolean addAll(int index, ObjectList<? extends T> l) {
        modified();
        return super.addAll(index, l);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean modified = super.removeAll(c);
        if (modified) modified();
        return modified;
    }

    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        modified();
        return super.removeIf(filter);
    }

    @Override
    public void sort(Comparator<? super T> comp) {
        modified();
        super.sort(comp);
    }

    @Override
    public void unstableSort(Comparator<? super T> comp) {
        modified();
        super.unstableSort(comp);
    }
}
