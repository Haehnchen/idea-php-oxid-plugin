package de.espend.idea.oxid;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.jetbrains.php.lang.PhpFileType;
import de.espend.idea.oxid.utils.OxidUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class OxidProjectComponent implements ProjectComponent {

    public static final int DUMPER_PERIODE = 600 * 1000;

    private final Project project;
    private Timer timer;

    public OxidProjectComponent(Project project) {
        this.project = project;
    }

    @Override
    public void projectOpened() {

        DumbService.getInstance(this.project).smartInvokeLater(new Runnable() {
            @Override
            public void run() {

                if(PhpElementsUtil.getClassInterface(project, "\\oxArticle") == null) {
                    return;
                }

                new Task.Backgroundable(project, "OXID extends dumper", true) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        OxidUtil.buildClassMetadataFile(project);
                    }
                }.queue();


                timer = new Timer();

                timer.schedule(new TimerTask() {
                    public void run() {

                        if(DumbService.getInstance(project).isDumb()) {
                            return;
                        }

                        OxidUtil.buildClassMetadataFile(project);
                    }
                }, DUMPER_PERIODE, DUMPER_PERIODE);

            }
        });


    }

    @Override
    public void projectClosed() {
        if(this.timer != null) {
            this.timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    @Override
    public void initComponent() {

    }

    @Override
    public void disposeComponent() {

    }

    @NotNull
    @Override
    public String getComponentName() {
        return "OXID Plugin";
    }

    public static boolean isValidForProject(@Nullable PsiElement psiElement) {
        if(psiElement == null) return false;

        /*
        if(VfsUtil.findRelativeFile(psiElement.getProject().getBaseDir(), "engine", "Shopware", "Kernel.php") != null) {
            return true;
        }

        if(PhpElementsUtil.getClassInterface(psiElement.getProject(), "\\Enlight_Controller_Action") != null) {
            return true;
        }
        */

        return true;
    }

}
