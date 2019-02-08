package mobi.maptrek.data.style;

public abstract class Style<T> {
    public String id;

    public abstract void copy(T t);

    public abstract boolean isDefault();
}
