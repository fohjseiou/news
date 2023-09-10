package com.heima.wemedia.service.impl;


import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.constants.WemediaConstants;
import com.heima.common.constants.WmNewsMessageConstants;
import com.heima.common.exception.CustomException;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.utils.thread.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import com.heima.wemedia.service.WmNewsService;
import com.heima.wemedia.service.WmNewsTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class WmNewsServiceImpl  extends ServiceImpl<WmNewsMapper, WmNews> implements WmNewsService {

    @Resource
    private WmNewsMaterialMapper wmNewsMaterialMapper;
    @Autowired
    private WmNewsTaskService wmNewsTaskService;
    /**
     * 条件查询文章列表
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult findList(WmNewsPageReqDto dto) {
        //1.检查参数
        dto.checkParam();
        //2.分页的条件查询
        IPage page = new Page(dto.getPage(),dto.getSize());
        LambdaQueryWrapper<WmNews> lambdaQueryWrapper =new LambdaQueryWrapper<>();
        //状态精确查询
        if (dto.getStatus() != null){
            lambdaQueryWrapper.eq(WmNews::getStatus,dto.getStatus());
        }
        //频道精确查询
        if (dto.getChannelId() != null){
            lambdaQueryWrapper.eq(WmNews::getChannelId,dto.getChannelId());
        }
        //时间范围查询
        if (dto.getBeginPubDate() != null && dto.getEndPubDate() != null){
            lambdaQueryWrapper.between(WmNews::getPublishTime,dto.getBeginPubDate(),dto.getEndPubDate());
        }
        //关键字的模糊查询
        if (StringUtils.isNotBlank(dto.getKeyword())){
            lambdaQueryWrapper.like(WmNews::getTitle,dto.getKeyword());
        }
        //查询当前登录人的文章
        try {
            lambdaQueryWrapper.eq(WmNews::getUserId, WmThreadLocalUtil.getUser().getId());
        }catch (Exception e){
            e.printStackTrace();
        }

        //按照发布时间倒叙查询
        lambdaQueryWrapper.orderByDesc(WmNews::getPublishTime);
        page = page(page,lambdaQueryWrapper);

        //3.返回参数
        ResponseResult responseResult = new PageResponseResult(dto.getPage(),dto.getSize(),(int)page.getTotal());
        responseResult.setData(page.getRecords());

        return responseResult;
    }

    @Autowired
    private WmNewsAutoScanService wmNewsAutoScanService;
    /**
     * 发布修改文章或者保存草稿
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult submitNews(WmNewsDto dto) {

        //0.条件判断
        if (dto == null || dto.getContent() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //1.保存或修改文章
        WmNews wmNews = new WmNews();
        //拷贝属性
        BeanUtils.copyProperties(dto,wmNews);
        //封面图片, list--->string
        if (dto.getImages() != null && dto.getImages().size()>0){
            String imageStr = org.apache.commons.lang3.StringUtils.join(dto.getImages(), ",");
            wmNews.setImages(imageStr);
        }
        //如果当前封面类型为自动 -1
        if (dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)){
            wmNews.setType(null);
        }
        saveOrUpdateWmNews(wmNews);
        //2.判断是否为草稿，如果为草稿则直接结束当前方法
         if (dto.getStatus().equals(WmNews.Status.NORMAL.getCode())){
             return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
         }
        //3.不是草稿，保存文章内容图片与素材的关系
        //提取到文章内容的图片信息
        List<String> material = ectractUrlInfo(dto.getContent());
        saveRelativeInfoForContent(material,wmNews.getId());
        //4.不是草稿，保存文章封面图片与素材的关系,如果当前布局是自动，需要匹配封面图片
        saveRelativeInfoForCover(dto,wmNews,material);
        //审核文章
        wmNewsTaskService.addNewsToTack(wmNews.getId(),wmNews.getPublishTime());
        //wmNewsAutoScanService.autoScanWmNews(wmNews.getId());

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 第一个功能，如果当前封面类型为自动，则设置封面类型的数据
     * 匹配规则
     * 1.如果内容图片大于等于1 ，小于3 单图 type 1
     * 2.如果内容图片大于等于3 多图 type 3
     * 3.如果内容没有图片，无图 type 0
     *
     * 第二个功能，保存图片与素材的关系
     * @param dto
     * @param wmNews
     * @param material
     */
    private void saveRelativeInfoForCover(WmNewsDto dto, WmNews wmNews, List<String> material) {
        //如果当前封面类型为自动，则设置封面类型的数据
        List<String> images = dto.getImages();
        if (dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)){
            //多图
            if(material.size() > 3){
                wmNews.setType(WemediaConstants.WM_NEWS_MANY_IMAGE);
                images =  material.stream().limit(3).collect(Collectors.toList());
            }else if (material.size() >= 1 && material.size() < 3){
                //单图
                wmNews.setType(WemediaConstants.WM_NEWS_SINGLE_IMAGE);
                images = material.stream().limit(1).collect(Collectors.toList());
            }else {
                //无图
                wmNews.setType(WemediaConstants.WM_NEWS_NONE_IMAGE);
            }

            //修改文章
            if(images != null && images.size() > 0){
                wmNews.setImages(org.apache.commons.lang3.StringUtils.join(images,","));
            }
            updateById(wmNews);
        }
        //不管封面是那种情况都要保存到数据库中
        if (images != null && images.size()>0){
            saveRelativeInfo(images,wmNews.getId(),WemediaConstants.WM_COVER_REFERENCE);
        }
    }

    /**
     * 处理文章图片内容与素材的关系
     * @param material
     * @param newsId
     */

    private void saveRelativeInfoForContent(List<String> material, Integer newsId) {
    saveRelativeInfo(material,newsId,WemediaConstants.CANCEL_COLLECT_MATERIAL);
    }

    @Resource
    private WmMaterialMapper wmMaterialMapper;

    /**
     * 保存文章图片与素材的关系到数据库中
     * @param material
     * @param newsId
     * @param type
     */
    private void saveRelativeInfo(List<String> material, Integer newsId, Short type) {
        if (material!= null && material.isEmpty()){
            //通过图片的URL查询素材的id
            List<WmMaterial> dbMaterials = wmMaterialMapper.selectList(Wrappers.<WmMaterial>lambdaQuery().in(WmMaterial::getUrl, material));

            //判断文章是否有效
            if(dbMaterials == null || dbMaterials.size() == 0){
                //手动抛出异常 第一个功能 能够提示调用者素材失效,第二个功能，进行数据回滚
                throw new CustomException(AppHttpCodeEnum.MATERIASL_REFERENCE_FALL);
            }

            if (material.size()!= dbMaterials.size()){
                //手动抛出异常 第一个功能 能够提示调用者素材失效,第二个功能，进行数据回滚
                throw new CustomException(AppHttpCodeEnum.MATERIASL_REFERENCE_FALL);
            }

            List<Integer> idList = dbMaterials.stream().map(WmMaterial::getId).collect(Collectors.toList());
            //批量保存
            wmNewsMaterialMapper.saveRelations(idList,newsId,type);
        }
    }

    /**
     * 提取文章内容信息
     * @param content
     * @return
     */
    private List<String> ectractUrlInfo(String content) {
        List<String> material = new ArrayList<>();

        List<Map> maps = JSON.parseArray(content, Map.class);
        for (Map map : maps) {
            if (map.get("type").equals("image")){
                String imgUrl = (String) map.get("value");
                material.add(imgUrl);
            }
        }
        return material;
    }

    /**
     * 保存或修改文章信息
     * @param wmNews
     */
    private void saveOrUpdateWmNews(WmNews wmNews) {
        //补全属性
        wmNews.setUserId(WmThreadLocalUtil.getUser().getId());
        wmNews.setCreatedTime(new Date());
        wmNews.setSubmitedTime(new Date());
        wmNews.setEnable((short)1);

        if (wmNews.getId() == null){
            save(wmNews);
        }else {
            //修改
            //删除文章图片与素材的关系
            wmNewsMaterialMapper.delete(Wrappers.<WmNewsMaterial>lambdaQuery().eq(WmNewsMaterial::getNewsId,wmNews.getId()));
            updateById(wmNews);
        }
    }
    @Resource
    private KafkaTemplate<String,String> kafkaTemplate;
    /**
     * 文章的上下架
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult downOrUp(WmNewsDto dto) {
        //1.检查参数
        if(dto.getId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.查询文章
        WmNews wmNews = getById(dto.getId());
        if(wmNews == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"文章不存在");
        }

        //3.判断文章是否已发布
        if(!wmNews.getStatus().equals(WmNews.Status.PUBLISHED.getCode())){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"当前文章不是发布状态，不能上下架");
        }

        //4.修改文章enable
        if(dto.getEnable() != null && dto.getEnable() > -1 && dto.getEnable() < 2){
            update(Wrappers.<WmNews>lambdaUpdate().set(WmNews::getEnable,dto.getEnable())
                    .eq(WmNews::getId,wmNews.getId()));

            if (wmNews.getArticleId() != null){
                //发售消息通知文章修改配置
                Map<String,Object> map = new HashMap<>();
                map.put("articleId",wmNews.getArticleId());
                map.put("enable",dto.getEnable());
                kafkaTemplate.send(WmNewsMessageConstants.WM_NEWS_UP_OR_DOWN_TOPIC, JSON.toJSONString(map));
            }

        }
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
