package de.espend.idea.oxid.dict.metadata;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class MetadataBlock {

    @Nullable
    private final String template;

    @Nullable
    private final String block;

    @Nullable
    private final String file;

    public MetadataBlock(@Nullable String template, @Nullable String block, @Nullable String file) {
        this.template = template;
        this.block = block;
        this.file = file;
    }

    @Nullable
    public String getTemplate() {
        return template;
    }

    @Nullable
    public String getBlock() {
        return block;
    }

    @Nullable
    public String getFile() {
        return file;
    }

    @Nullable
    public static MetadataBlock create(@NotNull Map<String, String> map) {

        if(!map.containsKey("template") && !map.containsKey("block") && !map.containsKey("file")) {
            return null;
        }

        return new MetadataBlock(
            map.get("template"),
            map.get("block"),
            map.get("file")
        );
    }

}
