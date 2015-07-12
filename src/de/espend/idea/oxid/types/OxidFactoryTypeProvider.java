package de.espend.idea.oxid.types;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.FunctionReferenceImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider2;
import de.espend.idea.oxid.utils.ModuleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpTypeProviderUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class OxidFactoryTypeProvider implements PhpTypeProvider2 {


    final static char TRIM_KEY = '\u0180';

    @Override
    public char getKey() {
        return '\u0169';
    }

    @Nullable
    @Override
    public String getType(PsiElement e) {

        if (DumbService.getInstance(e.getProject()).isDumb()) {
            return null;
        }
        if(e instanceof FunctionReferenceImpl && "oxNew".equalsIgnoreCase(((FunctionReferenceImpl) e).getName())) {
            PsiElement[] parameters = ((FunctionReferenceImpl) e).getParameters();
            if(parameters.length > 0 && parameters[0] instanceof StringLiteralExpression) {
                String contents = ((StringLiteralExpression) parameters[0]).getContents();
                if(StringUtils.isNotBlank(contents)) {
                    return ((FunctionReferenceImpl) e).getSignature() + TRIM_KEY + contents;
                }
            }
        }

        // container calls are only on "get" methods
        if(e instanceof MethodReference && PhpElementsUtil.isMethodWithFirstStringOrFieldReference(e, "get")) {
            return PhpTypeProviderUtil.getReferenceSignature((MethodReference) e, TRIM_KEY);
        }

        return null;
    }

    @Override
    public Collection<? extends PhpNamedElement> getBySignature(String expression, Project project) {

        int endIndex = expression.lastIndexOf(TRIM_KEY);
        if(endIndex == -1) {
            return Collections.emptySet();
        }

        String originalSignature = expression.substring(0, endIndex);
        String parameter = expression.substring(endIndex + 1);

        // search for called method
        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Collection<? extends PhpNamedElement> phpNamedElementCollections = phpIndex.getBySignature(originalSignature, null, 0);
        if(phpNamedElementCollections.size() == 0) {
            return Collections.emptySet();
        }

        // get first matched item
        PhpNamedElement phpNamedElement = phpNamedElementCollections.iterator().next();
        if(!(phpNamedElement instanceof Function)) {
            return phpNamedElementCollections;
        }

        parameter = PhpTypeProviderUtil.getResolvedParameter(phpIndex, parameter);
        if(parameter == null) {
            return phpNamedElementCollections;
        }

        PhpClass phpClass = PhpElementsUtil.getClassInterface(project, parameter);
        if(phpClass != null) {


            Collection<PhpClass> phpClasses = new ArrayList<PhpClass>();
            phpClasses.add(phpClass);

            addExtendsClasses(project, parameter, phpClasses);

            return phpClasses;
        }

        return null;
    }

    /**
     *
     * We support "extends" on module metadata
     *
     * TODO: use index, this in a performance issue
     */
    private void addExtendsClasses(Project project, String parameter, Collection<PhpClass> phpClasses) {

        for (Map.Entry<String, Set<VirtualFile>> entry : ModuleUtil.getExtendsList(project).entrySet()) {

            // ignore cases, so we need an each
            if(!entry.getKey().equalsIgnoreCase(parameter)) {
                continue;
            }

            for (VirtualFile virtualFile : entry.getValue()) {

                PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
                if (!(file instanceof PhpFile)) {
                    continue;
                }

                Collection<PhpClass> allClasses = PhpPsiUtil.findAllClasses((PhpFile) file);
                if (allClasses.size() > 0) {
                    phpClasses.add(allClasses.iterator().next());
                }

            }
        }
    }
}
