package io.austinbarrett.cppparameternames;

import com.intellij.psi.PsiElement;
import com.jetbrains.cidr.lang.editor.parameterInfo.OCArgumentListCallPlace;
import com.jetbrains.cidr.lang.editor.parameterInfo.OCFunctionCallOption;
import com.jetbrains.cidr.lang.editor.parameterInfo.OCFunctionParameterInfo;
import com.jetbrains.cidr.lang.psi.OCArgumentList;
import com.jetbrains.cidr.lang.psi.OCExpression;
import com.jetbrains.cidr.lang.refactoring.changeSignature.OCParameterInfo;
import com.jetbrains.cidr.lang.symbols.OCResolveContext;
import com.jetbrains.cidr.lang.types.OCFunctionType;
import com.jetbrains.cidr.lang.types.OCType;

import java.util.ArrayList;
import java.util.List;

public class ArgumentListInfo {
    private OCArgumentList argumentList;
    private List<OCExpression> arguments;
    private OCArgumentListCallPlace argumentListCallPlace;
    private OCFunctionParameterInfo parameterInfo;
    private OCFunctionType fnType;
    private List<OCType> types;
    private List<String> typeNames;
    private List<String> names;
    private List<String> defaultValues;
    private boolean validity;
    private int missingParameters;

    public ArgumentListInfo(OCArgumentList argList) {
        argumentList = argList;
        arguments = argumentList.getArguments();

        argumentListCallPlace = new OCArgumentListCallPlace(argumentList);
        List<OCFunctionCallOption> callOptions = new ArrayList<>();
        argumentListCallPlace.collectCallOptions(callOptions);
        
        OCFunctionCallOption firstMatchingOption = null;
        OCFunctionCallOption incompleteMatchingOption = null;
        int minMissingParameters = 0;
        for (OCFunctionCallOption callOption : callOptions) {
            List<OCType> types = callOption.getParameterInfo().getType().getParameterTypes();

            if (types.size() >= arguments.size()) {
                boolean viable = true;

                for (int i = 0; i < arguments.size(); i++) {
                    OCType childType = argumentList.getArgumentTypes(OCResolveContext.forPsi(argumentList)).get(i);
                    if (!typesCompatible(types.get(i), childType, argumentList)) {
                        viable = false;
                    }
                }

                if (viable) {
                    int optionMissingParameters = 0;
                    if (types.size() > arguments.size()) {
                        List<String> defaults = callOption.getParameterInfo().getDefaultParameterValues();
                        for (int i = arguments.size(); i < defaults.size(); i++) {
                            if (defaults.get(i) == null) {
                                optionMissingParameters++;
                            }
                        }
                    }
                    if (optionMissingParameters == 0) {
                        firstMatchingOption = callOption;
                        break;
                    } else if (incompleteMatchingOption == null || minMissingParameters > optionMissingParameters) {
                        incompleteMatchingOption = callOption;
                    }

                }
            }
        }

        if (firstMatchingOption == null && incompleteMatchingOption != null) {
            firstMatchingOption = incompleteMatchingOption;
            missingParameters = minMissingParameters;
        }

        if (firstMatchingOption != null) {
            parameterInfo = firstMatchingOption.getParameterInfo();
            defaultValues = parameterInfo.getDefaultParameterValues();
            fnType = parameterInfo.getType();
            types = fnType.getParameterTypes();
            names = fnType.getParameterNames();
            typeNames = new ArrayList<>();
            for (int i = 0; i < types.size(); i++) {
                typeNames.add(i, types.get(i).getName());
            }
            validity = true;
        } else {
            validity = false;
        }
    }

    public boolean isValid() {
        return validity;
    }

    public int getMissingParameters() {
        return missingParameters;
    }

    public String getTypeName(int i) {
        return typeNames.get(i);
    }

    public String getArgumentName(int i) {
        return names.get(i);
    }

    public String getDefaultValue(int i) {
        return defaultValues.get(i);
    }

    public int getArgumentsCount() {
        return arguments.size();
    }

    public List<OCExpression> getArguments() {
        return arguments;
    }

    public OCExpression getArgument(int i) {
        return arguments.get(i);
    }

    public String getFunctionSignature() {
        return fnType.getName();
    }

    private boolean typesCompatible(OCType wanted, OCType passed, PsiElement context) {
        OCResolveContext resolveContext = OCResolveContext.forPsi(context);

        // Special handling for void pointers
        if (passed.getName().equals("void *") && wanted.getName().endsWith("*")) {
            return true;
        }

        // Special handling for uintXX_t vs int. There *has* to be a better way to do this
        if (passed.getName().matches("[a-z]?int([0-9]{1,2}_t)?") && wanted.getName().matches("[a-z]?int([0-9]{1,2}_t)?")) {
            return true;
        }

        // These functions are poorly documented, so if it's close, call it ok
        return wanted.isCompatible(passed, resolveContext) ||
                passed.isConvertibleByOperator(wanted, resolveContext, true) ||
                passed.isConvertibleByOperator(wanted, resolveContext, false) ||
                wanted.isConvertibleByOperator(passed, resolveContext, true) ||
                wanted.isConvertibleByOperator(passed, resolveContext, false) ||
                wanted.isSuperType(passed, context) ||
                passed.isSuperType(wanted, context) ||
                wanted.equalsAfterResolving(passed, resolveContext) ||
                passed.equalsAfterResolving(wanted, resolveContext) ||
                passed.equalsWithAliasName(wanted, resolveContext) ||
                wanted.equalsWithAliasName(passed, resolveContext);
    }
}
