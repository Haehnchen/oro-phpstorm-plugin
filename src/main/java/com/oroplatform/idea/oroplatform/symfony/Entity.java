package com.oroplatform.idea.oroplatform.symfony;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class Entity {
    private final String shortcutName;
    private final Bundle bundle;
    private final String simpleName;
    private final String fqn;

    @Nullable
    public static Entity fromFqn(String fqn) {
        final String fixedFQN = StringUtil.trimStart(fqn, "\\");
        final List<String> parts = Arrays.asList(fixedFQN.split("\\\\"));
        final int entityIndex = parts.indexOf("Entity");

        if(parts.size() < 3 || entityIndex <= 0 || entityIndex == parts.size() - 1) return null;

        final Bundle bundle = new Bundle(parts.subList(0, entityIndex));

        return new Entity(bundle.getName()+":"+StringUtil.join(parts.subList(entityIndex + 1, parts.size()), "\\"), fqn, bundle, parts.get(parts.size() - 1));
    }

    private Entity(String shortcutName, String fqn, Bundle bundle, String simpleName) {
        this.shortcutName = shortcutName;
        this.fqn = fqn;
        this.bundle = bundle;
        this.simpleName = simpleName;
    }

    public String getShortcutName() {
        return shortcutName;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public Bundle getBundle() {
        return bundle;
    }
}
