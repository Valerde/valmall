package com.sovava.product.service.impl;

//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sovava.product.service.CategoryBrandRelationService;
import com.sovava.product.vo.Catelog2Vo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sovava.common.utils.PageUtils;
import com.sovava.common.utils.Query;

import com.sovava.product.dao.CategoryDao;
import com.sovava.product.entity.CategoryEntity;
import com.sovava.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;


@Service("categoryService")
@Slf4j
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {


    @Autowired
    private StringRedisTemplate redisTemplate;

    @Resource
    private CategoryBrandRelationService categoryBrandRelationService;

    @Resource
    private RedissonClient redisson;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //查出所有分类
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);
        //装成树形结构
        // 1.找到所有一级分类
        List<CategoryEntity> firstCategory = categoryEntities.stream()
                .filter(categoryEntity -> categoryEntity.getParentCid() == 0)
                .map((menu -> {
                    menu.setChildren(getChildren(menu, categoryEntities));
                    return menu;
                })).sorted((item1, item2) -> {
                    return ((item1.getSort() == null ? 0 : item1.getSort()) -
                            (item2.getSort() == null ? 0 : item2.getSort()));
                }).collect(Collectors.toList());


        return firstCategory;
    }

    @Override
    public void removeMenusByIds(List<Long> ids) {

        //TODO:检查当前要删除的菜单是否被别的地方引用

        //逻辑删除
        baseMapper.deleteBatchIds(ids);
    }

    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> path = new ArrayList<>();

        CategoryEntity categoryEntity = baseMapper.selectById(catelogId);
        Long secondPath = categoryEntity.getParentCid();
        CategoryEntity categoryEntity1 = baseMapper.selectById(secondPath);
        Long firstPath = categoryEntity1.getParentCid();
        path.add(firstPath);
        path.add(secondPath);
        path.add(catelogId);
        log.debug("查询的路径为{}", path.toString());
        return path.toArray(new Long[0]);
    }

    /**
     * 级联更新所有关联的数据
     * 同时进行多种缓存操作
     * 存储同一类型的数据，可以存储为一个分区 ， 当更新时，可以将分区有关这一类型的数据全部删除
     *
     * @param category
     */
    @Override
    @Transactional
//    @CacheEvict(value = {"category"}, key = "'Level1Categories'")
//    @Caching(evict = {
//            @CacheEvict(value = {"category"}, key = "'catalog'"),
//            @CacheEvict(value = {"category"}, key = "'Level1Categories'")
//    })
    @CacheEvict(value = "category", allEntries = true) // 删除指定分区下的所有数据
    public void updateCascat(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }

    @Override
    //每一个缓存的数据都需要指定放在那个名字的缓存【缓存的分区，按照业务类型来分】
    @Cacheable(value = {"category"}, key = "'Level1Categories'")
    //当前结果的返回值需要缓存 ， 如果缓存中有 ， 那么直接返回缓存中的数据，如果缓存中没有，调用方法，将方法的结果放入缓存
    public List<CategoryEntity> findLevel1Categories() {
        LambdaQueryWrapper<CategoryEntity> lqw = new LambdaQueryWrapper<>();
        lqw.eq(CategoryEntity::getParentCid, 0);
        List<CategoryEntity> categoryEntities = this.list(lqw);
        return categoryEntities;
//        return null;
    }

    /**
     * springboot2.0以后默认使用lettuce作为操作redis的客户端，，他使用netty进行通信
     * <p>
     * 空结果返回 ，： 解决缓存穿透
     * 设置过期时间加随机值，解决缓存雪崩
     * 加锁： 解决缓存击穿
     *
     * @return
     */
//    @Override
//    public Map<String, List<Catelog2Vo>> getCatalogJSON() {
//        //给redis中放入json字符串，拿出的json字符串 ， 还是逆转为能用的对象类型【序列化与反序列化】
//        //1. 加入缓存逻辑， 环村中存放的是json字符串 ， 便于跨平台跨语言使用
//        String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
//        if (StringUtils.isEmpty(catalogJSON)) {
//            log.error("缓存不命中，查询数据库");
//            //2. 缓存中没有，查询数据库
//            Map<String, List<Catelog2Vo>> catalogJSONFromDB = getDataFromdb();
//
//            return catalogJSONFromDB;
//        }
//        log.error("缓存命中，不查询数据库");
//        //转为指定的对象
//        Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
//        });
//        return result;
//
//    }
    @Override
    @Cacheable(value = {"category"}, key = "'catalog'")
    public Map<String, List<Catelog2Vo>> getCatalogJSON() {
        Map<String, List<Catelog2Vo>> dataFromDBRedisson = getDataFromDBRedisson();
        return dataFromDBRedisson;
    }

    /**
     * 缓存里的数据如何和数据库保持一致 ， 缓存一致性问题
     * 解决： 双写模式  失效模式（选择）
     * 缓存+过期时间可以解决大部分业务的缓存一致性问题 ， 可以加一个读写锁（选择）
     * 缓存里面放读多写少的数据，一致性和即时性要求低的数据
     * <p>
     * 我们的解决方案：
     * 缓存的所有数据都有过期时间，数据过期下一次查询更新触发主动更新
     * 读写数据的时候，加上分布式读写锁
     *
     * @return
     */
    public Map<String, List<Catelog2Vo>> getDataFromDBRedisson() {
        //注意锁的名字 ， 锁的力度，越细越快
        RLock lock = redisson.getLock("catalogJson-lock");
        lock.lock();
        log.debug("获取分布式锁成功");
        Map<String, List<Catelog2Vo>> dataFromDB = null;
        try {
            dataFromDB = getDataFromdb();
        } finally {
            lock.unlock();
        }

        return dataFromDB;
    }

    /**
     * 简单的分布式锁
     *
     * @return
     */
    public Map<String, List<Catelog2Vo>> getCatalogJSONFromDBWithRedisLock() {
        //1.占分布式锁，去redis占坑
        String uuid = UUID.randomUUID().toString();//误删了了别人的锁
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS);
        if (lock) {
            //加锁成功
            //设置过期时间必须和加锁是一条原子命令
//            redisTemplate.expire("lock",30,TimeUnit.SECONDS);
            Map<String, List<Catelog2Vo>> dataFromDB = getDataFromDB();
            //这两步也必须是原子操作,Lua脚本
//            if (redisTemplate.opsForValue().get("lock").equals(uuid))
//                redisTemplate.delete("lock");
            String script = "if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
                    "then\n" +
                    "    return redis.call(\"del\",KEYS[1])\n" +
                    "else\n" +
                    "    return 0\n" +
                    "end";
            Long lock1 = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList("lock"), uuid);

            return dataFromDB;
        } else {
            //加锁失败，重试 , 自旋的方式
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }
            return getCatalogJSONFromDBWithRedisLock();
        }
    }

    public Map<String, List<Catelog2Vo>> getCatalogJSONFromDBWithLocalLock() {
        Map<String, List<Catelog2Vo>> dataFromDB = null;
        synchronized (this) {
            dataFromDB = getDataFromDB();
        }
        return dataFromDB;
    }

    //    @Cacheable(value = "category", key = "#root.methodName")
    public Map<String, List<Catelog2Vo>> getDataFromdb() {

        List<CategoryEntity> selectList = this.baseMapper.selectList(null);
        //-------优化---------------------

        //查出所有一级分类
        List<CategoryEntity> level1Categories = getParentCid(selectList, 0L);
        //封装数据
        Map<String, List<Catelog2Vo>> parant_cid = level1Categories.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //每一个的一级分类 ， 查到这个以及分类的二级分类
            LambdaQueryWrapper<CategoryEntity> lqw = new LambdaQueryWrapper<>();
            lqw.eq(CategoryEntity::getParentCid, v.getCatId());
            //当前一级id的二级分类
            List<CategoryEntity> categoryEntities = getParentCid(selectList, v.getCatId());

            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map(l2 -> {

                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    //赵二级分类的三级分类
                    List<CategoryEntity> level3Cates = getParentCid(selectList, l2.getCatId());
                    if (level3Cates != null) {
                        List<Catelog2Vo.Catalog3Vo> catalog3Vos = level3Cates.stream().map(l3 -> {
                            Catelog2Vo.Catalog3Vo catalog3Vo = new Catelog2Vo.Catalog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return catalog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(catalog3Vos);
                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));
        return parant_cid;
    }

    public Map<String, List<Catelog2Vo>> getDataFromDB() {
        String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
        if (!StringUtils.isEmpty(catalogJSON)) {
            Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
            });
            return result;
        }
        log.error("查询了数据库");
        List<CategoryEntity> selectList = this.baseMapper.selectList(null);
        //-------优化---------------------

        //查出所有一级分类
        List<CategoryEntity> level1Categories = getParentCid(selectList, 0L);
        //封装数据
        Map<String, List<Catelog2Vo>> parant_cid = level1Categories.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //每一个的一级分类 ， 查到这个以及分类的二级分类
            LambdaQueryWrapper<CategoryEntity> lqw = new LambdaQueryWrapper<>();
            lqw.eq(CategoryEntity::getParentCid, v.getCatId());
            //当前一级id的二级分类
            List<CategoryEntity> categoryEntities = getParentCid(selectList, v.getCatId());

            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map(l2 -> {

                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    //赵二级分类的三级分类
                    List<CategoryEntity> level3Cates = getParentCid(selectList, l2.getCatId());
                    if (level3Cates != null) {
                        List<Catelog2Vo.Catalog3Vo> catalog3Vos = level3Cates.stream().map(l3 -> {
                            Catelog2Vo.Catalog3Vo catalog3Vo = new Catelog2Vo.Catalog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return catalog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(catalog3Vos);
                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));
        //3. 查到的数据在放入缓存 ，将对象转为json放入缓存中
        String s = JSON.toJSONString(parant_cid);
        redisTemplate.opsForValue().set("catalogJSON", s, 1, TimeUnit.DAYS);
        return parant_cid;
    }

    private List<CategoryEntity> getParentCid(List<CategoryEntity> selectList, Long parentCid) {
        return selectList.stream().filter(item -> item.getParentCid().equals(parentCid)).collect(Collectors.toList());
    }

    private List<CategoryEntity> getChildren(CategoryEntity entity, List<CategoryEntity> all) {
        List<CategoryEntity> children = new ArrayList<>();
        children = all.stream().filter((item) -> {
            return item.getParentCid().equals(entity.getCatId());
        }).map(item -> {
            //找到子菜单
            item.setChildren(getChildren(item, all));
            return item;
        }).sorted((menu1, menu2) -> {
            return ((menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort()));
        }).collect(Collectors.toList());
        return children;
    }

}