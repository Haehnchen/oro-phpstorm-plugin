package com.oroplatform.idea.oroplatform.schema;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

public class Literal implements Element {

    private final Value value;

    public Literal() {
        this(new Any());
    }

    public Literal(Value value) {
        this.value = value;
    }

    public Literal(List<String> choices) {
        this(new Choices(choices));
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitLiteral(this);
    }

    public Value getValue() {
        return value;
    }

    public interface Value {
    }

    public static class Any implements Value {
    }

    public static class Choices implements Value {
        private final List<String> choices = new LinkedList<String>();

        public Choices(List<String> choices) {
            this.choices.addAll(choices);
        }

        public List<String> getChoices() {
            return Collections.unmodifiableList(choices);
        }
    }

    public static class PhpClass implements Value {
        private final Type type;

        public PhpClass(Type type) {
            this.type = type;
        }

        public Type getType() {
            return type;
        }

        public enum Type {
            Entity, Controller
        }
    }

    public static class PhpMethod implements Value {
        private final Pattern pattern;

        /**
         * @param pattern Simple pattern, use * for any string
         */
        public PhpMethod(String pattern) {
            this.pattern = Pattern.compile("^"+pattern.replace("*", ".*")+"$");
        }

        public PhpMethod() {
            this("*");
        }

        public boolean matches(String name) {
            return pattern.matcher(name).matches();
        }
    }
}
