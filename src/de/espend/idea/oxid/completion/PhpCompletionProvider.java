package de.espend.idea.oxid.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.smarty.SmartyFileType;
import de.espend.idea.oxid.OxidProjectComponent;
import de.espend.idea.oxid.utils.MetadataUtil;
import de.espend.idea.oxid.utils.ModuleUtil;
import de.espend.idea.oxid.utils.PhpMetadataUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpCompletionProvider extends CompletionContributor {

    public PhpCompletionProvider() {
        
        extend(
                CompletionType.BASIC, PlatformPatterns.psiElement(),
                new CompletionProvider<CompletionParameters>() {
                    @Override
                    protected void addCompletions(final @NotNull CompletionParameters parameters, ProcessingContext context, final @NotNull CompletionResultSet result) {

                        PsiElement originalPosition = parameters.getOriginalPosition();
                        if(originalPosition == null || !OxidProjectComponent.isValidForProject(originalPosition)) {
                            return;
                        }

                        PsiElement parent = originalPosition.getParent();
                        if(!(parent instanceof StringLiteralExpression) || !PhpMetadataUtil.isModuleKeyInFlatArray((StringLiteralExpression) parent, "extend")) {
                            return;
                        }

                        ModuleUtil.visitModuleFile(originalPosition.getContainingFile(), new ModuleUtil.ModuleFileVisitor() {
                            @Override
                            public void visit(@NotNull VirtualFile virtualFile, @NotNull String relativePath) {

                                if (virtualFile.getFileType() != PhpFileType.INSTANCE || !relativePath.endsWith(".php")) {
                                    return;
                                }

                                result.addElement(LookupElementBuilder.create(relativePath.substring(0, relativePath.length() - 4)).withIcon(virtualFile.getFileType().getIcon()));
                            }
                        });

                    }
                }
        );


        extend(
                CompletionType.BASIC, PlatformPatterns.psiElement(),
                new CompletionProvider<CompletionParameters>() {
                    @Override
                    protected void addCompletions(final @NotNull CompletionParameters parameters, ProcessingContext context, final @NotNull CompletionResultSet result) {

                        PsiElement originalPosition = parameters.getOriginalPosition();
                        if(originalPosition == null || !OxidProjectComponent.isValidForProject(originalPosition)) {
                            return;
                        }

                        PsiElement parent = originalPosition.getParent();
                        if(!(parent instanceof StringLiteralExpression) || !PhpMetadataUtil.isInTemplateWithKey((StringLiteralExpression) parent, "file")) {
                            return;
                        }

                        ModuleUtil.visitModuleTemplatesInMetadataScope(originalPosition.getContainingFile(), new ModuleUtil.ModuleFileVisitor() {
                            @Override
                            public void visit(@NotNull VirtualFile virtualFile, @NotNull String relativePath) {
                                result.addElement(LookupElementBuilder.create(relativePath).withIcon(virtualFile.getFileType().getIcon()));
                            }
                        });

                    }
                }
        );


        extend(
            CompletionType.BASIC, PlatformPatterns.psiElement(),
            new ModuleFileParametersCompletionProvider("files", PhpFileType.INSTANCE)
        );

        extend(
            CompletionType.BASIC, PlatformPatterns.psiElement(),
            new ModuleFileParametersCompletionProvider("templates", SmartyFileType.INSTANCE)
        );

    }

    private static class ModuleFileParametersCompletionProvider extends CompletionProvider<CompletionParameters> {

        @NotNull
        private final String key;

        @NotNull
        private final FileType fileType;

        public ModuleFileParametersCompletionProvider(@NotNull String key, @NotNull FileType fileType) {
            this.key = key;
            this.fileType = fileType;
        }

        @Override
        protected void addCompletions(final @NotNull CompletionParameters parameters, ProcessingContext context, final @NotNull CompletionResultSet result) {

            PsiElement originalPosition = parameters.getOriginalPosition();
            if(originalPosition == null || !OxidProjectComponent.isValidForProject(originalPosition)) {
                return;
            }

            PsiElement parent = originalPosition.getParent();
            if(!(parent instanceof StringLiteralExpression) || !PhpMetadataUtil.isModuleKeyInFlatArray((StringLiteralExpression) parent, key)) {
                return;
            }

            ModuleUtil.visitModuleFile(originalPosition.getContainingFile(), new ModuleUtil.ModuleFileVisitor() {
                @Override
                public void visit(@NotNull VirtualFile virtualFile, @NotNull String relativePath) {

                    if (virtualFile.getFileType() != fileType) {
                        return;
                    }

                    result.addElement(LookupElementBuilder.create(relativePath).withIcon(virtualFile.getFileType().getIcon()));
                }
            });

        }
    }
}
