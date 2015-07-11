package de.espend.idea.oxid.dict.metadata;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class MetadataSetting {

    @NotNull
    private final PsiElement source;
    @Nullable
    private final String group;

    @Nullable
    private final String name;

    @Nullable
    private final String type;

    private MetadataSetting(@NotNull PsiElement source, @Nullable String group, @Nullable String name, @Nullable String type) {
        this.source = source;
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

    @NotNull
    public PsiElement getSource() {
        return source;
    }

    @Nullable
    public static MetadataSetting create(@NotNull PsiElement source, @NotNull Map<String, String> map) {

        if(!map.containsKey("group") && !map.containsKey("name") && !map.containsKey("type")) {
            return null;
        }

        return new MetadataSetting(
            source,
            map.get("group"),
            map.get("name"),
            map.get("type")
        );
    }


}
