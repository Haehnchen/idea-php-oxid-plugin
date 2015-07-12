package de.espend.idea.oxid.utils;

import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileWriter;
import java.io.IOException;
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

        for (Map.Entry<String, Set<VirtualFile>> entry : ModuleUtil.getExtendsList(project).entrySet()) {

            String key = entry.getKey();
            PhpClass parentClass = PhpElementsUtil.getClassInterface(project, key);
            if(parentClass == null) {
                System.out.println("err1" + key);
                continue;
            }

            String className = parentClass.getPresentableFQN();
            if(className == null) {
                System.out.println("err2" + key);
                continue;
            }

            Set<String> subClasses = new HashSet<String>();

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

                    subClasses.add(fqn);

                }

            }

            for (String subClass : subClasses) {
                s.append(String.format("    class %s extends %s {}\n", subClass, className));
            }

        }

        String content = s.toString();
        if(StringUtils.isBlank(content)) {
            return null;
        }

        String info = String.format("%s / %s / OXID Plugin %s",
            ApplicationInfo.getInstance().getVersionName(),
            ApplicationInfo.getInstance().getBuild(),
            PluginManager.getPlugin(PluginId.getId("de.espend.idea.oxid")).getVersion()
        );

        return "<?php\n" +
                "/**\n" +
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

    public static void buildClassMetadataFile(final @NotNull Project project) {

        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {

                final String content = OxidUtil.getPseudoClassOverwrites(project);
                if(content == null) {
                    return;
                }

                FileWriter fw;
                try {
                    fw = new FileWriter(project.getBaseDir().getPath() + "/" + PHPSTORM_OXID_META_PHP);
                    fw.write(content);
                    fw.close();
                } catch (IOException ignored) {
                }

            }
        });

    }

}
