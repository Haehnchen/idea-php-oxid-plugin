package de.espend.idea.oxid.navigation;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.smarty.SmartyFileType;
import de.espend.idea.oxid.OxidProjectComponent;
import de.espend.idea.oxid.utils.ModuleUtil;
import de.espend.idea.oxid.utils.PhpMetadataUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpGoToHandler implements GotoDeclarationHandler {

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int i, Editor editor) {

        if(!OxidProjectComponent.isValidForProject(psiElement)) {
            return new PsiElement[0];
        }

        Collection<PsiElement> psiElements = new ArrayList<PsiElement>();

        PsiElement parent = psiElement.getParent();
        if(parent instanceof StringLiteralExpression) {

            if(PhpMetadataUtil.isModuleKeyInFlatArray((StringLiteralExpression) parent, "extend")) {
                attachMetadataExtends((StringLiteralExpression) parent, psiElements);
            }

            if(PhpMetadataUtil.isModuleKeyInFlatArray((StringLiteralExpression) parent, "files")) {
                attachAllFileTypes((StringLiteralExpression) parent, psiElements, PhpFileType.INSTANCE);
            }

            if(PhpMetadataUtil.isModuleKeyInFlatArray((StringLiteralExpression) parent, "templates")) {
                attachAllFileTypes((StringLiteralExpression) parent, psiElements, SmartyFileType.INSTANCE);
            }

            if(PhpMetadataUtil.isInTemplateWithKey((StringLiteralExpression) parent, "file")) {
                attachTemplateFileTypes((StringLiteralExpression) parent, psiElements);
            }

        }

        return psiElements.toArray(new PsiElement[psiElements.size()]);
    }

    private void attachTemplateFileTypes(StringLiteralExpression psiElement, Collection<PsiElement> psiElements) {

        final String contents = getPathFormattedString(psiElement);
        if (contents == null) {
            return;
        }

        ModuleUtil.visitModuleTemplatesInMetadataScope(psiElement.getContainingFile(), new ContentEqualModuleFileVisitor(SmartyFileType.INSTANCE, contents, psiElement, psiElements));
    }

    private void attachMetadataExtends(@NotNull final StringLiteralExpression psiElement, final @NotNull Collection<PsiElement> psiElements) {

        final String contents = getPathFormattedString(psiElement);
        if (contents == null) {
            return;
        }

        ModuleUtil.visitModuleFile(psiElement.getContainingFile(), new ModuleUtil.ModuleFileVisitor() {
            @Override
            public void visit(@NotNull VirtualFile virtualFile, @NotNull String relativePath) {

                if (virtualFile.getFileType() != PhpFileType.INSTANCE || !relativePath.toLowerCase().endsWith(".php") || !contents.equalsIgnoreCase(relativePath.substring(0, relativePath.length() - 4))) {
                    return;
                }

                PsiFile file = PsiManager.getInstance(psiElement.getProject()).findFile(virtualFile);
                if(file != null) {
                    psiElements.add(file);
                }

            }
        });

    }

    private void attachAllFileTypes(@NotNull final StringLiteralExpression psiElement, final @NotNull Collection<PsiElement> psiElements, final FileType fileType) {

        final String contents = getPathFormattedString(psiElement);
        if (contents == null) {
            return;
        }

        ModuleUtil.visitModuleFile(psiElement.getContainingFile(), new ContentEqualModuleFileVisitor(fileType, contents, psiElement, psiElements));
    }

    @Nullable
    private String getPathFormattedString(@NotNull StringLiteralExpression psiElement) {
        String contents = psiElement.getContents();
        if(StringUtils.isBlank(contents)) {
            return null;
        }

        if(contents.startsWith("/")) {
            contents = contents.substring(1);
        }

        return contents;
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        return null;
    }

    private static class ContentEqualModuleFileVisitor implements ModuleUtil.ModuleFileVisitor {
        private final FileType fileType;
        private final String contents;
        private final StringLiteralExpression psiElement;
        private final Collection<PsiElement> psiElements;

        public ContentEqualModuleFileVisitor(FileType fileType, String contents, StringLiteralExpression psiElement, Collection<PsiElement> psiElements) {
            this.fileType = fileType;
            this.contents = contents;
            this.psiElement = psiElement;
            this.psiElements = psiElements;
        }

        @Override
        public void visit(@NotNull VirtualFile virtualFile, @NotNull String relativePath) {

            if (virtualFile.getFileType() != fileType || !contents.equalsIgnoreCase(relativePath)) {
                return;
            }

            PsiFile file = PsiManager.getInstance(psiElement.getProject()).findFile(virtualFile);
            if(file != null) {
                psiElements.add(file);
            }

        }
    }
}
