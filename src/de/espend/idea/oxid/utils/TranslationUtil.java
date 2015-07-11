package de.espend.idea.oxid.utils;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationUtil {

    public static Collection<LookupElement> getTranslationLookupElements(@NotNull Project project) {

        final Set<String> keys = new HashSet<String>();

        for (VirtualFile virtualFile : FilenameIndex.getAllFilesByExt(project, "php", GlobalSearchScope.allScope(project))) {

            if (!virtualFile.getName().endsWith("_lang.php")) {
                continue;
            }

            PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
            if (file != null) {
                MetadataUtil.visitTranslationKey(file, new MetadataUtil.TranslationKeyVisitor() {
                    @Override
                    public void visit(@NotNull String name, @NotNull PsiElement value) {
                        keys.add(name);
                    }
                });
            }
        }

        Collection<LookupElement> elements = new ArrayList<LookupElement>();

        for (String setting : keys) {
            elements.add(LookupElementBuilder.create(setting).withIcon(Symfony2Icons.TRANSLATION));
        }

        return elements;
    }

    public static Collection<PsiElement> getTranslationTargets(@NotNull Project project, final @NotNull String translationName) {

        final Collection<PsiElement> targets = new ArrayList<PsiElement>();

        for (VirtualFile virtualFile : FilenameIndex.getAllFilesByExt(project, "php", GlobalSearchScope.allScope(project))) {

            if (!virtualFile.getName().endsWith("_lang.php")) {
                continue;
            }

            PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
            if (file != null) {
                MetadataUtil.visitTranslationKey(file, new MetadataUtil.TranslationKeyVisitor() {
                    @Override
                    public void visit(@NotNull String name, @NotNull PsiElement value) {
                        if(name.equalsIgnoreCase(translationName)) {
                            targets.add(value);
                        }

                    }
                });
            }
        }

        return targets;
    }
}
