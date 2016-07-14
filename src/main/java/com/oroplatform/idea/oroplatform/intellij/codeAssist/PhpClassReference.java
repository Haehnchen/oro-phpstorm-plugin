package com.oroplatform.idea.oroplatform.intellij.codeAssist;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamespace;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.oroplatform.idea.oroplatform.Icons;
import com.oroplatform.idea.oroplatform.PhpClassUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static java.util.Arrays.asList;

public class PhpClassReference extends PsiPolyVariantReferenceBase<PsiElement> {
    private final String text;
    private final InsertHandler<LookupElement> insertHandler;
    private final String rootBundlePath;
    private final com.oroplatform.idea.oroplatform.schema.PhpClass phpClass;
    private final Set<String> skippedClassNames = new HashSet<String>();
    private final PhpIndex phpIndex;
    private final PhpClass repositoryInterface;

    public PhpClassReference(PsiElement psiElement, com.oroplatform.idea.oroplatform.schema.PhpClass phpClass, @NotNull String text, InsertHandler<LookupElement> insertHandler, Set<String> skippedClassNames) {
        super(psiElement);
        this.phpClass = phpClass;
        this.insertHandler = insertHandler;
        this.text = text.replace(PsiElements.IN_PROGRESS_VALUE, "").trim().replace("\\\\", "\\");
        this.rootBundlePath = myElement.getContainingFile() == null ? "" : myElement.getContainingFile().getOriginalFile().getVirtualFile().getCanonicalPath().replaceFirst("/Resources/.*", "");
        this.skippedClassNames.addAll(skippedClassNames);
        this.phpIndex = PhpIndex.getInstance(psiElement.getProject());
        this.repositoryInterface = getRepositoryInterface(phpIndex);
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        final List<ResolveResult> results = new LinkedList<ResolveResult>();

        for(String className : resolveClassNames(text)) {
            for(PhpClass phpClass : phpIndex.getClassesByFQN(className)) {
                results.add(new PsiElementResolveResult(phpClass));
            }
        }

        return results.toArray(new ResolveResult[results.size()]);
    }

    private Collection<String> resolveClassNames(String text) {
        final Set<String> names = new HashSet<String>();
        names.add(text);

        if(text.contains(":")) {
            final String simpleName = PhpClassUtil.getSimpleName(text);
            final String namespaceShortcut = text.substring(0, text.indexOf(':'));
            final Collection<PhpClass> phpClasses = phpIndex.getClassesByName(simpleName);
            final PrefixMatcher matcher = new StrictCamelHumpMatcher(namespaceShortcut);

            for(PhpClass phpClass : phpClasses) {
                final String simplifiedNamespace = phpClass.getNamespaceName().replace("\\Bundle\\", "").replace("\\Entity\\", "").replace("\\", "");
                if(matcher.isStartMatch(simplifiedNamespace)) {
                    names.add(phpClass.getFQN());
                }
            }
        }

        return names;
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        if(phpClass.getNamespacePart() != null) {
            return getVariantsFromBundles();
        } else {
            return getVariantsFromAnyNamespace();
        }
    }

    @NotNull
    private Object[] getVariantsFromBundles() {
        final List<LookupElement> results = new LinkedList<LookupElement>();

        for (PhpClass phpClass : getPhpClassesFrom(getBundlesNamespaceNames())) {
            final int priority = getPriorityFor(phpClass);
            if(this.phpClass.allowDoctrineShortcutNotation()) {
                addEntitiesShortcutsLookups(results, phpClass, priority);
            } else {
                results.add(PrioritizedLookupElement.withPriority(new PhpClassLookupElement(phpClass, true, getPhpClassInsertHandler()), priority));
            }
        }

        return results.toArray();
    }

    private Collection<PhpClass> getPhpClassesFrom(Collection<String> namespaceNames) {
        final Set<PhpClass> phpClasses = new HashSet<PhpClass>();


        for (String namespaceName : namespaceNames) {

            for (PhpNamespace phpNamespace : phpIndex.getNamespacesByName(namespaceName)) {
                for (PhpClass phpClass : getPhpClassesFrom(phpNamespace)) {
                    final boolean isClass = !phpClass.isInterface() && !phpClass.isTrait();
                    if(isClass && !skippedClassNames.contains(phpClass.getFQN()) &&
                        (!this.phpClass.getNamespacePart().equals("Entity") || isEntity(phpClass))) {
                        phpClasses.add(phpClass);
                    }
                }
            }
        }

        return phpClasses;
    }

    @Nullable
    private static PhpClass getRepositoryInterface(PhpIndex phpIndex) {
        final Collection<PhpClass> repositoryInterfaces = phpIndex.getInterfacesByFQN("\\Doctrine\\Common\\Persistence\\ObjectRepository");
        return repositoryInterfaces.isEmpty() ? null : repositoryInterfaces.iterator().next();
    }

    private boolean isEntity(PhpClass phpClass) {
        return (repositoryInterface == null || !isInstanceOf(phpClass, repositoryInterface)) && !phpClass.getName().endsWith("Manager");
    }

    private boolean isInstanceOf(PhpClass subjectClass, PhpClass expectedClass) {
        return new PhpType().add(expectedClass).isConvertibleFrom(new PhpType().add(subjectClass), phpIndex);
    }

    private Collection<PhpClass> getPhpClassesFrom(PhpNamespace phpNamespace) {
        final List<PhpClass> phpClasses = PsiTreeUtil.getChildrenOfTypeAsList(phpNamespace.getStatements(), PhpClass.class);

        final String namespaceName = phpNamespace.getFQN().toLowerCase() + "\\";
        for (String parentNamespaceName : phpIndex.getChildNamespacesByParentName(namespaceName)) {
            for (PhpNamespace parentNamespace : phpIndex.getNamespacesByName(namespaceName + parentNamespaceName)) {
                phpClasses.addAll(getPhpClassesFrom(parentNamespace));
            }
        }

        return phpClasses;
    }

    private Collection<String> getBundlesNamespaceNames() {
        Collection<PhpClass> classes = phpIndex.getAllSubclasses("\\Symfony\\Component\\HttpKernel\\Bundle\\Bundle");
        Collection<String> namespaces = new HashSet<String>();

        for (PhpClass phpClass : classes) {
            namespaces.add(phpClass.getNamespaceName()+this.phpClass.getNamespacePart());
        }

        return namespaces;
    }

    private int getPriorityFor(PhpClass phpClass) {
        int priority = 150;

        if(isFromVendors(phpClass)) {
            priority -= 50;
        }

        final String classRootPath = phpClass.getNamespaceName().replace("\\", "/").replaceFirst("/"+this.phpClass.getNamespacePart()+"/.*", "");
        if(!rootBundlePath.endsWith(classRootPath)) {
            priority -= 50;
        }

        return priority;
    }

    private boolean isFromVendors(@NotNull PhpClass phpClass) {
        //sad null pointer checks
        return phpClass.getContainingFile() != null && phpClass.getContainingFile().getVirtualFile() != null &&
                phpClass.getContainingFile().getVirtualFile().getCanonicalPath() != null &&
                phpClass.getContainingFile().getVirtualFile().getCanonicalPath().contains("/vendor/");
    }

    private void addEntitiesShortcutsLookups(List<LookupElement> results, PhpClass phpClass, int priority) {
        final String shortcutName = PhpClassUtil.getDoctrineShortcutClassName(phpClass.getPresentableFQN());
        if(shortcutName != null) {
            results.add(PrioritizedLookupElement.withPriority(
                LookupElementBuilder.create(shortcutName)
                    .withIcon(Icons.DOCTRINE)
                    .withTypeText(phpClass.getPresentableFQN())
                    .withInsertHandler(insertHandler)
                    .withLookupString(StringUtil.trimLeading(phpClass.getFQN(), '\\')),
                priority
            ));
        }
    }

    @NotNull
    private Object[] getVariantsFromAnyNamespace() {
        final List<LookupElement> results = new LinkedList<LookupElement>();

        for (String className : getAllPhpClassNames()) {
            for (PhpClass phpClass : phpIndex.getClassesByName(className)) {
                if(!PhpClassUtil.isTestOrGeneratedClass(phpClass.getPresentableFQN())) {
                    results.add(new PhpClassLookupElement(phpClass, true, getPhpClassInsertHandler()));
                }
            }
        }

        return results.toArray(new LookupElement[results.size()]);
    }

    private InsertHandler<LookupElement> getPhpClassInsertHandler() {
        return insertHandler != null ?
                    new ComposedInsertHandler(asList(PhpClassInsertHandler.INSTANCE, insertHandler)) : PhpClassInsertHandler.INSTANCE;
    }

    private Collection<String> getAllPhpClassNames() {
        final PrefixMatcher classMatcher = new CamelHumpMatcher(text);
        return phpIndex.getAllClassNames(classMatcher);
    }

    private static class StrictCamelHumpMatcher extends CamelHumpMatcher {
        private final String prefixUpperLetters;

        StrictCamelHumpMatcher(@NotNull String prefix) {
            super(prefix);
            this.prefixUpperLetters = getUpperLetters(prefix);
        }

        @NotNull
        private String getUpperLetters(@NotNull String prefix) {
            final StringBuilder prefixUpperLetters = new StringBuilder();

            for (int i=0; i<prefix.length(); i++) {
                char c = prefix.charAt(i);
                if(Character.isUpperCase(c)) {
                    prefixUpperLetters.append(c);
                }
            }
            return prefixUpperLetters.toString();
        }

        @Override
        public boolean isStartMatch(String name) {
            return super.isStartMatch(name) && getUpperLetters(name).equals(prefixUpperLetters);
        }
    }
}
