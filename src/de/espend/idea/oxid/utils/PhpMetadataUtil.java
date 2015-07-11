package de.espend.idea.oxid.utils;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.ArrayHashElement;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpMetadataUtil {

    public static boolean isModuleKeyInFlatArray(@NotNull StringLiteralExpression psiElement, @NotNull String key) {
        PsiElement arrayKey = psiElement.getParent();
        if(arrayKey != null && arrayKey.getNode().getElementType() == PhpElementTypes.ARRAY_VALUE) {
            PsiElement hashElement = arrayKey.getParent();
            if(hashElement instanceof ArrayHashElement) {
                PsiElement arrayCreation = hashElement.getParent();
                if(arrayCreation instanceof ArrayCreationExpression) {
                    PsiElement arrayValue = arrayCreation.getParent();
                    if(arrayValue != null && arrayValue.getNode().getElementType() == PhpElementTypes.ARRAY_VALUE) {
                        PsiElement hashArray = arrayValue.getParent();
                        if(hashArray instanceof ArrayHashElement) {
                            PhpPsiElement keyString = ((ArrayHashElement) hashArray).getKey();
                            if(keyString instanceof StringLiteralExpression) {
                                String contents = ((StringLiteralExpression) keyString).getContents();
                                if(key.equals(contents)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Block keys, a really depth tree structure
     */
    public static boolean isInTemplateWithKey(@NotNull StringLiteralExpression psiElement, @NotNull String key) {
        PsiElement arrayValue = psiElement.getParent();
        if(arrayValue != null && arrayValue.getNode().getElementType() == PhpElementTypes.ARRAY_VALUE) {
            PsiElement hashElement = arrayValue.getParent();
            if(hashElement instanceof ArrayHashElement) {
                PhpPsiElement keyString = ((ArrayHashElement) hashElement).getKey();
                if(keyString instanceof StringLiteralExpression) {
                    String contents = ((StringLiteralExpression) keyString).getContents();
                    if(key.equals(contents)) {

                        PsiElement templateArrayValue = hashElement.getParent();
                        if(templateArrayValue instanceof ArrayCreationExpression) {
                            PsiElement blockArrayValue = templateArrayValue.getParent();
                            if(blockArrayValue.getNode().getElementType() == PhpElementTypes.ARRAY_VALUE) {

                                PsiElement arrayCrea = blockArrayValue.getParent();
                                if(arrayCrea instanceof ArrayCreationExpression) {

                                    PsiElement arrayValueBlock = arrayCrea.getParent();
                                    if(arrayValueBlock != null && arrayValueBlock.getNode().getElementType() == PhpElementTypes.ARRAY_VALUE) {
                                        PsiElement blockHash = arrayValueBlock.getParent();
                                        if(blockHash instanceof ArrayHashElement) {
                                            PhpPsiElement keyBlock = ((ArrayHashElement) blockHash).getKey();
                                            if(keyBlock instanceof StringLiteralExpression) {
                                                String blockContents = ((StringLiteralExpression) keyBlock).getContents();
                                                if("blocks".equals(blockContents)) {
                                                    return true;
                                                }
                                            }
                                        }
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

}
