/*
 * Copyright 2024-2026 Jared Ottley
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.ottleys.xpository.config;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;

/**
 * Lucene analysis configuration for Hibernate Search indexing.
 *
 * <p>Defines text analysis strategies for full-text search including a default analyzer with
 * standard tokenization and lowercasing, and an exact analyzer for keyword-based matching without
 * tokenization.
 */
public class XpositoryLuceneAnalysisConfigurer implements LuceneAnalysisConfigurer {

  /**
   * Configures Lucene text analyzers for full-text search indexing.
   *
   * <p>Sets up a default analyzer for general text search and an exact analyzer for precise keyword
   * matching.
   *
   * @param context the Lucene analysis configuration context
   */
  @Override
  public void configure(LuceneAnalysisConfigurationContext context) {
    // Default analyzer configuration
    context.analyzer("default").custom().tokenizer("standard").tokenFilter("lowercase");

    // Exact match analyzer (no tokenization)
    context.analyzer("exact").custom().tokenizer("keyword").tokenFilter("lowercase");
  }
}
