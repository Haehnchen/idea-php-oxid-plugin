package de.espend.idea.oxid.utils;

import com.intellij.codeInsight.completion.impl.CamelHumpMatcher;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.completion.PhpClassLookupElement;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.completion.PhpClassReferenceInsertHandler;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class OxidUtil {

    private static final String PHPSTORM_OXID_META_PHP = ".phpstorm-oxid.meta.php";

    public static boolean isFactory(@NotNull StringLiteralExpression literalExpression) {

        PsiElement parameterList = literalExpression.getParent();
        if(parameterList instanceof ParameterList) {
            PsiElement function = parameterList.getParent();
            if(function instanceof FunctionReference) {
                if("oxNew".equalsIgnoreCase(((FunctionReference) function).getName())) {
                    return true;
                }
            }
        }

        return new MethodMatcher.StringParameterRecursiveMatcher(literalExpression, 0)
                .withSignature("\\oxRegistry", "get")
                .match() != null;
    }

    @Nullable
    public static String getPseudoClassOverwrites(@NotNull Project project) {

        StringBuilder s = new StringBuilder();

        Map<String, String> traits = new HashMap<String, String>();

        for (Map.Entry<String, Set<VirtualFile>> entry : ModuleUtil.getExtendsList(project).entrySet()) {

            String key = entry.getKey();
            PhpClass parentClass = PhpElementsUtil.getClassInterface(project, key);
            if(parentClass == null) {
                continue;
            }

            String className = parentClass.getPresentableFQN();
            if(className == null) {
                continue;
            }

            Map<String, String> subClasses = new HashMap<String, String>();

            for (VirtualFile virtualFile : entry.getValue()) {

                PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
                if(!(file instanceof PhpFile)) {
                    continue;
                }

                Collection<PhpClass> allClasses = PhpPsiUtil.findAllClasses((PhpFile) file);
                if(allClasses.size() == 0) {
                    continue;
                }

                PhpClass phpClass = allClasses.iterator().next();
                ExtendsList extendsList = phpClass.getExtendsList();
                List<ClassReference> referenceElements = extendsList.getReferenceElements();
                if(referenceElements != null && referenceElements.size() > 0) {
                    ClassReference next = referenceElements.iterator().next();

                    String fqn = next.getFQN();
                    if(fqn == null) {
                        continue;
                    }

                    if(fqn.startsWith("\\")) {
                        fqn = fqn.substring(1);
                    }

                    if(subClasses.containsKey(fqn))  {
                        continue;
                    }

                    String originClass = phpClass.getFQN();
                    if(originClass == null) {
                        continue;
                    }

                    subClasses.put(fqn, originClass);

                    if(originClass.startsWith("\\")) {
                        originClass = originClass.substring(1);
                    }

                    traits.put(originClass, getDummyTraitContent(phpClass));
                }

            }

            for (final Map.Entry<String, String> entryC : subClasses.entrySet()) {
                s.append(String.format("class %s extends %s { %s }\n", entryC.getKey(), className, buildTraitUses(subClasses, entryC)));
            }

        }

        String content = s.toString();
        if(StringUtils.isBlank(content)) {
            return null;
        }

        for (Map.Entry<String, String> entry : traits.entrySet()) {
            content += String.format("trait %sTrait {\n %s \n }\n", entry.getKey(), entry.getValue());
        }

        if(StringUtils.isBlank(content)) {
            return null;
        }

        String info = String.format("%s / %s / OXID Plugin %s",
            ApplicationInfo.getInstance().getVersionName(),
            ApplicationInfo.getInstance().getBuild(),
            PluginManager.getPlugin(PluginId.getId("de.espend.idea.oxid")).getVersion()
        );

        return  "/**\n" +
                " * An helper file for OXID, to provide autocomplete information to your IDE\n" +
                " * Generated with " + info + " on " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime()) + ".\n" +
                " *\n" +
                " * @author Daniel Espendiller <daniel@espendiller.net>\n" +
                " * @see https://github.com/Haehnchen/idea-php-oxid-plugin\n" +
                " */\n" +
                "\n" +
                "namespace {\n" +
                "    exit(\"This file should not be included, only analyzed by your IDE\");\n" +
                content +
                "\n}";
    }

    private static String getDummyTraitContent(@NotNull PhpClass phpClass) {

        StringBuilder dummyMethods = new StringBuilder();

        for (Method method : phpClass.getOwnMethods()) {
            String emptyMethod = getEmptyMethod(method);
            if(emptyMethod != null) {
                dummyMethods.append(emptyMethod);
            }
        }

        for (Field field : ContainerUtil.filter(phpClass.getOwnFields(), new MyDummyFieldCondition())) {
            String emptyMethod = getEmptyField(field);
            if(emptyMethod != null) {
                dummyMethods.append(emptyMethod);
            }
        }

        return dummyMethods.toString();
    }

    @NotNull
    private static String buildTraitUses(@NotNull Map<String, String> subClasses, @NotNull Map.Entry<String, String> entryC) {
        Set<String> filter = new HashSet<String>();

        // filter to find all foreign traits
        // we dont want to add self one
        for (Map.Entry<String, String> stringStringEntry : subClasses.entrySet()) {
            if(entryC.getKey().equals(stringStringEntry.getKey())) {
                continue;
            }
            filter.add(stringStringEntry.getValue() + "Trait");
        }

        if(filter.size() == 0) {
            return "";
        }

        return String.format("\n use %s;\n", StringUtils.join(filter, ", "));
    }

    public static void buildClassMetadataFile(final @NotNull Project project) {

        final String[] content1 = new String[] {null};

        ApplicationManager.getApplication().runReadAction(() -> {
            final String content = OxidUtil.getPseudoClassOverwrites(project);
            if (content == null) {
                return;
            }
            content1[0] = content;
        });

        if(content1[0] == null) {
            return;
        }

        new WriteCommandAction<Void>(project, null) {
            @Override
            protected void run(@NotNull Result<Void> result) throws Throwable {

                VirtualFile relativeFile = VfsUtil.findRelativeFile(project.getBaseDir(), PHPSTORM_OXID_META_PHP);

                if(relativeFile == null) {
                    PsiDirectory directory = PsiManager.getInstance(getProject()).findDirectory(project.getBaseDir());
                    if(directory == null) {
                        return;
                    }

                    relativeFile = directory.createFile(PHPSTORM_OXID_META_PHP).getVirtualFile();
                }

                if(relativeFile == null) {
                    return;
                }

                final PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(relativeFile);
                if(psiFile == null || psiFile.getFirstChild() == null) {
                    return;
                }

                final PsiFile psiFileFromText = PhpPsiElementFactory.createPsiFileFromText(project, content1[0]);
                CodeStyleManager.getInstance(project).reformat(psiFileFromText);
                PsiElement firstChild = psiFileFromText.getFirstChild();
                if(firstChild == null) {
                    return;
                }

                psiFile.getFirstChild().replace(firstChild);
            }
        }.execute();

    }

    public static Collection<LookupElement> getOverloadAbleClasses(@NotNull Project project, @NotNull String contents) {

        Collection<LookupElement> elements = new ArrayList<LookupElement>();

        // @TODO: is there a class filter on oxid, so we provide completion only for
        // "extends" classes
        PhpIndex phpIndex = PhpIndex.getInstance(project);
        for (String name : phpIndex.getAllClassNames(new CamelHumpMatcher(contents))) {
            for (PhpClass phpClass : phpIndex.getClassesByName(name)) {
                elements.add(new PhpClassLookupElement(phpClass, true, PhpClassReferenceInsertHandler.getInstance()));
            }
        }

        return elements;
    }

    private static class MyDummyFieldCondition implements Condition<Field> {
        @Override
        public boolean value(Field field) {
            return !field.isConstant() && (field.getModifier().getAccess().isPublic() || field.getModifier().getAccess().isProtected());
        }
    }


    @Nullable
    private static String getEmptyMethod(@NotNull Method method) {

        StringBuilder s = new StringBuilder();
        if(!(method.getAccess().isPublic() || method.getAccess().isProtected())) {
            return null;
        }

        String methodSignatureLine = method.getText();
        int i = methodSignatureLine.indexOf("{");
        if(i <= 0) {
            return null;
        }

        String docText = "/**\n*/";

        PhpDocComment docComment = method.getDocComment();
        if(docComment != null) {
            String docTextOrigin = docComment.getText();
            if(StringUtils.isNotBlank(docTextOrigin)) {
                docText = docTextOrigin;
            }
        }

        PhpClass containingClass = method.getContainingClass();
        if(containingClass == null) {
            return null;
        }

        String fqn = containingClass.getFQN();
        if(fqn == null) {
            return null;
        }

        s.append(docText.replace("*/",  String.format("* @see %s::%s\n*/", fqn, method.getName())));
        s.append((methodSignatureLine.substring(0, i) + "{}").replaceAll("\\r\\n|\\r|\\n", " ").replaceAll(" +", " ")).append("\n");

        return s.toString();
    }

    @Nullable
    private static String getEmptyField(@NotNull Field field) {

        StringBuilder s = new StringBuilder();
        if(!(field.getModifier().getAccess().isPublic() || field.getModifier().getAccess().isProtected())) {
            return null;
        }

        String modifierName = field.getModifier().toString();

        PsiElement nameIdentifier = field.getNameIdentifier();
        if(nameIdentifier == null) {
            return null;
        }

        String varName = nameIdentifier.getText();
        if(!varName.startsWith("$")) {
            return null;
        }

        String docText = "/**\n*/";

        PhpDocComment docComment = field.getDocComment();
        if(docComment != null) {
            String text1 = docComment.getText();
            if(StringUtils.isNotBlank(text1)) {
                docText = text1;
            }
        }

        PhpClass containingClass = field.getContainingClass();
        if(containingClass == null) {
            return null;
        }

        String fqn = containingClass.getFQN();
        if(fqn == null) {
            return null;
        }

        s.append(docText.replace("*/",  String.format("* @see %s::%s\n*/", field.getContainingClass().getFQN(), field.getName())));
        s.append(String.format("%s %s;", modifierName, varName));

        return s.toString();
    }
}
