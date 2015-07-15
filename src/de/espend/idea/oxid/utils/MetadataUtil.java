package de.espend.idea.oxid.utils;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.GroupStatement;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import com.jetbrains.php.lang.psi.elements.Statement;
import com.jetbrains.php.lang.psi.elements.impl.AssignmentExpressionImpl;
import de.espend.idea.oxid.dict.metadata.MetadataBlock;
import de.espend.idea.oxid.dict.metadata.MetadataSetting;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class MetadataUtil {

    public static Collection<MetadataSetting> getSettings(@NotNull PsiFile psiFile) {

        final Collection<MetadataSetting> blocks = new ArrayList<MetadataSetting>();

        visitMetadataKey(psiFile, "settings", new MetadataKeyVisitor() {
            @Override
            public void visit(@NotNull ArrayCreationExpression arrayCreationExpression) {

                for (PhpPsiElement phpPsiElement : PsiTreeUtil.getChildrenOfTypeAsList(arrayCreationExpression, PhpPsiElement.class)) {

                    if(phpPsiElement.getNode().getElementType() != PhpElementTypes.ARRAY_VALUE) {
                        continue;
                    }

                    PhpPsiElement firstPsiChild = phpPsiElement.getFirstPsiChild();
                    if(!(firstPsiChild instanceof ArrayCreationExpression)) {
                        continue;
                    }

                    MetadataSetting block = MetadataSetting.create(firstPsiChild, getArrayKeyValueMapProxy((ArrayCreationExpression) firstPsiChild));
                    if(block != null) {
                        blocks.add(block);
                    }

                }

            }
        });

        return blocks;
    }

    public static Collection<MetadataBlock> getBlocks(@NotNull PsiFile psiFile) {

        final Collection<MetadataBlock> blocks = new ArrayList<MetadataBlock>();

        visitMetadataKey(psiFile, "blocks", new MetadataKeyVisitor() {
            @Override
            public void visit(@NotNull ArrayCreationExpression arrayCreationExpression) {

                for (PhpPsiElement phpPsiElement : PsiTreeUtil.getChildrenOfTypeAsList(arrayCreationExpression, PhpPsiElement.class)) {

                    if(phpPsiElement.getNode().getElementType() != PhpElementTypes.ARRAY_VALUE) {
                        continue;
                    }

                    PhpPsiElement firstPsiChild = phpPsiElement.getFirstPsiChild();
                    if(!(firstPsiChild instanceof ArrayCreationExpression)) {
                        continue;
                    }

                    MetadataBlock block = MetadataBlock.create(getArrayKeyValueMapProxy((ArrayCreationExpression) firstPsiChild));
                    if(block != null) {
                        blocks.add(block);
                    }

                }

            }
        });

        return blocks;
    }

    public static Map<String, String> getMetadataKeyMap(@NotNull PsiFile psiFile, @NotNull String key) {

        getBlocks(psiFile);

        final Map<String, String> values = new HashMap<String, String>();

        visitMetadataKey(psiFile, key, new MetadataKeyVisitor() {
            @Override
            public void visit(@NotNull ArrayCreationExpression arrayCreationExpression) {
                values.putAll(getArrayKeyValueMapProxy(arrayCreationExpression));
            }
        });

        return values;
    }


    /**
     * GetArrayKeyValueMap adds null Value; proxy until fixed external
     */
    @NotNull
    private static Map<String, String> getArrayKeyValueMapProxy(@NotNull ArrayCreationExpression arrayCreationExpression) {

        Map<String, String> map = new HashMap<String, String>();

        for (Map.Entry<String, String> entry : PhpElementsUtil.getArrayKeyValueMap(arrayCreationExpression).entrySet()) {
            String value = entry.getValue();
            String key = entry.getKey();
            if(key != null && StringUtils.isNotBlank(key) && value != null && StringUtils.isNotBlank(value)) {
                map.put(entry.getKey(), entry.getValue());
            }
        }

        return map;
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

    public static void visitTranslationKey(@NotNull PsiFile psiFile, @NotNull TranslationKeyVisitor visitor) {

        PsiElement childOfType = PsiTreeUtil.getChildOfType(psiFile, GroupStatement.class);
        if(childOfType == null) {
            return;
        }

        Statement[] childrenOfType = PsiTreeUtil.getChildrenOfType(childOfType, Statement.class);
        if(childrenOfType == null) {
            return;
        }

        for (Statement statement : childrenOfType) {
            PsiElement assignmentExpr = statement.getFirstPsiChild();
            if(assignmentExpr instanceof AssignmentExpressionImpl) {
                PhpPsiElement variable = ((AssignmentExpressionImpl) assignmentExpr).getVariable();
                if(variable != null && "aLang".equals(variable.getName())) {

                    PhpPsiElement value = ((AssignmentExpressionImpl) assignmentExpr).getValue();
                    if(value instanceof ArrayCreationExpression) {
                        for (Map.Entry<String, PsiElement> entry : PhpElementsUtil.getArrayCreationKeyMap((ArrayCreationExpression) value).entrySet()) {
                            visitor.visit(entry.getKey(), entry.getValue());
                        }
                    }
                }
            }
        }
    }

    public interface TranslationKeyVisitor {
        void visit(@NotNull String name, @NotNull PsiElement value);
    }

    public interface MetadataKeyVisitor {
        void visit(@NotNull ArrayCreationExpression arrayCreationExpression);
    }

    /**
     * Find modules folder on "modules" or vendor structure
     *
     * @param psiFile metadata file
     */
    @Nullable
    public static VirtualFile getModuleVendorFolderFromMetadata(@NotNull PsiFile psiFile) {

        VirtualFile file = psiFile.getVirtualFile();

        // save previous we
        VirtualFile current = file;

        for (VirtualFile parent = file.getParent(); parent != null; parent = parent.getParent()) {

            if(parent.getName().equals("modules")) {
                return current;
            }

            if(parent.isDirectory() && parent.findChild("vendormetadata.php") != null) {
                return parent;
            }

            // we need to return previous PsiElement
            current = parent;
        }

        return null;
    }

    @Nullable
    public static VirtualFile getModuleDirectoryOnMetadata(@NotNull VirtualFile metadataFile) {

        VirtualFile file = metadataFile;
        for ( int i = 0; i <= 2; i++) {
            file = file.getParent();
            if(file == null) {
                return null;
            }
        }

        VirtualFile parent = metadataFile.getParent();
        if(parent == null) {
            return null;
        }

        return file;
    }

}
