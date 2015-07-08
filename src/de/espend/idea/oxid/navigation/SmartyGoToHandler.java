package de.espend.idea.oxid.navigation;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import de.espend.idea.oxid.OxidProjectComponent;
import de.espend.idea.oxid.utils.SmartyPattern;
import de.espend.idea.oxid.utils.TemplateUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SmartyGoToHandler implements GotoDeclarationHandler {
    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(@Nullable PsiElement psiElement, int i, Editor editor) {

        if(!OxidProjectComponent.isValidForProject(psiElement)) {
            return new PsiElement[0];
        }

        Collection<PsiElement> psiElements = new ArrayList<PsiElement>();

        if(SmartyPattern.getFilePattern().accepts(psiElement)) {
            attachFiles(psiElements, psiElement);
        }

        return psiElements.toArray(new PsiElement[psiElements.size()]);
    }

    private void attachFiles(@NotNull Collection<PsiElement> psiElements, @NotNull PsiElement psiElement) {

        final String text = psiElement.getText();
        if(StringUtils.isBlank(text)) {
            return;
        }

        attachTemplateFiles(psiElement.getProject(), text, psiElements);
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        return null;
    }

    private void attachTemplateFiles(final Project project, final @NotNull String s, final @NotNull Collection<PsiElement> psiElements) {
        TemplateUtil.collectFiles(project, new TemplateUtil.SmartyTemplateVisitor() {
            @Override
            public void visitFile(VirtualFile virtualFile, String fileName) {
                if(s.equals(fileName)) {
                    PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
                    if(file != null) {
                        psiElements.add(file);
                    }
                }
            }
        });
    }

}
