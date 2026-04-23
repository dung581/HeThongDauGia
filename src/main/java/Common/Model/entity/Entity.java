package Common.Model.entity;

import java.io.Serializable;

/**
 * Lớp cơ sở cho mọi thực thể có định danh trong hệ thống (User, Item, ...).
 * Cài Serializable để có thể truyền qua Socket bằng ObjectOutputStream sau này.
 */
public abstract class Entity implements Serializable {
    private static final long serialVersionUID = 1L;

    protected String IID;

    public Entity(String IID) {
        this.IID = IID;
    }

    public String getIID() {
        return IID;
    }

    public void setIID(String IID) {
        this.IID = IID;
    }
}