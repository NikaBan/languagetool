/* LanguageTool, a natural language style checker 
 * Copyright (C) 2008 Daniel Naber (http://www.danielnaber.de)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */

package org.languagetool.rules.patterns;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.languagetool.AnalyzedSentence;
import org.languagetool.Language;
import org.languagetool.rules.Rule;
import org.languagetool.rules.RuleMatch;
import org.languagetool.tagging.disambiguation.rules.DisambiguationPatternRule;

/**
 * An Abstract Pattern Rule that describes a pattern of words or part-of-speech tags 
 * used for PatternRule and DisambiguationPatternRule.
 * 
 * Introduced to minimize code duplication between those classes.
 * 
 * @author Marcin Miłkowski
 */
public abstract class AbstractPatternRule extends Rule {

  protected final Language language;
  protected final List<PatternToken> patternTokens;
  protected final Pattern regex;
  protected final boolean testUnification;
  protected final boolean sentStart;
  protected final List<Match> suggestionMatches = new ArrayList<>();
  protected final List<Match> suggestionMatchesOutMsg = new ArrayList<>();
  protected final List<DisambiguationPatternRule> antiPatterns = new ArrayList<>();

  protected String subId; // because there can be more than one rule in a rule group
  protected int startPositionCorrection;
  protected int endPositionCorrection;
  protected String suggestionsOutMsg; // extra suggestions outside message
  protected RuleFilter filter;
  protected String filterArgs;
  protected String message;

  private final String id;
  private final String description;
  private final boolean getUnified;
  private final boolean groupsOrUnification;

  /**
   * @since 3.2
   */
  public AbstractPatternRule(String id, String description, Language language, Pattern regex) {
    this(id, description, language, null, regex, false);
  }

  public AbstractPatternRule(String id, String description, Language language, List<PatternToken> patternTokens, boolean getUnified, String message) {
    this(id, description, language, patternTokens, null, getUnified);
    this.message = message;
  }

  public AbstractPatternRule(String id, String description, Language language, List<PatternToken> patternTokens, boolean getUnified) {
    this(id, description, language, patternTokens, null, getUnified);
  }

  private AbstractPatternRule(String id, String description, Language language, List<PatternToken> patternTokens, Pattern regex, boolean getUnified) {
    this.id = Objects.requireNonNull(id, "id cannot be null");
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.language = Objects.requireNonNull(language, "language cannot be null");
    this.getUnified = getUnified;
    if (patternTokens == null && regex == null) {
      throw new IllegalArgumentException("patternTokens and regex cannot both be null");
    }
    if (patternTokens != null) {
      this.patternTokens = new ArrayList<>(patternTokens);
      testUnification = initUnifier();
      sentStart = this.patternTokens.size() > 0 && this.patternTokens.get(0).isSentenceStart();
      if (!testUnification) {
        boolean found = false;
        for (PatternToken elem : this.patternTokens) {
          if (elem.hasAndGroup()) {
            found = true;
            break;
          }
        }
        groupsOrUnification = found;
      } else {
        groupsOrUnification = true;
      }
      this.regex = null;
    } else {
      this.regex = regex;
      this.patternTokens = null;
      groupsOrUnification = false;
      sentStart = false;
      testUnification = false;
    }
  }

  @Override
  public boolean supportsLanguage(final Language language) {
    return language.equalsConsiderVariantsIfSpecified(this.language);
  }

  private boolean initUnifier() {
    for (final PatternToken pToken : patternTokens) {
      if (pToken.isUnified()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return id + "[" + subId + "]:" + patternTokens + ":" + description;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public RuleMatch[] match(AnalyzedSentence sentence) throws IOException {
    return null;
  }

  @Override
  public void reset() {
  }

  /**
   * @since 2.3
   */
  public final Language getLanguage() {
    return language;
  }

  public final void setStartPositionCorrection(final int startPositionCorrection) {
    this.startPositionCorrection = startPositionCorrection;
  }

  public final int getStartPositionCorrection() {
    return startPositionCorrection;
  }

  public final void setEndPositionCorrection(final int endPositionCorrection) {
    this.endPositionCorrection = endPositionCorrection;
  }

  public final int getEndPositionCorrection() {
    return endPositionCorrection;
  }

  /**
   * The rule id and it's sub id, if any. The format is like {@code RULE_ID[SUB_ID]}.
   * @since 3.2
   */
  public String getFullId() {
    if (subId != null) {
      return id + "[" + subId + "]";
    } else {
      return id;
    }
  }

  public final String getSubId() {
    return subId;
  }

  public final void setSubId(final String subId) {
    this.subId = subId;
  }

  /**
   * @since 2.3
   */
  public boolean isGroupsOrUnification() {
    return groupsOrUnification;
  }

  /**
   * @since 2.3
   */
  public boolean isGetUnified() {
    return getUnified;
  }

  /**
   * @since 2.3
   */
  public boolean isSentStart() {
    return sentStart;
  }

  /**
   * @since 2.3
   */
  public boolean isTestUnification() {
    return testUnification;
  }

  /**
   * @since 2.3
   */
  public List<PatternToken> getPatternTokens() {
    return patternTokens;
  }

  /** Add formatted suggestion elements. */
  public final void addSuggestionMatch(final Match m) {
    suggestionMatches.add(m);
  }

  /** Add formatted suggestion elements outside message. */
  public final void addSuggestionMatchOutMsg(final Match m) {
    suggestionMatchesOutMsg.add(m);
  }
  
  List<Match> getSuggestionMatches() {
    return suggestionMatches;
  }

  List<Match> getSuggestionMatchesOutMsg() {
    return suggestionMatchesOutMsg;
  }

  @NotNull
  public final String getSuggestionsOutMsg() {
    return suggestionsOutMsg;
  }
  
  /**
   * Get the message shown to the user if this rule matches.
   */
  public final String getMessage() {
    return message;
  }

  /**
   * Set the message shown to the user if this rule matches.
   */
  public final void setMessage(final String message) {
    this.message = message;
  }

  /** @since 2.7 (public since 3.2) */
  public void setFilter(RuleFilter filter) {
    this.filter = filter;
  }

  /** @since 2.7 (public since 3.2) */
  @Nullable
  public RuleFilter getFilter() {
    return filter;
  }

  /** @since 2.7 (public since 3.2) */
  public void setFilterArguments(String filterArgs) {
    this.filterArgs = filterArgs;
  }

  /** @since 2.7 (public since 3.2) */
  @Nullable
  public String getFilterArguments() {
    return filterArgs;
  }

  /**
   * Set up the list of antipatterns used to immunize tokens, i.e., make them
   * non-matchable by the current rule. Useful for multi-word complex exceptions,
   * such as multi-word idiomatic expressions.
   * @param antiPatterns A list of antiPatterns, implemented as {@code DisambiguationPatternRule}.
   * @since 2.5
   */
  public void setAntiPatterns(List<DisambiguationPatternRule> antiPatterns) {
    this.antiPatterns.addAll(antiPatterns);
  }

  /**
   * @since 3.1
   */
  @Override
  public final List<DisambiguationPatternRule> getAntiPatterns() {
    return Collections.unmodifiableList(antiPatterns);
  }

}
