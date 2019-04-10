package io.austinbarrett.cppparameternames;

import com.intellij.codeInsight.hints.HintInfo;
import com.intellij.codeInsight.hints.InlayInfo;
import com.intellij.codeInsight.hints.InlayParameterHintsProvider;
import com.intellij.codeInsight.hints.Option;
import com.intellij.psi.PsiElement;
import com.jetbrains.cidr.lang.psi.OCArgumentList;
import com.jetbrains.cidr.lang.psi.OCCallExpression;
import com.jetbrains.cidr.lang.psi.OCExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CppParameterNameHints implements InlayParameterHintsProvider {

    @NotNull
    @Override
    public List<InlayInfo> getParameterHints(PsiElement psiElement) {
        if (psiElement instanceof OCArgumentList) {
            return getParameters((OCArgumentList) psiElement);
        } else {
            return Collections.emptyList();
        }
    }

    @Nullable
    @Override
    public HintInfo getHintInfo(PsiElement psiElement) {
        return null;
    }

    @NotNull
    @Override
    public Set<String> getDefaultBlackList() {
        return Collections.emptySet();
    }

    @NotNull
    @Override
    public List<Option> getSupportedOptions() {
        //return Collections.singletonList(CppParameterNameHints.OPTION_PARAM_HINTS_NON_LITERALS);
        return Collections.emptyList();
    }

    @Override
    public boolean isBlackListSupported() {
        return false;
    }

    private List<InlayInfo> getParameters(OCArgumentList argumentList) {
        List<InlayInfo> inlayInfos = new ArrayList<>();
        ArgumentListInfo info = new ArgumentListInfo(argumentList);
        if (info.isValid()) {
            for (int i = 0; i < info.getArgumentsCount(); i++) {
                inlayInfos.add(new InlayInfo(info.getArgumentName(i), info.getArgument(i).getTextRange().getStartOffset()));
            }
        }
        return inlayInfos;
    }
}
