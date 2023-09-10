package com.heima.wemedia.service;

import java.util.Date;

public interface WmNewsTaskService {
    /**
     * 添加任务到延迟队列
     * @param id 文章的id
     * @param publishTime 发布的时间 可以作为任务的执行时间
     */
    public void addNewsToTack(Integer id, Date publishTime);

    /**
     * 消费任务，审核文章
     */
    public void scanNewsByTask();
}
