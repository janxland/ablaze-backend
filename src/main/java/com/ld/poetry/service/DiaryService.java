package com.ld.poetry.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.vo.ArticleVO;
import com.ld.poetry.vo.BaseRequestVO;
import com.ld.poetry.entity.Diary;

/**
 * <p>
 * 文章表 服务类
 * </p>
 *
 * @author sara
 * @since 2023-01-01
 */
public interface DiaryService extends IService<Diary> {

    PoetryResult saveArticle(ArticleVO articleVO);

    PoetryResult deleteArticle(Integer id);

    PoetryResult updateArticle(ArticleVO articleVO);

    PoetryResult<Page> listArticle(BaseRequestVO baseRequestVO);

    PoetryResult<ArticleVO> getArticleById(Integer id, Boolean flag, String password);

    PoetryResult<Page> listAdminArticle(BaseRequestVO baseRequestVO, Boolean isBoss);

    PoetryResult<ArticleVO> getArticleByIdForUser(Integer id);
}
