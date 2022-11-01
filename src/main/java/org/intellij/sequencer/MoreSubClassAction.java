package org.intellij.sequencer;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.apache.commons.collections.CollectionUtils;
import org.intellij.sequencer.openapi.model.CallStack;
import org.intellij.sequencer.util.MyPsiUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MoreSubClassAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        String basePath = project.getBasePath();
        List<PsiElement> list = new LinkedList<>();
        PsiElement psiElement = event.getData(CommonDataKeys.PSI_ELEMENT);
        {
            if (psiElement instanceof PsiClass) {
                list.add(psiElement);
            } else {
                list = getPsiElement(psiElement);
            }
        }
        Map<PsiElement, List<PsiElement>> clazzName2PsiElementMap = new HashMap<>();
        {
            for (PsiElement element : list) {
                if (element instanceof PsiClass) {
                    PsiClass psiClass = (PsiClass) element;
                    for (PsiClass clazz : psiClass.getSupers()) {
                        if (!MyPsiUtil.isExternal(clazz)) {
                            List<PsiElement> valueList = clazzName2PsiElementMap.get(clazz);
                            if (CollectionUtils.isEmpty(valueList)) {
                                valueList = new LinkedList<>();
                            }
                            valueList.add(element);
                            clazzName2PsiElementMap.put(clazz, valueList);
                        }
                    }
                }
            }
        }
        {
            List<CallStack> callStackList = new LinkedList<>();
            InvocationChainAction action = new InvocationChainAction();
            for (Map.Entry<PsiElement, List<PsiElement>> entry : clazzName2PsiElementMap.entrySet()) {
                List<PsiElement> elementList = entry.getValue();
                int size = CollectionUtils.size(elementList);
                if (size > 1) {
                    List<CallStack> ll = action.filterSpecialCallStack(elementList);
                    callStackList.addAll(ll);
                }
            }
            action.printSpecialCallStack(basePath, callStackList);
        }
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
