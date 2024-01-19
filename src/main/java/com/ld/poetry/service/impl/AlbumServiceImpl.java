package com.ld.poetry.service.impl;

import com.ld.poetry.entity.Album;
import com.ld.poetry.dao.AlbumMapper;
import com.ld.poetry.service.AlbumService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 文章表 服务实现类
 * </p>
 *
 * @author sara
 * @since 2022-12-31
 */
@Service
public class AlbumServiceImpl extends ServiceImpl<AlbumMapper, Album> implements AlbumService {

}
