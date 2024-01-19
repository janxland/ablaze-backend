package com.ld.poetry.service.impl;

import com.ld.poetry.entity.Diary;
import com.ld.poetry.dao.DiaryMapper;
import com.ld.poetry.service.DiaryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.entity.Label;
import com.ld.poetry.entity.Sort;
import com.ld.poetry.entity.User;
import com.ld.poetry.utils.*;
import com.ld.poetry.vo.ArticleVO;
import com.ld.poetry.vo.BaseRequestVO;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
/**
 * <p>
 * 文章表 服务实现类
 * </p>
 *
 * @author sara
 * @since 2023-01-01
 */
@Service
public class DiaryServiceImpl extends ServiceImpl<DiaryMapper, Diary> implements DiaryService {
    @Autowired
    private DiaryMapper diaryMapper;
    
    @Autowired
    private CommonQuery commonQuery;

    @Override
    public PoetryResult saveArticle(ArticleVO articleVO) {
        if (articleVO.getViewStatus() != null && !articleVO.getViewStatus() && !StringUtils.hasText(articleVO.getPassword())) {
            return PoetryResult.fail("请设置文章密码！");
        }
        Diary article = new Diary();
        if (StringUtils.hasText(articleVO.getArticleCover())) {
            article.setArticleItems(articleVO.getArticleCover());
        }
        if (StringUtils.hasText(articleVO.getArticleAddress())) {
            article.setArticleAddress(articleVO.getArticleAddress());
        }
        if (StringUtils.hasText(articleVO.getArticleDevice())) {
            article.setArticleDevice(articleVO.getArticleDevice());
        }
        if (StringUtils.hasText(articleVO.getArticleEmotion())) {
            article.setArticleEmotion(articleVO.getArticleEmotion());
        }
        if (articleVO.getViewStatus() != null && !articleVO.getViewStatus() && StringUtils.hasText(articleVO.getPassword())) {
            article.setPassword(articleVO.getPassword());
        }

        article.setViewStatus(articleVO.getViewStatus());
        article.setCommentStatus(articleVO.getCommentStatus());
        article.setRecommendStatus(articleVO.getRecommendStatus());
        article.setArticleTitle(articleVO.getArticleTitle());
        article.setArticleContent(articleVO.getArticleContent());
        article.setSortId(articleVO.getSortId());
        article.setLabelId(articleVO.getLabelId());
        article.setUserId(PoetryUtil.getUserId());
        save(article);

        List<Sort> sortInfo = commonQuery.getSortInfo();
        if (!CollectionUtils.isEmpty(sortInfo)) {
            PoetryCache.put(CommonConst.SORT_INFO, sortInfo);
        }
        return PoetryResult.success();
    }

    @Override
    public PoetryResult deleteArticle(Integer id) {
        Integer userId = PoetryUtil.getUserId();
        lambdaUpdate().eq(Diary::getId, id)
                .eq(Diary::getUserId, userId)
                .remove();
        List<Sort> sortInfo = commonQuery.getSortInfo();
        if (!CollectionUtils.isEmpty(sortInfo)) {
            PoetryCache.put(CommonConst.SORT_INFO, sortInfo);
        }
        return PoetryResult.success();
    }

    @Override
    public PoetryResult updateArticle(ArticleVO articleVO) {
        if (articleVO.getViewStatus() != null && !articleVO.getViewStatus() && !StringUtils.hasText(articleVO.getPassword())) {
            return PoetryResult.fail("请设置文章密码！");
        }

        Integer userId = PoetryUtil.getUserId();
        LambdaUpdateChainWrapper<Diary> updateChainWrapper = lambdaUpdate()
                .eq(Diary::getId, articleVO.getId())
                .eq(Diary::getUserId, userId)
                .set(Diary::getLabelId, articleVO.getLabelId())
                .set(Diary::getSortId, articleVO.getSortId())
                .set(Diary::getArticleTitle, articleVO.getArticleTitle())
                .set(Diary::getUpdateBy, PoetryUtil.getUsername())
                .set(Diary::getArticleContent, articleVO.getArticleContent());

        if (StringUtils.hasText(articleVO.getArticleCover())) {
            updateChainWrapper.set(Diary::getArticleItems, articleVO.getArticleCover());
        }
        if (articleVO.getCommentStatus() != null) {
            updateChainWrapper.set(Diary::getCommentStatus, articleVO.getCommentStatus());
        }

        if (articleVO.getRecommendStatus() != null) {
            updateChainWrapper.set(Diary::getRecommendStatus, articleVO.getRecommendStatus());
        }

        if (articleVO.getViewStatus() != null && !articleVO.getViewStatus() && StringUtils.hasText(articleVO.getPassword())) {
            updateChainWrapper.set(Diary::getPassword, articleVO.getPassword());
        }
        if (articleVO.getViewStatus() != null) {
            updateChainWrapper.set(Diary::getViewStatus, articleVO.getViewStatus());
        }
        updateChainWrapper.update();
        List<Sort> sortInfo = commonQuery.getSortInfo();
        if (!CollectionUtils.isEmpty(sortInfo)) {
            PoetryCache.put(CommonConst.SORT_INFO, sortInfo);
        }
        return PoetryResult.success();
    }

    @Override
    public PoetryResult<Page> listArticle(BaseRequestVO baseRequestVO) {
        LambdaQueryChainWrapper<Diary> lambdaQuery = lambdaQuery();
        lambdaQuery.eq(Diary::getViewStatus, PoetryEnum.STATUS_ENABLE.getCode());
        // .select(Article.class, a -> ((!a.getColumn().equals("article_content"))));
        if (StringUtils.hasText(baseRequestVO.getKeywords())) {
            lambdaQuery.like(Diary::getArticleTitle, baseRequestVO.getKeywords());
        }
        if (baseRequestVO.getRecommendStatus() != null && baseRequestVO.getRecommendStatus()) {
            lambdaQuery.eq(Diary::getRecommendStatus, PoetryEnum.STATUS_ENABLE.getCode());
        }

        if (baseRequestVO.getLabelId() != null) {
            lambdaQuery.eq(Diary::getLabelId, baseRequestVO.getLabelId());
        } else if (baseRequestVO.getSortId() != null) {
            lambdaQuery.eq(Diary::getSortId, baseRequestVO.getSortId());
        }

        lambdaQuery.orderByDesc(Diary::getCreateTime).page((Page)baseRequestVO);

        return PoetryResult.success(baseRequestVO);
    }

    @Override
    public PoetryResult<ArticleVO> getArticleById(Integer id, Boolean flag, String password) {
        LambdaQueryChainWrapper<Diary> lambdaQuery = lambdaQuery();
        lambdaQuery.eq(Diary::getId, id);
        if (flag) {
            lambdaQuery.eq(Diary::getViewStatus, PoetryEnum.STATUS_ENABLE.getCode());
        } else {
            if (!StringUtils.hasText(password)) {
                return PoetryResult.fail("请输入文章密码！");
            }
            lambdaQuery.eq(Diary::getPassword, password);
        }
        Diary article = lambdaQuery.one();
        if (article == null) {
            return PoetryResult.success();
        }
        article.setPassword(null);
        diaryMapper.updateViewCount(id);
        ArticleVO articleVO = buildArticleVO(article, false);
        return PoetryResult.success(articleVO);
    }

    @Override
    public PoetryResult<Page> listAdminArticle(BaseRequestVO baseRequestVO, Boolean isBoss) {
        LambdaQueryChainWrapper<Diary> lambdaQuery = lambdaQuery();
        lambdaQuery.select(Diary.class, a -> !a.getColumn().equals("article_content"));
        if (!isBoss) {
            lambdaQuery.eq(Diary::getUserId, PoetryUtil.getUserId());
        } else {
            if (baseRequestVO.getUserId() != null) {
                lambdaQuery.eq(Diary::getUserId, baseRequestVO.getUserId());
            }
        }
        if (StringUtils.hasText(baseRequestVO.getKeywords())) {
            lambdaQuery.like(Diary::getArticleTitle, baseRequestVO.getKeywords());
        }
        if (baseRequestVO.getRecommendStatus() != null && baseRequestVO.getRecommendStatus()) {
            lambdaQuery.eq(Diary::getRecommendStatus, PoetryEnum.STATUS_ENABLE.getCode());
        }

        if (baseRequestVO.getLabelId() != null) {
            lambdaQuery.eq(Diary::getLabelId, baseRequestVO.getLabelId());
        }

        if (baseRequestVO.getSortId() != null) {
            lambdaQuery.eq(Diary::getSortId, baseRequestVO.getSortId());
        }

        lambdaQuery.orderByDesc(Diary::getCreateTime).page((Page)baseRequestVO);

        return PoetryResult.success(baseRequestVO);
    }

    @Override
    public PoetryResult<ArticleVO> getArticleByIdForUser(Integer id) {
        LambdaQueryChainWrapper<Diary> lambdaQuery = lambdaQuery();
        lambdaQuery.eq(Diary::getId, id).eq(Diary::getUserId, PoetryUtil.getUserId());
        Diary article = lambdaQuery.one();
        if (article == null) {
            return PoetryResult.fail("文章不存在！");
        }
        ArticleVO articleVO = new ArticleVO();
        BeanUtils.copyProperties(article, articleVO);
        return PoetryResult.success(articleVO);
    }

    private ArticleVO buildArticleVO(Diary article, Boolean isAdmin) {
        ArticleVO articleVO = new ArticleVO();
        BeanUtils.copyProperties(article, articleVO);
        if (!isAdmin) {
            if (!StringUtils.hasText(articleVO.getArticleCover())) {
                articleVO.setArticleCover(PoetryUtil.getRandomCover(articleVO.getId().toString()));
            }
        }

        User user = commonQuery.getUser(articleVO.getUserId());
        if (user != null && StringUtils.hasText(user.getUsername())) {
            articleVO.setUsername(user.getUsername());
        } else if (!isAdmin) {
            articleVO.setUsername(PoetryUtil.getRandomName(articleVO.getUserId().toString()));
        }
        if (articleVO.getCommentStatus()) {
            articleVO.setCommentCount(commonQuery.getCommentCount(articleVO.getId()));
        } else {
            articleVO.setCommentCount(0);
        }

        List<Sort> sortInfo = (List<Sort>) PoetryCache.get(CommonConst.SORT_INFO);
        if (sortInfo != null) {
            for (Sort s : sortInfo) {
                if (s.getId().intValue() == articleVO.getSortId().intValue()) {
                    Sort sort = new Sort();
                    BeanUtils.copyProperties(s, sort);
                    sort.setLabels(null);
                    articleVO.setSort(sort);
                    if (!CollectionUtils.isEmpty(s.getLabels())) {
                        for (int j = 0; j < s.getLabels().size(); j++) {
                            Label l = s.getLabels().get(j);
                            if (l.getId().intValue() == articleVO.getLabelId().intValue()) {
                                Label label = new Label();
                                BeanUtils.copyProperties(l, label);
                                articleVO.setLabel(label);
                                break;
                            }
                        }
                    }
                    break;
                }
            }
        }
        return articleVO;
    }
}
