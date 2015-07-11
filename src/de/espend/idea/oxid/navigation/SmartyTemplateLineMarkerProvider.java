package de.espend.idea.oxid.navigation;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.ConstantFunction;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.smarty.SmartyFile;
import de.espend.idea.oxid.OxidPluginIcons;
import de.espend.idea.oxid.OxidProjectComponent;
import de.espend.idea.oxid.utils.SmartyBlockUtil;
import de.espend.idea.oxid.utils.SmartyPattern;
import de.espend.idea.oxid.utils.TemplateUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.RelatedPopupGotoLineMarker;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SmartyTemplateLineMarkerProvider implements LineMarkerProvider {

    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement psiElement) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> psiElements, @NotNull Collection<LineMarkerInfo> collection) {

        if(psiElements.size() == 0 || !OxidProjectComponent.isValidForProject(psiElements.get(0))) {
            return;
        }

        for(PsiElement psiElement: psiElements) {

            if (psiElement instanceof SmartyFile) {
                attachFileContextMaker((SmartyFile) psiElement, collection);
            }

            if(SmartyPattern.getBlockPattern().accepts(psiElement)) {
                attachBlocks(psiElement, collection);
            }

        }
    }

    private void attachBlocks(PsiElement psiElement, Collection<LineMarkerInfo> lineMarkerInfos) {

        final String contents = psiElement.getText();
        if(StringUtils.isBlank(contents)) {
            return;
        }

        VirtualFile virtualFile = psiElement.getContainingFile().getVirtualFile();

        Collection<PsiElement> psiElements = new ArrayList<PsiElement>();

        for (String templateName: TemplateUtil.getTemplateNames(psiElement.getProject(), psiElement.getContainingFile().getVirtualFile())) {
            for (SmartyBlockUtil.SmartyBlock block : TemplateUtil.getBlocksTemplateName(psiElement.getProject(), templateName)) {
                if(block.getName().equals(contents)) {
                    if(!virtualFile.equals(block.getElement().getContainingFile().getVirtualFile())) {
                        psiElements.add(block.getElement());
                    }
                }
            }
        }

        if(psiElements.size() == 0) {
            return;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(PhpIcons.OVERRIDES).
                setTargets(psiElements).
                setTooltipText("Navigate to block");

        lineMarkerInfos.add(builder.createLineMarkerInfo(psiElement));
    }

    private void attachFileContextMaker(SmartyFile smartyFile, @NotNull Collection<LineMarkerInfo> lineMarkerInfo) {

        final VirtualFile virtualFile = smartyFile.getVirtualFile();

        final Set<String> templates = TemplateUtil.getTemplateNames(smartyFile.getProject(), virtualFile);
        if(templates.size() == 0) {
            return;
        }

        List<GotoRelatedItem> gotoRelatedItems = new ArrayList<GotoRelatedItem>();

        for (String template : templates) {
            for (VirtualFile file : TemplateUtil.getFilesByTemplateName(smartyFile.getProject(), template)) {

                if(file.equals(virtualFile)) {
                   continue;
                }

                PsiFile psiFile = PsiManager.getInstance(smartyFile.getProject()).findFile(file);
                if(psiFile != null) {
                    String templateName = getPresentableTemplateName(smartyFile, file);
                    gotoRelatedItems.add(new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(psiFile, templateName).withIcon(file.getFileType().getIcon(), OxidPluginIcons.OXID_LINEMARKER));
                }
            }
        }

        if(gotoRelatedItems.size() == 0) {
            return;
        }

        // only one item dont need popover
        if(gotoRelatedItems.size() == 1) {
            lineMarkerInfo.add(getSingleLineMarker(smartyFile, lineMarkerInfo, gotoRelatedItems.get(0)));
            return;
        }

        if(gotoRelatedItems.size() == 0) {
            return;
        }

        lineMarkerInfo.add(getRelatedPopover("Overwrite", "Overwrites", smartyFile, gotoRelatedItems));
    }

    private LineMarkerInfo getRelatedPopover(String singleItemTitle, String singleItemTooltipPrefix, PsiElement lineMarkerTarget, List<GotoRelatedItem> gotoRelatedItems) {

        // single item has no popup
        String title = singleItemTitle;
        if(gotoRelatedItems.size() == 1) {
            String customName = gotoRelatedItems.get(0).getCustomName();
            if(customName != null) {
                title = String.format(singleItemTooltipPrefix, customName);
            }
        }

        return new LineMarkerInfo<PsiElement>(lineMarkerTarget, lineMarkerTarget.getTextOffset(), OxidPluginIcons.OXID_LINEMARKER, 6, new ConstantFunction<PsiElement, String>(title), new fr.adrienbrault.idea.symfony2plugin.dic.RelatedPopupGotoLineMarker.NavigationHandler(gotoRelatedItems));
    }

    public static RelatedItemLineMarkerInfo<PsiElement> getSingleLineMarker(SmartyFile smartyFile, Collection<LineMarkerInfo> lineMarkerInfos, GotoRelatedItem gotoRelatedItem) {

        // hell: find any possible small icon
        Icon icon = null;
        if(gotoRelatedItem instanceof RelatedPopupGotoLineMarker.PopupGotoRelatedItem) {
            icon = ((RelatedPopupGotoLineMarker.PopupGotoRelatedItem) gotoRelatedItem).getSmallIcon();
        }

        if(icon == null) {
            icon = OxidPluginIcons.OXID_LINEMARKER;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(icon).
                setTargets(gotoRelatedItem.getElement());

        String customName = gotoRelatedItem.getCustomName();
        if(customName != null) {
            builder.setTooltipText(customName);
        }

        return builder.createLineMarkerInfo(smartyFile);
    }

    private String getPresentableTemplateName(SmartyFile smartyFile, VirtualFile file) {
        String templateName = file.getPath();

        String relativePath = VfsUtil.getRelativePath(file, smartyFile.getProject().getBaseDir());
        if(relativePath != null) {
            templateName = relativePath;
        }

        int i = templateName.indexOf("/views/");
        if(i > 0) {
            templateName = templateName.substring(i + "/views/".length());
        }

        if(templateName.length() > 50) {
            templateName = "..." + templateName.substring(templateName.length() - 50);
        }
        return templateName;
    }

}
