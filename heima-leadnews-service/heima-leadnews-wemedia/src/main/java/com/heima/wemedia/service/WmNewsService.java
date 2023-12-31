package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmNews;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface WmNewsService extends IService<WmNews> {

    /**
     * 条件查询文章列表
     * @param dto
     * @return
     */
    public ResponseResult findList(@RequestBody WmNewsPageReqDto dto);

    /**
     * 发布修改文章或者保存草稿
     * @param dto
     * @return
     */
    public ResponseResult submitNews(WmNewsDto dto);


    /**
     * 文章的上下架
     * @param dto
     * @return
     */
    @PostMapping("down_or_up")
    public ResponseResult downOrUp(@RequestBody WmNewsDto dto);
}
