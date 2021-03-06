package com.oroplatform.idea.oroplatform.intellij.codeAssist.yml.completionProvider;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.util.ProcessingContext;
import com.oroplatform.idea.oroplatform.intellij.codeAssist.yml.YamlPsiElements;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ChoiceCompletionProvider extends CompletionProvider<CompletionParameters> {

    private final List<Choice> choices = new LinkedList<Choice>();
    private final InsertHandler<LookupElement> insertHandler;

    public static ChoiceCompletionProvider fromChoiceNames(List<String> choices, InsertHandler<LookupElement> insertHandler) {
        return new ChoiceCompletionProvider(toChoices(choices), insertHandler);
    }

    private static List<Choice> toChoices(List<String> names) {
        List<Choice> choices = new LinkedList<Choice>();
        for (String name : names) {
            choices.add(new Choice(name, null));
        }

        return choices;
    }

    public ChoiceCompletionProvider(List<Choice> choices, InsertHandler<LookupElement> insertHandler) {
        this.insertHandler = insertHandler;
        this.choices.addAll(choices);
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet result) {
        final Set<String> existingProperties = getExistingChoices(parameters);
        result = result.withPrefixMatcher(new PlainPrefixMatcher(result.getPrefixMatcher().getPrefix()));

        for(Choice choice : choices) {
            if(!existingProperties.contains(choice.getName())) {
                result.addElement(LookupElementBuilder.create(choice.getName()).withInsertHandler(insertHandler).withTypeText(choice.getDescription(), true));
            }
        }
    }

    @NotNull
    private Set<String> getExistingChoices(@NotNull CompletionParameters parameters) {
        final YAMLMapping mapping = YamlPsiElements.getFirstMapping(parameters.getPosition(), /* set max depth in order to skip mapping if property is not followed by colon */ 3);
        final Set<String> existingProperties = new HashSet<String>();

        if (mapping != null) {
            for (YAMLKeyValue keyValue : mapping.getKeyValues()) {
                existingProperties.add(keyValue.getKeyText());
            }
        }
        return existingProperties;
    }

    public static class Choice {
        private final String name;
        private final String description;

        public Choice(String name, String description) {
            this.name = name;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        String getDescription() {
            return description;
        }
    }
}
