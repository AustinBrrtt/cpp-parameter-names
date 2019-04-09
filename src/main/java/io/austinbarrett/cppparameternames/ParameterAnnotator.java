package io.austinbarrett.cppparameternames;

import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.cidr.lang.editor.parameterInfo.OCArgumentListCallPlace;
import com.jetbrains.cidr.lang.editor.parameterInfo.OCFunctionCallOption;
import com.jetbrains.cidr.lang.editor.parameterInfo.OCFunctionParameterInfo;
import com.jetbrains.cidr.lang.psi.*;
import com.jetbrains.cidr.lang.psi.impl.OCExpressionBase;
import com.jetbrains.cidr.lang.psi.impl.OCTypeElementImpl;
import com.jetbrains.cidr.lang.symbols.OCResolveContext;
import com.jetbrains.cidr.lang.types.OCFunctionType;
import com.jetbrains.cidr.lang.types.OCType;
import org.intellij.plugins.relaxNG.compact.psi.util.PsiFunction;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ParameterAnnotator implements Annotator {
    @Override
    public void annotate(@NotNull final PsiElement element, @NotNull AnnotationHolder holder) {
        if (element instanceof OCExpressionBase && element.getParent() instanceof OCArgumentList) {
            OCArgumentList parent = (OCArgumentList) element.getParent();
            List<OCExpression> children = parent.getArguments();

            OCExpression elem = (OCExpression) element;
            int index = children.indexOf(elem);

            OCArgumentListCallPlace callPlace = new OCArgumentListCallPlace(parent);
            List<OCFunctionCallOption> callOptions = new ArrayList<>();
            callPlace.collectCallOptions(callOptions);

            String errors = " ";

            OCFunctionCallOption firstMatchingOption = null;
            for (OCFunctionCallOption callOption : callOptions) {
                List<OCType> types = callOption.getParameterInfo().getType().getParameterTypes();

                if (types.size() >= children.size()) {
                    boolean viable = true;
                    String incompatibilities = "";

                    for (int i = 0; i < children.size(); i++) {
                        OCType childType = parent.getArgumentTypes(OCResolveContext.forPsi(element)).get(i);
                        if (!typesCompatible(types.get(i), childType, element)) {
                            viable = false;
                            errors += i + ": " + types.get(i).getName() + " vs " + childType.getName() + "\n";
                        }
                    }

                    if (viable) {
                        firstMatchingOption = callOption;
                        break;
                    }
                }
            }

            if (firstMatchingOption != null) {
                OCFunctionParameterInfo parameterInfo = firstMatchingOption.getParameterInfo();
                String defaultValue = parameterInfo.getDefaultParameterValues().get(index);
                OCFunctionType fnType = parameterInfo.getType();
                String type = fnType.getParameterTypes().get(index).getName();
                String name = fnType.getParameterNames().get(index);

                String annotationValue = type + " " + name;
                if (defaultValue != null) {
                    annotationValue += " = " + defaultValue;
                }

                Annotation annotation = holder.createInfoAnnotation(element, annotationValue);
                annotation.setTextAttributes(DefaultLanguageHighlighterColors.PARAMETER);
            } else {
                holder.createErrorAnnotation(element, "No matching signatures found" + errors);
            }


        }
    }

    private boolean typesCompatible(OCType wanted, OCType passed, PsiElement context) {
        OCResolveContext resolveContext = OCResolveContext.forPsi(context);

        // Special handling for void pointers
        if (passed.getName().equals("void *") && wanted.getName().endsWith("*")) {
            return true;
        }

        // Special handling for uintXX_t (note: update this logic later. This should be doable with actual code.
        if (passed.getName().equals("int") && wanted.getName().matches("[a-z]?int[0-9]{1,2}_t")) {
            return true;
        }

        return wanted.isCompatible(passed, resolveContext) ||
                passed.isConvertibleByOperator(wanted, resolveContext, true) ||
                wanted.isSuperType(passed, context) ||
                wanted.equalsAfterResolving(passed, resolveContext) ||
                passed.equalsAfterResolving(wanted, resolveContext) ||
                passed.equalsWithAliasName(wanted, resolveContext) ||
                wanted.equalsWithAliasName(passed, resolveContext);
    }
}
