package com.heima.search.service;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.search.dtos.UserSearchDto;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;

public interface ArticleSearchService {
    /**
     * 文章分页检索
     * @param dto
     * @return
     */
    public ResponseResult search( UserSearchDto dto) throws IOException;
}
