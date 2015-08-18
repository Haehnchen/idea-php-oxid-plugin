package de.espend.idea.oxid.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.jetbrains.smarty.SmartyFileType;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ModuleUtil {

    private static final Key<CachedValue<Map<String, Set<VirtualFile>>>> EXTEND_LIST_CACHE = new Key<CachedValue<Map<String, Set<VirtualFile>>>>("OXID_EXTEND_LIST_CACHE");

    public static void visitModuleTemplatesInMetadataScope(@NotNull PsiFile metaFile, final @NotNull ModuleFileVisitor visitor) {

        ModuleUtil.visitModuleFile(metaFile, new ModuleUtil.ModuleFileVisitor() {
            @Override
            public void visit(@NotNull VirtualFile virtualFile, @NotNull String relativePath) {

                if (virtualFile.getFileType() != SmartyFileType.INSTANCE) {
                    return;
                }

                String[] split = relativePath.split("/");
                if(split.length <= 2) {
                    return;
                }

                int n = split.length - 2;
                String[] newArray = new String[n];
                System.arraycopy(split, 2, newArray, 0, n);

                visitor.visit(virtualFile, StringUtils.join(newArray, "/"));
            }
        });
    }

    public static void visitModuleFile(@NotNull PsiFile metaFile, final @NotNull ModuleFileVisitor visitor) {

        final VirtualFile moduleFolder = MetadataUtil.getModuleVendorFolderFromMetadata(metaFile.getVirtualFile());
        if(moduleFolder == null) {
            return;
        }

        PsiDirectory parent = metaFile.getParent();
        if(parent == null) {
            return;
        }

        VfsUtil.visitChildrenRecursively(parent.getVirtualFile(), new VirtualFileVisitor() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {

                if (file.isDirectory()) {
                    return super.visitFile(file);
                }

                String relativePath = VfsUtil.getRelativePath(file, moduleFolder.getParent(), '/');
                if(relativePath == null) {
                    return super.visitFile(file);
                }

                visitor.visit(file, relativePath);

                return super.visitFile(file);
            }
        });
    }

    public interface ModuleFileVisitor {
        void visit(@NotNull VirtualFile virtualFile, @NotNull String relativePath);
    }

    @NotNull
    synchronized public static Map<String, Set<VirtualFile>> getExtendsList(@NotNull final Project project) {

        CachedValue<Map<String, Set<VirtualFile>>> cache = project.getUserData(EXTEND_LIST_CACHE);

        if (cache == null) {
            cache = CachedValuesManager.getManager(project).createCachedValue(new CachedValueProvider<Map<String, Set<VirtualFile>>>() {
                @Nullable
                @Override
                public Result<Map<String, Set<VirtualFile>>> compute() {

                    Map<String, Set<VirtualFile>> extendsList = new HashMap<String, Set<VirtualFile>>();

                    for (PsiFile psiFile : FilenameIndex.getFilesByName(project, "metadata.php", GlobalSearchScope.allScope(project))) {

                        VirtualFile vendorDir = MetadataUtil.getModuleDirectoryOnMetadata(psiFile.getVirtualFile());
                        if(vendorDir == null) {
                            continue;
                        }

                        for (Map.Entry<String, String> entry : MetadataUtil.getMetadataKeyMap(psiFile, "extend").entrySet()) {
                            if(!extendsList.containsKey(entry.getKey())) {
                                extendsList.put(entry.getKey(), new HashSet<VirtualFile>());
                            }

                            String replace = entry.getValue().replace("\\", "/");
                            if(replace.startsWith("/")) {
                                replace = replace.substring(1);
                            }

                            if(!replace.toLowerCase().endsWith(".php")) {
                                replace = replace + ".php";
                            }

                            VirtualFile relativeFile = VfsUtil.findRelativeFile(vendorDir, replace.split("/"));
                            if(relativeFile != null) {
                                extendsList.get(entry.getKey()).add(relativeFile);
                            }
                        }
                    }

                    return Result.create(extendsList, PsiModificationTracker.MODIFICATION_COUNT);
                }
            }, false);
            project.putUserData(EXTEND_LIST_CACHE, cache);
        }

        return cache.getValue();
    }

}
