/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.idea.blaze.base.lang.buildfile.completion;

import static com.intellij.patterns.PlatformPatterns.psiComment;
import static com.intellij.patterns.PlatformPatterns.psiElement;

import com.google.idea.blaze.base.lang.buildfile.language.BuildFileLanguage;
import com.google.idea.blaze.base.lang.buildfile.language.semantics.BuildLanguageSpec;
import com.google.idea.blaze.base.lang.buildfile.language.semantics.BuildLanguageSpecProvider;
import com.google.idea.blaze.base.lang.buildfile.language.semantics.RuleDefinition;
import com.google.idea.blaze.base.lang.buildfile.lexer.BuildToken;
import com.google.idea.blaze.base.lang.buildfile.lexer.TokenKind;
import com.google.idea.blaze.base.lang.buildfile.psi.Argument;
import com.google.idea.blaze.base.lang.buildfile.psi.FuncallExpression;
import com.google.idea.blaze.base.lang.buildfile.psi.util.PsiUtils;
import com.intellij.codeInsight.completion.AutoCompletionContext;
import com.intellij.codeInsight.completion.AutoCompletionDecision;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import javax.annotation.Nullable;

/** Known attributes for built-in blaze functions. */
public class BuiltInFunctionAttributeCompletionContributor extends CompletionContributor {

  @Override
  public AutoCompletionDecision handleAutoCompletionPossibility(AutoCompletionContext context) {
    // auto-insert the obvious only case; else show other cases.
    final LookupElement[] items = context.getItems();
    if (items.length == 1) {
      return AutoCompletionDecision.insertItem(items[0]);
    }
    return AutoCompletionDecision.SHOW_LOOKUP;
  }

  public BuiltInFunctionAttributeCompletionContributor() {
    extend(
        CompletionType.BASIC,
        psiElement()
            .withLanguage(BuildFileLanguage.INSTANCE)
            .inside(psiElement(FuncallExpression.class))
            .andNot(psiComment())
            .andNot(psiElement().afterLeaf("."))
            .andOr(
                psiElement().withSuperParent(2, FuncallExpression.class),
                psiElement()
                    .withSuperParent(2, Argument.class)
                    .andNot(psiElement().afterLeaf("="))
                    .andNot(
                        psiElement()
                            .afterLeaf(psiElement(BuildToken.fromKind(TokenKind.IDENTIFIER))))),
        new CompletionProvider<CompletionParameters>() {
          @Override
          protected void addCompletions(
              CompletionParameters parameters,
              ProcessingContext context,
              CompletionResultSet result) {
            BuildLanguageSpec spec =
                BuildLanguageSpecProvider.getInstance()
                    .getLanguageSpec(parameters.getPosition().getProject());
            if (spec == null) {
              return;
            }
            RuleDefinition rule = spec.getRule(getEnclosingFuncallName(parameters.getPosition()));
            if (rule == null) {
              return;
            }
            for (String attributeName : rule.getKnownAttributeNames()) {
              result.addElement(
                  LookupElementBuilder.create(attributeName).withIcon(AllIcons.Nodes.Parameter));
            }
          }
        });
  }

  @Nullable
  private static String getEnclosingFuncallName(PsiElement element) {
    FuncallExpression funcall = PsiUtils.getParentOfType(element, FuncallExpression.class, true);
    return funcall != null ? funcall.getFunctionName() : null;
  }
}
