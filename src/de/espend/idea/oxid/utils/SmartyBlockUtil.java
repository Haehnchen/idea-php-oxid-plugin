package de.espend.idea.oxid.utils;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SmartyBlockUtil {

    public static Collection<SmartyBlock> getFileBlocks(@NotNull PsiFile psiFile) {

        final Collection<SmartyBlock> blockNameSet = new ArrayList<SmartyBlock>();

        PsiTreeUtil.processElements(psiFile, new PsiElementProcessor() {
            @Override
            public boolean execute(@NotNull PsiElement element) {

                if (SmartyPattern.getBlockPattern().accepts(element)) {
                    blockNameSet.add(new SmartyBlock(element, element.getText()));
                }

                return true;
            }
        });

        return blockNameSet;
    }

    public static class SmartyBlock {

        final private PsiElement element;
        final private String name;

        public SmartyBlock(PsiElement element, String name) {
            this.element = element;
            this.name = name;
        }

        public PsiElement getElement() {
            return element;
        }

        public String getName() {
            return name;
        }

    }
}
