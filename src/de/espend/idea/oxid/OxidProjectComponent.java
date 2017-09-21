package de.espend.idea.oxid;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import de.espend.idea.oxid.utils.OxidUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

        DumbService.getInstance(this.project).smartInvokeLater(() -> {
            if(PhpElementsUtil.getClassInterface(project, "\\oxArticle") == null) {
                return;
            }

            new Task.Backgroundable(project, "OXID extends dumper", true) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    if(DumbService.getInstance(project).isDumb()) {
                        return;
                    }

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
        if(psiElement == null) {
            return false;
        }

        // @TODO: plugin switch
        // VfsUtil.findRelativeFile(psiElement.getProject().getBaseDir(), "engine", "Shopware", "Kernel.php") != null
        // PhpElementsUtil.getClassInterface(psiElement.getProject(), "\\Enlight_Controller_Action") != null

        return true;
    }

}
