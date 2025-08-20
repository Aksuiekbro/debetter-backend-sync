package com.heliozz10.debetter.config;

import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.ngram.EdgeNGramFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LuceneAnalysisConfig implements LuceneAnalysisConfigurer {
    @Override
    public void configure(LuceneAnalysisConfigurationContext context) {
        context.analyzer("edge_ngram").custom()
                .tokenizer(StandardTokenizerFactory.class)
                .tokenFilter(LowerCaseFilterFactory.class)
                .tokenFilter(EdgeNGramFilterFactory.class)
                .param("minGramSize", "2")
                .param("maxGramSize", "6");
    }
}
