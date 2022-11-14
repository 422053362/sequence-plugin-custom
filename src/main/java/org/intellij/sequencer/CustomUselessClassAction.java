package org.intellij.sequencer;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.search.searches.DefinitionsScopedSearch;
import org.intellij.sequencer.config.SequenceParamsState;
import org.intellij.sequencer.generator.CallHierachyGenerator;
import org.intellij.sequencer.generator.filters.NoConstructorsFilter;
import org.intellij.sequencer.generator.filters.NoGetterSetterFilter;
import org.intellij.sequencer.generator.filters.NoPrivateMethodsFilter;
import org.intellij.sequencer.openapi.SequenceParams;
import org.intellij.sequencer.openapi.filters.ProjectOnlyFilter;
import org.intellij.sequencer.openapi.model.CallStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CustomUselessClassAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent event) {


        Project project = event.getProject();
        String basePath = project.getBasePath();
        List<PsiElement> psiClassList = new LinkedList<>();
        PsiElement psiElement = event.getData(CommonDataKeys.PSI_ELEMENT);
        {
            if (psiElement instanceof PsiClass) {
                psiClassList.add(psiElement);
            } else {
                psiClassList = getPsiElement(psiElement);
            }
        }
        //提取全部的方法
        List<PsiMethod> psiMethodList = new LinkedList<>();
        for (PsiElement element : psiClassList) {
            List<PsiElement> methodList = Arrays.stream(element.getChildren()).filter(psiElement1 -> psiElement1 instanceof PsiMethod).collect(Collectors.toList());
            for (PsiElement el : methodList) {
                psiMethodList.add((PsiMethod) el);
            }
        }
        //提取全部的属性
        List<PsiField> psiFieldList = new LinkedList<>();
        for (PsiElement element : psiClassList) {
            PsiClass psiClass = (PsiClass) element;
            List<PsiElement> methodList = Arrays.asList(psiClass.getAllFields());
            for (PsiElement el : methodList) {
                PsiField psiField = (PsiField) el;
                String text = psiField.getType().getCanonicalText();
                if (text.contains("com.qiekj.ads.dao.mapper") || text.contains("com.qiekj.bi.dao.mapper") || text.contains("com.qiekj.hologres.dao.mapper") || text.contains("com.qiekj.sell.dao.mapper")) {
                    psiFieldList.add(psiField);
                }
            }
        }

        //
        SequenceParams params = new SequenceParams();
        {
            SequenceParamsState state = SequenceParamsState.getInstance();
            params.setMaxDepth(state.callDepth);
            params.getMethodFilter().addFilter(new ProjectOnlyFilter(state.projectClassesOnly));
            params.getMethodFilter().addFilter(new NoGetterSetterFilter(false));
            params.getMethodFilter().addFilter(new NoPrivateMethodsFilter(false));
            params.getMethodFilter().addFilter(new NoConstructorsFilter(false));
            params.setAllowRecursion(true);
        }
        Map<PsiMethod, CallStack> psiMethodCallStackMap = new HashMap<>();
        int index = 0;
        for (PsiMethod psiMethod : psiMethodList) {
            System.out.println(index + "%" + psiMethodList.size());
            index++;
            CallHierachyGenerator generator = new CallHierachyGenerator(params);
            CallStack callStack = generator.generate(psiMethod, null);
            String line = callStack.toString();
            if (line.contains("com.qiekj.ads.dao.mapper") || line.contains("com.qiekj.bi.dao.mapper") || line.contains("com.qiekj.hologres.dao.mapper") || line.contains("com.qiekj.sell.dao.mapper")) {
                psiMethodCallStackMap.put(psiMethod, callStack);
            }
        }
        Map<String, PsiMethod> tempMap = new HashMap<>();
        for (Map.Entry<PsiMethod, CallStack> entry : psiMethodCallStackMap.entrySet()) {
            PsiMethod psiMethod = entry.getKey();
            PsiClass containingClass = psiMethod.getContainingClass();
            Boolean isInterface = containingClass.isInterface();
            if (isInterface) {
                PsiElement[] classTypeArray = DefinitionsScopedSearch.search(containingClass).toArray(PsiElement.EMPTY_ARRAY);
                for (PsiElement psiClassType : classTypeArray) {
                    PsiClass psiClass = (PsiClass) psiClassType;
                    PsiMethod[] subMethodArray = psiClass.findMethodsBySignature(psiMethod, false);
                    for (PsiMethod mm : subMethodArray) {
                        tempMap.put(mm.getContainingClass().getName() + "#" + mm.toString(), mm);
                    }
                }
            }
        }
        index = 0;
        for (Map.Entry<PsiMethod, CallStack> entry : psiMethodCallStackMap.entrySet()) {
            PsiMethod psiMethod = entry.getKey();
            int finalIndex = index++;
            WriteCommandAction.runWriteCommandAction(project, new Runnable() {
                @Override
                public void run() {
                    psiMethod.delete();
                    //tempMap.remove(psiMethod.getContainingClass().getName() + "#" + psiMethod.toString());
                    System.out.println(finalIndex + "#" + psiMethod);
                }
            });
            System.out.println(index + "#" + psiMethod);
        }
        for (Map.Entry<String, PsiMethod> entry : tempMap.entrySet()) {
            PsiMethod psiMethod = entry.getValue();
            int finalIndex = index++;
            WriteCommandAction.runWriteCommandAction(project, new Runnable() {
                @Override
                public void run() {
                    psiMethod.delete();
                    System.out.println(finalIndex + "#" + psiMethod);
                }
            });
            System.out.println(index + "#" + psiMethod);
        }
        index = 0;
        for (PsiField psiField : psiFieldList) {
            int finalIndex = index++;
            WriteCommandAction.runWriteCommandAction(project, new Runnable() {
                @Override
                public void run() {
                    psiField.delete();
                    System.out.println(finalIndex + "#" + psiField);
                }
            });
        }
        System.out.println(1);
    }


    List<PsiElement> getPsiElement(PsiElement psiDirectory) {
        List<PsiElement> retList = new ArrayList<>();
        PsiElement @NotNull [] children = psiDirectory.getChildren();
        for (PsiElement element : children) {
            if (element instanceof PsiClass) {
                retList.add(element);
            } else {
                List<PsiElement> tempList = getPsiElement(element);
                retList.addAll(tempList);
            }
        }
        return retList;
    }
}
