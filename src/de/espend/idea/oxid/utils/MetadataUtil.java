package de.espend.idea.oxid.utils;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.GroupStatement;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import com.jetbrains.php.lang.psi.elements.Statement;
import com.jetbrains.php.lang.psi.elements.impl.AssignmentExpressionImpl;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class MetadataUtil {

    public static Map<String, String> getMetadataKeyMap(@NotNull PsiFile psiFile, @NotNull String key) {

        final Map<String, String> values = new HashMap<String, String>();

        visitMetadataKey(psiFile, key, new MetadataKeyVisitor() {
            @Override
            public void visit(@NotNull ArrayCreationExpression arrayCreationExpression) {
                values.putAll(PhpElementsUtil.getArrayKeyValueMap(arrayCreationExpression));
            }
        });

        return values;
    }

    private static void visitMetadataKey(@NotNull PsiFile psiFile, @NotNull String key, @NotNull MetadataKeyVisitor visitor) {

        PsiElement childOfType = PsiTreeUtil.getChildOfType(psiFile, GroupStatement.class);
        if(childOfType == null) {
            return;
        }

        for (Statement statement : PsiTreeUtil.getChildrenOfType(childOfType, Statement.class)) {
            PsiElement assignmentExpr = statement.getFirstPsiChild();
            if(assignmentExpr instanceof AssignmentExpressionImpl) {
                PhpPsiElement variable = ((AssignmentExpressionImpl) assignmentExpr).getVariable();
                if(variable != null && "aModule".equals(variable.getName())) {

                    PhpPsiElement value = ((AssignmentExpressionImpl) assignmentExpr).getValue();
                    if(value instanceof ArrayCreationExpression) {
                        PhpPsiElement arrayCreationKeyMap = PhpElementsUtil.getArrayValue((ArrayCreationExpression) value, key);
                        if(arrayCreationKeyMap instanceof ArrayCreationExpression) {
                            visitor.visit((ArrayCreationExpression) arrayCreationKeyMap);
                        }
                    }
                }
            }
        }
    }

    public static interface MetadataKeyVisitor {
        public void visit(@NotNull ArrayCreationExpression arrayCreationExpression);
    }

}
