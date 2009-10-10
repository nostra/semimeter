package org.semispace.semimeter.bean;

/**
 * A single datum
 */
public class Item {
    /**
     * Synthetic primary key - applicable only when read from database
     */
    private Long id;

    private String path;

    private long when;

    private int accessNumber;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getWhen() {
        return when;
    }

    public void setWhen(long when) {
        this.when = when;
    }

    public int getAccessNumber() {
        return accessNumber;
    }

    public void increment() {
        accessNumber++;
    }

    public String toString() {
        return "Item[path:"+path+"][when:"+when+"][accessNumber:"+accessNumber+"]";
    }
}
