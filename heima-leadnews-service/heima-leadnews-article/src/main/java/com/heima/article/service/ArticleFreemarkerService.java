package com.heima.article.service;

import com.heima.model.article.pojos.ApArticle;

public interface ArticleFreemarkerService {
    /**
     * 上床文件到minion中
     * @param apArticle
     * @param content
     */
    public void buildArticleToMinIo(ApArticle apArticle,String content);
}
