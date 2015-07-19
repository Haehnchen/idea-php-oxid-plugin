package de.espend.idea.oxid.stub;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.jetbrains.smarty.SmartyFileType;
import de.espend.idea.oxid.utils.SmartyPattern;
import gnu.trove.THashMap;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class OxidContentIdentIndexer extends FileBasedIndexExtension<String, Void> {

    public static final ID<String, Void> KEY = ID.create("de.espend.idea.oxid.oxid_content_ident");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();

    @NotNull
    @Override
    public ID<String, Void> getName() {
        return KEY;
    }

    @NotNull
    @Override
    public DataIndexer<String, Void, FileContent> getIndexer() {

        return new DataIndexer<String, Void, FileContent>() {
            @NotNull
            @Override
            public Map<String, Void> map(@NotNull FileContent inputData) {

                PsiFile psiFile = inputData.getPsiFile();
                final Map<String, Void> map = new THashMap<String, Void>();

                psiFile.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
                    @Override
                    public void visitElement(PsiElement element) {

                        if(SmartyPattern.getAttributeInsideTagPattern("ident", "oxcontent").accepts(element)) {
                            String content = element.getText();
                            if(StringUtils.isNotBlank(content)) {
                                map.put(content, null);
                            }
                        }

                        super.visitElement(element);
                    }

                });

                return map;
            }
        };

    }

    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @NotNull
    @Override
    public DataExternalizer<Void> getValueExternalizer() {
        return ScalarIndexExtension.VOID_DATA_EXTERNALIZER;
    }

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return new FileBasedIndex.InputFilter() {
            @Override
            public boolean acceptInput(@NotNull VirtualFile file) {
                return file.getFileType() == SmartyFileType.INSTANCE;
            }
        };
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 1;
    }
}
