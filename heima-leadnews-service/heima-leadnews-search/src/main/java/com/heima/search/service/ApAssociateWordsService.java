package com.heima.search.service;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.search.dtos.UserSearchDto;
import org.springframework.web.bind.annotation.RequestBody;

public interface ApAssociateWordsService {
    /**
     * 联想词查询
     * @param dto
     * @return
     */
    public ResponseResult search( UserSearchDto dto);
}
