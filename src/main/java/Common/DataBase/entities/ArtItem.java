package Common.DataBase.entities;

import Common.Enum.ItemStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ArtItem extends Item{
    private String artistName;
    public ArtItem(long id, long winner_id, long beginPrice, ItemStatus status, String artistName){
        super(id, winner_id, beginPrice, status);
        this.artistName = artistName;
    }
}
