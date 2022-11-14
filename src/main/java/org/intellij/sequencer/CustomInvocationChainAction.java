package org.intellij.sequencer;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import org.intellij.sequencer.config.SequenceParamsState;
import org.intellij.sequencer.generator.filters.NoConstructorsFilter;
import org.intellij.sequencer.generator.filters.NoGetterSetterFilter;
import org.intellij.sequencer.generator.filters.NoPrivateMethodsFilter;
import org.intellij.sequencer.openapi.GeneratorFactory;
import org.intellij.sequencer.openapi.IGenerator;
import org.intellij.sequencer.openapi.SequenceParams;
import org.intellij.sequencer.openapi.filters.ProjectOnlyFilter;
import org.intellij.sequencer.openapi.model.CallStack;
import org.jetbrains.annotations.NotNull;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * 调用链
 *
 * @author xujin
 */
public class CustomInvocationChainAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent event) {


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
        //提取调用堆栈
        List<CallStack> callStackList = filterSpecialCallStack(list);
        for (CallStack callStack : callStackList) {
            printSpecialCallStack(basePath, Arrays.asList(callStack));
        }
    }

    public List<CallStack> filterSpecialCallStack(List<PsiElement> list) {
        List<CallStack> callStackList = new LinkedList<>();
        SequenceParams params = new SequenceParams();
        SequenceParamsState state = SequenceParamsState.getInstance();

        params.setMaxDepth(state.callDepth);
        params.getMethodFilter().addFilter(new ProjectOnlyFilter(state.projectClassesOnly));
        params.getMethodFilter().addFilter(new NoGetterSetterFilter(state.noGetterSetters));
        params.getMethodFilter().addFilter(new NoPrivateMethodsFilter(state.noPrivateMethods));
        params.getMethodFilter().addFilter(new NoConstructorsFilter(state.noConstructors));
        params.setAllowRecursion(state.allowChainInvocation);

        int i = 0;
        for (PsiElement element : list) {
            PsiElement @NotNull [] methodList = element.getChildren();
            //System.out.println(element.toString());
            for (PsiElement methodElement : methodList) {
                if (methodElement instanceof PsiMethod) {
                    IGenerator generator = GeneratorFactory.createGenerator(element.getLanguage(), params);
                    final CallStack callStack = generator.generate(methodElement, null);
                    System.out.println(i++);
                    callStackList.add(callStack);
                }
            }
        }
        return callStackList;
    }

    public void printSpecialCallStack(String basePath, List<CallStack> callStackList) {
        try {
            int ii = 0;
            FileWriter file = new FileWriter(basePath + "/callstack.txt");
            for (CallStack callStack : callStackList) {
                System.out.println(ii++ + "#" + callStackList.size());
                String line = callStack.toString();
                if (line.contains("com.qiekj.ads.dao.mapper") || line.contains("com.qiekj.bi.dao.mapper") || line.contains("com.qiekj.hologres.dao.mapper") || line.contains("com.qiekj.sell.dao.mapper")) {
                    file.append(callStack.toString() + "\n\n");
                }
            }
            file.flush();
            file.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
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