package org.intellij.sequencer;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import org.apache.commons.io.FileUtils;
import org.intellij.sequencer.openapi.GeneratorFactory;
import org.intellij.sequencer.openapi.IGenerator;
import org.intellij.sequencer.openapi.SequenceParams;
import org.intellij.sequencer.openapi.model.CallStack;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * 右键创建Basic Action
 *
 * @author xujin
 */
public class NewBasicExeAction extends AnAction {

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
        List<CallStack> callStackList = new LinkedList<>();
        SequenceParams sequenceParams = new SequenceParams();
        sequenceParams.setMaxDepth(50);
        int i = 0;

        for (PsiElement element : list) {
            PsiElement @NotNull [] methodList = element.getChildren();
            System.out.println(element.toString());
            for (PsiElement methodElement : methodList) {
                if (methodElement instanceof PsiMethod) {
                    IGenerator generator = GeneratorFactory.createGenerator(psiElement.getLanguage(), sequenceParams);
                    final CallStack callStack = generator.generate(methodElement, null);
                    System.out.println(i++);
                    callStackList.add(callStack);
                }
            }
        }

        try {
            int ii = 0;
            FileWriter file = new FileWriter(basePath + "/callstack.txt");
            for (CallStack callStack : callStackList) {
                System.out.println(ii++ + "#" + callStackList.size());
                String line = callStack.toString();
                //if (line.contains("com.qiekj.ads.dao.mapper") || line.contains("com.qiekj.bi.dao.mapper")|| line.contains("com.qiekj.hologres.dao.mapper")) {
                    file.append(callStack.toString()+"\n\n");
                //}
            }
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