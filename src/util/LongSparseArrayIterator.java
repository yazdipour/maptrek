package mobi.maptrek.util;

import android.util.LongSparseArray;
import java.util.ListIterator;
import java.util.NoSuchElementException;

public final class LongSparseArrayIterator<E> implements ListIterator<E> {
    private final LongSparseArray<E> array;
    private int cursor;
    private boolean cursorNowhere;

    public static <E> ListIterator<E> iterate(LongSparseArray<E> array) {
        return new LongSparseArrayIterator(array, -1);
    }

    private LongSparseArrayIterator(LongSparseArray<E> array, int location) {
        this.array = array;
        if (location < 0) {
            this.cursor = -1;
            this.cursorNowhere = true;
        } else if (location < array.size()) {
            this.cursor = location;
            this.cursorNowhere = false;
        } else {
            this.cursor = array.size() - 1;
            this.cursorNowhere = true;
        }
    }

    public boolean hasNext() {
        return this.cursor < this.array.size() + -1;
    }

    public boolean hasPrevious() {
        return (this.cursorNowhere && this.cursor >= 0) || this.cursor > 0;
    }

    public int nextIndex() {
        if (hasNext()) {
            return this.array.indexOfKey(this.array.keyAt(this.cursor + 1));
        }
        throw new NoSuchElementException();
    }

    public int previousIndex() {
        if (!hasPrevious()) {
            throw new NoSuchElementException();
        } else if (this.cursorNowhere) {
            return this.array.indexOfKey(this.array.keyAt(this.cursor));
        } else {
            return this.array.indexOfKey(this.array.keyAt(this.cursor - 1));
        }
    }

    public E next() {
        if (hasNext()) {
            if (this.cursorNowhere) {
                this.cursorNowhere = false;
            }
            this.cursor++;
            return this.array.valueAt(this.cursor);
        }
        throw new NoSuchElementException();
    }

    public E previous() {
        if (hasPrevious()) {
            if (this.cursorNowhere) {
                this.cursorNowhere = false;
            } else {
                this.cursor--;
            }
            return this.array.valueAt(this.cursor);
        }
        throw new NoSuchElementException();
    }

    public void add(E e) {
        throw new UnsupportedOperationException();
    }

    public void remove() {
        if (this.cursorNowhere) {
            throw new IllegalStateException();
        }
        this.array.remove(this.array.keyAt(this.cursor));
        this.cursorNowhere = true;
        this.cursor--;
    }

    public void set(E object) {
        if (this.cursorNowhere) {
            throw new IllegalStateException();
        }
        this.array.setValueAt(this.cursor, object);
    }
}
