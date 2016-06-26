package com.oroplatform.idea.oroplatform.schema;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class PropertyPath {

    private final List<Property> properties = new LinkedList<Property>();

    PropertyPath(String... properties) {
        this(getProperties(properties));
    }

    @NotNull
    private static List<Property> getProperties(String[] properties) {
        final List<Property> newProperties = new LinkedList<Property>();
        for (String property : properties) {
            newProperties.add("$this".equals(property) ? new Property(null, true) : new Property(property, false));
        }
        return newProperties;
    }

    private PropertyPath(List<Property> properties) {
        this.properties.addAll(properties);
    }

    public Queue<Property> getProperties() {
        return new LinkedList<Property>(properties);
    }

    public PropertyPath dropHead() {
        LinkedList<Property> newProperties = new LinkedList<Property>(properties);
        newProperties.poll();
        return new PropertyPath(newProperties);
    }

    public final static class Property {
        private final String name;
        private final boolean isThis;

        public Property(String name, boolean isThis) {
            this.name = name;
            this.isThis = isThis;
        }

        public String getName() {
            return name;
        }

        public boolean isThis() {
            return isThis;
        }
    }

}