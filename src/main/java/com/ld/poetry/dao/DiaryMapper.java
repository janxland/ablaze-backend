package com.ld.poetry.dao;

import com.ld.poetry.entity.Diary;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 * 文章表 Mapper 接口
 * </p>
 *
 * @author sara
 * @since 2023-01-01
 */
public interface DiaryMapper extends BaseMapper<Diary> {
    @Update("update diary set view_count=view_count+1 where id=#{id}")
    int updateViewCount(@Param("id") Integer id);
}
