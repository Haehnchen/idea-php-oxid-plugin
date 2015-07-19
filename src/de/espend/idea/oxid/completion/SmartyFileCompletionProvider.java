package de.espend.idea.oxid.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ProcessingContext;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.smarty.SmartyFileType;
import de.espend.idea.oxid.OxidProjectComponent;
import de.espend.idea.oxid.utils.TemplateUtil;
import de.espend.idea.oxid.utils.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.SymfonyProcessors;
import org.jetbrains.annotations.NotNull;
import de.espend.idea.oxid.utils.SmartyPattern;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SmartyFileCompletionProvider extends CompletionContributor {
    
    public SmartyFileCompletionProvider() {
        
        extend(
                CompletionType.BASIC, SmartyPattern.getFilePattern(),
                new CompletionProvider<CompletionParameters>() {
                    @Override
                    protected void addCompletions(final @NotNull CompletionParameters parameters, ProcessingContext context, final @NotNull CompletionResultSet result) {

                        if(!OxidProjectComponent.isValidForProject(parameters.getOriginalPosition())) {
                            return;
                        }

                        attachTemplateFiles(parameters, result);
                    }
                }
        );

        extend(
                CompletionType.BASIC, SmartyPattern.getBlockPattern(),
                new CompletionProvider<CompletionParameters>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, final @NotNull CompletionResultSet result) {

                        PsiElement originalPosition = parameters.getOriginalPosition();
                        if(!OxidProjectComponent.isValidForProject(originalPosition)) {
                            return;
                        }

                        Set<String> templateNames = TemplateUtil.getTemplateNames(originalPosition.getProject(), originalPosition.getContainingFile().getVirtualFile());
                        if(templateNames.size() == 0) {
                            return;
                        }

                        result.addAllElements(TemplateUtil.getBlockFileLookupElements(parameters.getPosition().getProject(), templateNames.toArray(new String[templateNames.size()])));
                    }

                }

        );

        extend(
                CompletionType.BASIC, SmartyPattern.getAttributeInsideTagPattern("ident", "oxmultilang"),
                new CompletionProvider<CompletionParameters>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, final @NotNull CompletionResultSet result) {

                        PsiElement originalPosition = parameters.getOriginalPosition();
                        if(!OxidProjectComponent.isValidForProject(originalPosition)) {
                            return;
                        }

                        result.addAllElements(TranslationUtil.getTranslationLookupElements(originalPosition.getProject()));
                    }

                }

        );

    }

    private void attachTemplateFiles(CompletionParameters parameters, final CompletionResultSet result) {
        TemplateUtil.collectFiles(parameters.getPosition().getProject(), new TemplateUtil.SmartyTemplateVisitor() {
            @Override
            public void visitFile(VirtualFile virtualFile, String fileName) {
                result.addElement(LookupElementBuilder.create(fileName).withIcon(virtualFile.getFileType().getIcon()));
            }
        });
    }

}
