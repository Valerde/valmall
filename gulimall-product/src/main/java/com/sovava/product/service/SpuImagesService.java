package com.sovava.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sovava.common.utils.PageUtils;
import com.sovava.product.entity.SpuImagesEntity;

import java.util.List;
import java.util.Map;

/**
 * spu图片
 *
 * @author ykn
 * @email 602533622@qq.com
 * @date 2022-10-22 19:02:36
 */
public interface SpuImagesService extends IService<SpuImagesEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveImages(Long id, List<String> images);
}

