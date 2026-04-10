package common;

public enum ItemType {
    VEHICLE,
    ELECTRONICS,
    ART;
    //Ham ho tro chuyen String thanh enum an toan
    public static ItemType fromString(String type){
        try{
            return ItemType.valueOf(type.toUpperCase());
        }
        catch (IllegalArgumentException | NullPointerException e){
            throw new IllegalArgumentException("He thong khong ho tro loai san pham nay: "+type);
        }
    }
}
