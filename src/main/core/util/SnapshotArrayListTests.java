package core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class SnapshotArrayListTests {
    @Test
    void allocsControl() {
        var list = new SnapshotArrayList<Integer>();

        {
            Object[] snapshot = list.begin(); var copy = snapshot.clone(); int size = list.size();

            list.add(-42); list.pop();
            assertArrayEquals(snapshot, copy);
            assertEquals(1, SnapshotArrayList.allocs);

            list.add(0);
            assertArrayEquals(snapshot, copy);
            assertEquals(1, SnapshotArrayList.allocs);

            list.end(snapshot);
        }

        {
            Object[] snapshot1 = list.begin(); var copy1 = snapshot1.clone(); int count1 = list.size();

            list.add(-42); list.pop();
            assertArrayEquals(snapshot1, copy1);
            assertEquals(1, SnapshotArrayList.allocs);

            list.add(1);
            assertArrayEquals(snapshot1, copy1);
            assertEquals(1, SnapshotArrayList.allocs);

            {
                Object[] snapshot2 = list.begin(); var copy2 = snapshot2.clone(); int count2 = list.size();

                list.add(-42); list.pop();
                assertArrayEquals(snapshot2, copy2);
                assertEquals(2, SnapshotArrayList.allocs);

                list.add(2);
                assertArrayEquals(snapshot2, copy2);
                assertEquals(2, SnapshotArrayList.allocs);

                list.end(snapshot2);
            }

            list.add(-42); list.pop();
            assertArrayEquals(snapshot1, copy1);
            assertEquals(2, SnapshotArrayList.allocs);

            list.add(3);
            assertArrayEquals(snapshot1, copy1);
            assertEquals(2, SnapshotArrayList.allocs);

            list.end(snapshot1);
        }
    }

    @Test
    void zeroAllocs() {
        var list = new SnapshotArrayList<Integer>();
        // Обычный ArrayList
        list.add(0);
        assertEquals(0, SnapshotArrayList.allocs);

        list.add(-42); list.pop();
        assertEquals(0, SnapshotArrayList.allocs);

        {
            Object[] snapshot1 = list.begin(); int count1 = list.size();
            assertEquals(0, SnapshotArrayList.allocs);
            {
                Object[] snapshot2 = list.begin(); int count2 = list.size();
                assertEquals(0, SnapshotArrayList.allocs);
                list.end(snapshot2);
            }
            list.end(snapshot1);
        }
    }
}
