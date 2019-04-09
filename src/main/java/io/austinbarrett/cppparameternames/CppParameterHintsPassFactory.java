package io.austinbarrett.cppparameternames;

import com.intellij.codeHighlighting.TextEditorHighlightingPass;
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory;
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar;
import com.intellij.codeInsight.hints.InlayParameterHintsExtension;
import com.intellij.codeInsight.hints.InlayParameterHintsProvider;
import com.intellij.codeInsight.hints.MethodInfoBlacklistFilter;
import com.intellij.codeInsight.hints.ParameterHintsPass;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CppParameterHintsPassFactory implements TextEditorHighlightingPassFactory {
    protected static final Key<Long> PSI_MODIFICATION_STAMP = Key.create("psi.modification.stamp");

    CppParameterHintsPassFactory(TextEditorHighlightingPassRegistrar registrar) {
        registrar.registerTextEditorHighlightingPass(this, null, null, false, -1);
    }

    @Nullable
    @Override
    public TextEditorHighlightingPass createHighlightingPass(@NotNull PsiFile file, @NotNull Editor editor) {
        if (editor.isOneLineMode()) return null;
        long currentStamp = getCurrentModificationStamp(file);
        Long savedStamp = editor.getUserData(PSI_MODIFICATION_STAMP);
        if (savedStamp != null && savedStamp == currentStamp) return null;
        Language language = file.getLanguage();
        InlayParameterHintsProvider provider = InlayParameterHintsExtension.INSTANCE.forLanguage(language);
        if (provider == null) return null;
        return new ParameterHintsPass(file, editor, MethodInfoBlacklistFilter.forLanguage(Language.findLanguageByID("JAVA")), false);
    }

    public static long getCurrentModificationStamp(@NotNull PsiFile file) {
        return file.getManager().getModificationTracker().getModificationCount();
    }

    public static void forceHintsUpdateOnNextPass() {
        for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
            forceHintsUpdateOnNextPass(editor);
        }
    }

    public static void forceHintsUpdateOnNextPass(@NotNull Editor editor) {
        editor.putUserData(PSI_MODIFICATION_STAMP, null);
    }

    protected static void putCurrentPsiModificationStamp(@NotNull Editor editor, @NotNull PsiFile file) {
        editor.putUserData(PSI_MODIFICATION_STAMP, getCurrentModificationStamp(file));
    }

}
