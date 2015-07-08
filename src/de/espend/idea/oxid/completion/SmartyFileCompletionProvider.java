package de.espend.idea.oxid.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ProcessingContext;
import de.espend.idea.oxid.OxidProjectComponent;
import de.espend.idea.oxid.utils.TemplateUtil;
import org.jetbrains.annotations.NotNull;
import de.espend.idea.oxid.utils.SmartyPattern;

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
