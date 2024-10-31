package net.xincraft.systems.match.cage;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Material;
import org.bukkit.material.MaterialData;

@Getter
@NoArgsConstructor
public class CageBlock {
    private Material type;
    private byte data;

    public CageBlock(Material type, MaterialData state) {
        this.type = type;
        this.data = state.getData(); // this fucking sucks, but I can't see a way around it
    }

    @Override
    public String toString() {
        return type.name().toCharArray()[0] + "";
    }

    @JsonIgnore public MaterialData getMaterialData() {
        return new MaterialData(type, data); // this fucking sucks, but I can't see a way around it
    }
}
