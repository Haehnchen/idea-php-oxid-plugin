package de.espend.idea.oxid.dict.metadata;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class MetadataSetting {

    @Nullable
    private final String group;

    @Nullable
    private final String name;

    @Nullable
    private final String type;

    public MetadataSetting(@Nullable String group, @Nullable String name, @Nullable String type) {
        this.group = group;
        this.name = name;
        this.type = type;
    }

    @Nullable
    public String getGroup() {
        return group;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public String getType() {
        return type;
    }

    @Nullable
    public static MetadataSetting create(@NotNull Map<String, String> map) {

        if(!map.containsKey("group") && !map.containsKey("name") && !map.containsKey("type")) {
            return null;
        }

        return new MetadataSetting(
            map.get("group"),
            map.get("name"),
            map.get("type")
        );
    }


}
