package io.austinbarrett.cppparameternames;

import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.psi.PsiElement;
import com.jetbrains.cidr.lang.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ParameterAnnotator implements Annotator {
    @Override
    public void annotate(@NotNull final PsiElement element, @NotNull AnnotationHolder holder) {
        if (element instanceof OCArgumentList && ((OCArgumentList) element).getArguments().size() > 0) {
            OCArgumentList argumentList = (OCArgumentList) element;
            ArgumentListInfo info = new ArgumentListInfo(argumentList);

            if (info.isValid()) {
                List<OCExpression> children = info.getArguments();

                for (int i = 0; i < children.size(); i++) {
                    String defaultValue = info.getDefaultValue(i);
                    String type = info.getTypeName(i);
                    String name = info.getArgumentName(i);

                    String annotationValue = type + " " + name;
                    if (defaultValue != null) {
                        annotationValue += " = " + defaultValue;
                    }

                    Annotation hoverAnnotation = holder.createInfoAnnotation(children.get(i), annotationValue);
                    hoverAnnotation.setTextAttributes(DefaultLanguageHighlighterColors.PARAMETER);
                }
                HighlightSeverity severity =
                    info.getMissingParameters() > 0 ? HighlightSeverity.ERROR : HighlightSeverity.INFORMATION;
                holder.createAnnotation(severity, argumentList.getPrevSibling().getTextRange(), info.getFunctionSignature());

            } else {
                holder.createWarningAnnotation(element, "No matching signatures found");
            }


        }
    }
}
