package de.espend.idea.oxid.utils;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.jetbrains.smarty.lang.SmartyTokenTypes;
import com.jetbrains.smarty.lang.psi.SmartyCompositeElementTypes;
import com.jetbrains.smarty.lang.psi.SmartyTag;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SmartyPattern {

    public static PsiElementPattern.Capture<PsiElement> getFilePattern() {
        // getName dont work
        return PlatformPatterns.psiElement(SmartyTokenTypes.STRING_LITERAL).withParent(
                PlatformPatterns.psiElement(SmartyCompositeElementTypes.ATTRIBUTE_VALUE).withParent(
                        PlatformPatterns.psiElement(SmartyCompositeElementTypes.ATTRIBUTE).withText(PlatformPatterns.string().contains("file="))
                )
        );
    }

    public static PsiElementPattern.Capture<PsiElement> getBlockPattern() {
        return PlatformPatterns.psiElement(SmartyTokenTypes.STRING_LITERAL).withParent(
                PlatformPatterns.psiElement(SmartyCompositeElementTypes.ATTRIBUTE_VALUE).withParent(
                        PlatformPatterns.psiElement(SmartyCompositeElementTypes.ATTRIBUTE).withText(PlatformPatterns.string().contains("name=")).withParent(
                                PlatformPatterns.psiElement(SmartyCompositeElementTypes.TAG).withText(PlatformPatterns.string().startsWith("{block"))
                        )
                )
        );
    }

    /**
     * Not all tags are known, we need some custom syntax check
     *
     * {oxmultilang ident="FOO"}
     * { oxmultilang ident="FOO"}
     */
    public static PsiElementPattern.Capture<PsiElement> getAttributeInsideTagPattern(@NotNull String attribute, @NotNull String tag) {
        return PlatformPatterns.psiElement(SmartyTokenTypes.STRING_LITERAL).afterLeafSkipping(
                PlatformPatterns.or(
                        PlatformPatterns.psiElement(SmartyTokenTypes.DOUBLE_QUOTE),
                        PlatformPatterns.psiElement(SmartyTokenTypes.SINGLE_QUOTE),
                        PlatformPatterns.psiElement(SmartyTokenTypes.EQ),
                        PlatformPatterns.psiElement(SmartyTokenTypes.WHITE_SPACE),
                        PlatformPatterns.psiElement(PsiWhiteSpace.class)
                ),
                PlatformPatterns.psiElement(SmartyTokenTypes.IDENTIFIER).withText(attribute)
        ).withParent(
                PlatformPatterns.psiElement(SmartyTag.class).withText(
                        PlatformPatterns.string().matches("\\{\\s*" + tag + ".*")
                )
        );
    }

}
