package de.espend.idea.oxid.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.GroupStatement;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import com.jetbrains.php.lang.psi.elements.Statement;
import com.jetbrains.php.lang.psi.elements.impl.AssignmentExpressionImpl;
import com.jetbrains.smarty.SmartyFileType;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

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

    public interface SmartyTemplateVisitor {
        public void visitFile(VirtualFile virtualFile, String fileName);
    }

}
