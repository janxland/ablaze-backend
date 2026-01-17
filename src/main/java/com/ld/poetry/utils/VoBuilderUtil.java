package com.ld.poetry.utils;

import com.ld.poetry.entity.Label;
import com.ld.poetry.entity.Sort;
import com.ld.poetry.entity.User;
import com.ld.poetry.vo.ArticleVO;
import com.ld.poetry.vo.CommentVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * VO 构建工具类
 * 职责明确：专门负责 VO 对象的构建，减少代码冗余
 * 扁平化：避免深层嵌套
 * 低耦合：不依赖具体 Service
 */
@Slf4j
public class VoBuilderUtil {

    /**
     * 构建 ArticleVO（扁平化、低耦合）
     * 
     * @param article 文章实体
     * @param isAdmin 是否管理员
     * @param userId 用户ID
     * @param commonQuery 通用查询工具
     * @return ArticleVO
     */
    public static ArticleVO buildArticleVO(Object article, Boolean isAdmin, Integer userId, CommonQuery commonQuery) {
        ArticleVO articleVO = new ArticleVO();
        BeanUtils.copyProperties(article, articleVO);
        
        // 扁平化处理：分别处理各个属性
        setArticleCover(articleVO, isAdmin);
        setArticleUsername(articleVO, isAdmin, userId, commonQuery);
        setArticleCommentCount(articleVO, commonQuery);
        setArticleSortAndLabel(articleVO);
        
        return articleVO;
    }

    /**
     * 设置文章封面（扁平化处理）
     */
    private static void setArticleCover(ArticleVO articleVO, Boolean isAdmin) {
        if (!isAdmin && !StringUtils.hasText(articleVO.getArticleCover())) {
            articleVO.setArticleCover(PoetryUtil.getRandomCover(articleVO.getId().toString()));
        }
    }

    /**
     * 设置文章用户名（扁平化处理）
     */
    private static void setArticleUsername(ArticleVO articleVO, Boolean isAdmin, Integer userId, CommonQuery commonQuery) {
        if (articleVO.getUserId() == null) {
            return;
        }
        
        User user = commonQuery.getUser(articleVO.getUserId());
        if (user != null && StringUtils.hasText(user.getUsername())) {
            articleVO.setUsername(user.getUsername());
        } else if (!isAdmin) {
            articleVO.setUsername(PoetryUtil.getRandomName(articleVO.getUserId().toString()));
        }
    }

    /**
     * 设置评论数量（扁平化处理）
     */
    private static void setArticleCommentCount(ArticleVO articleVO, CommonQuery commonQuery) {
        if (articleVO.getCommentStatus() != null && articleVO.getCommentStatus()) {
            articleVO.setCommentCount(commonQuery.getCommentCount(articleVO.getId()));
        } else {
            articleVO.setCommentCount(0);
        }
    }

    /**
     * 设置分类和标签（扁平化处理，减少嵌套）
     */
    private static void setArticleSortAndLabel(ArticleVO articleVO) {
        if (articleVO.getSortId() == null) {
            return;
        }
        
        @SuppressWarnings("unchecked")
        List<Sort> sortInfo = (List<Sort>) PoetryCache.get(CommonConst.SORT_INFO);
        if (CollectionUtils.isEmpty(sortInfo)) {
            return;
        }
        
        // 扁平化：使用 stream 替代嵌套循环
        Sort matchedSort = sortInfo.stream()
                .filter(s -> s.getId().intValue() == articleVO.getSortId().intValue())
                .findFirst()
                .orElse(null);
        
        if (matchedSort == null) {
            return;
        }
        
        // 设置分类
        Sort sort = new Sort();
        BeanUtils.copyProperties(matchedSort, sort);
        sort.setLabels(null);
        articleVO.setSort(sort);
        
        // 设置标签（扁平化处理）
        if (articleVO.getLabelId() != null && !CollectionUtils.isEmpty(matchedSort.getLabels())) {
            Label matchedLabel = matchedSort.getLabels().stream()
                    .filter(l -> l.getId().intValue() == articleVO.getLabelId().intValue())
                    .findFirst()
                    .orElse(null);
            
            if (matchedLabel != null) {
                Label label = new Label();
                BeanUtils.copyProperties(matchedLabel, label);
                articleVO.setLabel(label);
            }
        }
    }

    /**
     * 构建 CommentVO（扁平化、低耦合）
     */
    public static CommentVO buildCommentVO(Object comment, CommonQuery commonQuery) {
        CommentVO commentVO = new CommentVO();
        BeanUtils.copyProperties(comment, commentVO);
        
        // 扁平化处理：分别设置各个属性
        setCommentUserInfo(commentVO, commonQuery);
        setCommentParentUserInfo(commentVO, commonQuery);
        
        return commentVO;
    }

    /**
     * 设置评论用户信息（扁平化处理）
     */
    private static void setCommentUserInfo(CommentVO commentVO, CommonQuery commonQuery) {
        if (commentVO.getUserId() == null) {
            return;
        }
        
        User user = commonQuery.getUser(commentVO.getUserId());
        if (user != null) {
            commentVO.setAvatar(user.getAvatar());
            commentVO.setUsername(user.getUsername());
        }
        
        if (!StringUtils.hasText(commentVO.getUsername())) {
            commentVO.setUsername(PoetryUtil.getRandomName(commentVO.getUserId().toString()));
        }
    }

    /**
     * 设置父评论用户信息（扁平化处理）
     */
    private static void setCommentParentUserInfo(CommentVO commentVO, CommonQuery commonQuery) {
        if (commentVO.getParentUserId() == null) {
            return;
        }
        
        User parentUser = commonQuery.getUser(commentVO.getParentUserId());
        if (parentUser != null) {
            commentVO.setParentUsername(parentUser.getUsername());
        }
        
        if (!StringUtils.hasText(commentVO.getParentUsername())) {
            commentVO.setParentUsername(PoetryUtil.getRandomName(commentVO.getParentUserId().toString()));
        }
    }
}
