package de.espend.idea.oxid.utils;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileBasedIndexImpl;
import com.jetbrains.smarty.SmartyFileType;
import de.espend.idea.oxid.OxidPluginIcons;
import de.espend.idea.oxid.stub.OxidContentIdentIndexer;
import fr.adrienbrault.idea.symfony2plugin.stubs.SymfonyProcessors;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TemplateUtil {

    public static void collectFiles(@NotNull Project project, @NotNull final SmartyTemplateVisitor visitor) {


        for(VirtualFile virtualFile : FileBasedIndex.getInstance().getContainingFiles(FileTypeIndex.NAME, SmartyFileType.INSTANCE, GlobalSearchScope.allScope(project))) {

            // try to get /templates/frontend/...
            String path = virtualFile.toString();
            int i = path.lastIndexOf("/application/views/");
            if(i > 0) {
                String frontendName = path.substring(i + "/application/views/".length());
                attachTemplates(virtualFile, frontendName, visitor);
            }
        }

        for (PsiFile psiFile : FilenameIndex.getFilesByName(project, "metadata.php", GlobalSearchScope.allScope(project))) {
            for (Map.Entry<String, String> entry : MetadataUtil.getMetadataKeyMap(psiFile, "templates").entrySet()) {

                VirtualFile parent = psiFile.getVirtualFile().getParent();
                if(parent != null) {
                    VirtualFile moduleFolder = parent.getParent();
                    if(moduleFolder != null) {
                        VirtualFile mainModule = moduleFolder.getParent();
                        if(mainModule != null) {
                            String[] split = entry.getValue().split("/");
                            VirtualFile relativeFile = VfsUtil.findRelativeFile(mainModule, split);
                            if(relativeFile != null) {
                                visitor.visitFile(relativeFile, entry.getKey());
                            }
                        }
                    }
                }

            }
        }

    }
    private static void attachTemplates(VirtualFile virtualFile, String frontendName, SmartyTemplateVisitor smartyTemplateVisitor) {

        String[] pathSplits = StringUtils.split(frontendName, "/");
        if(pathSplits.length <= 2 || pathSplits[0].equals("admin") || !pathSplits[1].equals("tpl") ) {
            return;
        }

        int n = pathSplits.length - 2;
        String[] newArray = new String[n];
        System.arraycopy(pathSplits, 2, newArray, 0, n);

        String fileName = StringUtils.join(newArray, "/");
        smartyTemplateVisitor.visitFile(virtualFile, fileName);
    }

    @NotNull
    public static Collection<SmartyBlockUtil.SmartyBlock> getBlocksTemplateName(@NotNull Project project, final @NotNull String templateName) {

        final Collection<SmartyBlockUtil.SmartyBlock> blocks = new ArrayList<SmartyBlockUtil.SmartyBlock>();

        for (VirtualFile virtualFile : getFilesByTemplateName(project, templateName)) {
            PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
            if(file != null) {
                blocks.addAll(SmartyBlockUtil.getFileBlocks(file));
            }
        }

        return blocks;
    }

    public static Collection<VirtualFile> getFilesByTemplateName(@NotNull Project project, final @NotNull String templateName) {

        final Collection<VirtualFile> files = new ArrayList<VirtualFile>();

        TemplateUtil.collectFiles(project, new TemplateUtil.SmartyTemplateVisitor() {
            @Override
            public void visitFile(VirtualFile virtualFile, String fileName) {
                if(fileName.equalsIgnoreCase(templateName)) {
                    files.add(virtualFile);
                }
            }
        });

        return files;
    }

    public interface SmartyTemplateVisitor {
        public void visitFile(VirtualFile virtualFile, String fileName);
    }

    @NotNull
    public static Set<String> getTemplateNames(@NotNull Project project, final @NotNull VirtualFile virtualFile) {

        final Set<String> templates = new HashSet<String>();

        TemplateUtil.collectFiles(project, new TemplateUtil.SmartyTemplateVisitor() {
            @Override
            public void visitFile(VirtualFile templateFile, String fileName) {
                if(templateFile.equals(virtualFile)) {
                    templates.add(fileName);
                }

            }
        });

        return templates;
    }

    public static Collection<LookupElement> getBlockFileLookupElements(@NotNull Project project, String... templateNames) {

        Set<String> blocks = new HashSet<String>();

        for (String template : new HashSet<String>(Arrays.asList(templateNames))) {
            for (SmartyBlockUtil.SmartyBlock smartyBlock : TemplateUtil.getBlocksTemplateName(project, template)) {
                if(!blocks.contains(smartyBlock.getName())) {
                    blocks.add(smartyBlock.getName());
                }
            }
        }

        Collection<LookupElement> lookupElements = new ArrayList<LookupElement>();
        for (String block : blocks) {
            lookupElements.add(LookupElementBuilder.create(block).withIcon(OxidPluginIcons.OXID).withTypeText("block", true));
        }

        return lookupElements;
    }

    public static Set<String> getContentIdents(@NotNull Project project) {
        SymfonyProcessors.CollectProjectUniqueKeys processor = new SymfonyProcessors.CollectProjectUniqueKeys(project, OxidContentIdentIndexer.KEY);
        FileBasedIndex.getInstance().processAllKeys(OxidContentIdentIndexer.KEY, processor, project);
        return processor.getResult();
    }

    public static Set<PsiElement> getContentIdentsTargets(final @NotNull Project project, final @NotNull VirtualFile currentFile, final @NotNull String ident) {

        final Set<PsiElement> psiElements = new HashSet<PsiElement>();

        FileBasedIndexImpl.getInstance().getFilesWithKey(OxidContentIdentIndexer.KEY, new HashSet<String>(Arrays.asList(ident)), new Processor<VirtualFile>() {
            @Override
            public boolean process(VirtualFile virtualFile) {

                if (currentFile.equals(virtualFile)) {
                    return true;
                }

                PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                if (psiFile == null) {
                    return true;
                }

                psiFile.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
                    @Override
                    public void visitElement(PsiElement element) {

                        if(SmartyPattern.getAttributeInsideTagPattern("ident", "oxcontent").accepts(element)) {
                            String content = element.getText();
                            if(StringUtils.isNotBlank(content) && content.equalsIgnoreCase(ident)) {
                                psiElements.add(element);
                            }
                        }

                        super.visitElement(element);
                    }

                });


                return true;
            }
        }, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), SmartyFileType.INSTANCE));

        return psiElements;
    }


}
