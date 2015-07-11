package de.espend.idea.oxid.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.smarty.SmartyFileType;
import de.espend.idea.oxid.navigation.PhpGoToHandler;
import de.espend.idea.oxid.utils.PhpMetadataUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class OxidMetadataFileInspection extends LocalInspectionTool {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {

        PsiFile psiFile = holder.getFile();

        String name = psiFile.getName();
        if(!name.toLowerCase().contains("metadata.php")) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        psiFile.acceptChildren(new MyPsiRecursiveElementWalkingVisitor(holder));
        return super.buildVisitor(holder, isOnTheFly);
    }

    private static class MyPsiRecursiveElementWalkingVisitor extends PsiRecursiveElementWalkingVisitor {
        private final ProblemsHolder holder;

        public MyPsiRecursiveElementWalkingVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitElement(PsiElement element) {

            if(element instanceof StringLiteralExpression) {
                visitLiteralExpression((StringLiteralExpression) element);
            }

            super.visitElement(element);
        }

        public void visitLiteralExpression(StringLiteralExpression element) {

            // @TODO: refactor in providers
            if(PhpMetadataUtil.isModuleKeyInFlatArray(element, "extend")) {
                Collection<PsiElement> psiElements = new ArrayList<PsiElement>();
                PhpGoToHandler.attachMetadataExtends(element, psiElements);
                if(psiElements.size() == 0) {
                    holder.registerProblem(element, "File not found", ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                }
            } else if(PhpMetadataUtil.isModuleKeyInFlatArray(element, "files")) {
                Collection<PsiElement> psiElements = new ArrayList<PsiElement>();
                PhpGoToHandler.attachAllFileTypes(element, psiElements, PhpFileType.INSTANCE);
                if(psiElements.size() == 0) {
                    holder.registerProblem(element, "File not found", ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                }
            } else if(PhpMetadataUtil.isModuleKeyInFlatArray(element, "templates")) {
                Collection<PsiElement> psiElements = new ArrayList<PsiElement>();
                PhpGoToHandler.attachAllFileTypes(element, psiElements, SmartyFileType.INSTANCE);
                if(psiElements.size() == 0) {
                    holder.registerProblem(element, "File not found", ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                }
            } else if(PhpMetadataUtil.isInTemplateWithKey(element, "file")) {
                Collection<PsiElement> psiElements = new ArrayList<PsiElement>();
                PhpGoToHandler.attachTemplateFileTypes(element, psiElements);
                if(psiElements.size() == 0) {
                    holder.registerProblem(element, "File not found", ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                }
            }

        }

    }
}
