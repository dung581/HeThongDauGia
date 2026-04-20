package Common.Model;

public abstract class Entity {
    protected String IID;
    public Entity(String IID){
        this.IID = IID;
    }
    public String getIID(){
        return IID;
    }

    public void setIID(String IID) {
        this.IID = IID;
    }
}
