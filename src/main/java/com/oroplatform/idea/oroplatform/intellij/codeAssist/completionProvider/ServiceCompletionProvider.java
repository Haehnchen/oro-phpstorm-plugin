package com.oroplatform.idea.oroplatform.intellij.codeAssist.completionProvider;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.util.ProcessingContext;
import com.oroplatform.idea.oroplatform.intellij.indexes.ServicesIndex;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class ServiceCompletionProvider extends CompletionProvider<CompletionParameters> {

    private final InsertHandler<LookupElement> insertHandler;

    public ServiceCompletionProvider(InsertHandler<LookupElement> insertHandler) {
        this.insertHandler = insertHandler;
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet result) {
        final Project project = parameters.getOriginalFile().getProject();

        final Collection<String> actions = ServicesIndex.instance(project).findServices();

        for (String service : actions) {
            result.addElement(LookupElementBuilder.create(service).withInsertHandler(insertHandler));
        }
    }
}
