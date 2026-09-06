package com.ld.poetry.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 文章表 前端控制器
 * </p>
 *
 * @author sara
 * @since 2022-12-31
 */
@RestController
@Tag(name = "Album", description = "相册管理")
@RequestMapping("/album")
public class AlbumController {

}

