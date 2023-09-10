package com.heima.article.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.common.dtos.ResponseResult;

public interface ApArticleService extends IService<ApArticle> {
    /**
     *  加载文章列表
     * @param dto
     * @param type 1 加载更多 2 加载更新
     * @return
     */
    public ResponseResult load(ArticleHomeDto dto,Short type);

    /**
     *  加载文章列表
     * @param dto
     * @param type 1 加载更多 2 加载更新
     * @firstPage true是首页 false 不是首页
     * @return
     */
    public ResponseResult load2(ArticleHomeDto dto,Short type,boolean firstPage);


    /**
     * 保存APP端相关文章
     * @param dto
     * @return
     */
    public ResponseResult saveArticle(ArticleDto dto);
}
