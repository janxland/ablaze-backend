package com.ld.poetry.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.dao.ArticleMapper;
import com.ld.poetry.dao.CommentMapper;
import com.ld.poetry.entity.Article;
import com.ld.poetry.entity.Comment;
import com.ld.poetry.entity.UserArticleAuth;
import com.ld.poetry.service.CommentService;
import com.ld.poetry.service.UserArticleAuthService;
import com.ld.poetry.utils.*;
import com.ld.poetry.utils.VoBuilderUtil;
import com.ld.poetry.vo.BaseRequestVO;
import com.ld.poetry.vo.CommentVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 文章评论表 服务实现类
 * </p>
 *
 * @author sara
 * @since 2021-08-13
 */
@Service
@Slf4j
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Autowired
    private UserArticleAuthService userArticleAuthService;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private CommonQuery commonQuery;

    @Autowired
    private MailSendUtil mailSendUtil;

    @Override
    public PoetryResult saveComment(CommentVO commentVO) {
        LambdaQueryChainWrapper<Article> articleWrapper = new LambdaQueryChainWrapper<>(articleMapper);
        Article one = articleWrapper.eq(Article::getId, commentVO.getSource()).select(Article::getUserId, Article::getArticleTitle, Article::getCommentStatus).one();

        if (one != null && !one.getCommentStatus()) {
            return PoetryResult.fail("评论功能已关闭！");
        }

        if (one == null && commentVO.getSource() != CommonConst.TREE_HOLE_COMMENT_SOURCE) {
            return PoetryResult.fail(CodeMsg.PARAMETER_ERROR);
        }

        Comment comment = new Comment();
        comment.setSource(commentVO.getSource());
        comment.setCommentContent(commentVO.getCommentContent());
        comment.setParentCommentId(commentVO.getParentCommentId());
        comment.setFloorCommentId(commentVO.getFloorCommentId());
        comment.setParentUserId(commentVO.getParentUserId());
        comment.setUserId(PoetryUtil.getUserId());
        if (StringUtils.hasText(commentVO.getCommentInfo())) {
            comment.setCommentInfo(commentVO.getCommentInfo());
        }
        save(comment);

        try {
            mailSendUtil.sendCommentMail(commentVO, one, this);
        } catch (Exception e) {
            log.error("发送评论邮件失败：", e);
        }

        try {
            UserArticleAuth auth = new UserArticleAuth();
            auth.setUserId(comment.getUserId());
            auth.setArticleId(comment.getSource());
            auth.setReply(1);
            userArticleAuthService.createOrUpdate(auth);
        } catch (Exception e) {
            log.error("创建或更新用户文章权限失败", e);
        }

        return PoetryResult.success();
    }

    @Override
    public PoetryResult deleteComment(Integer id) {
        // 扁平化处理：提前返回，减少嵌套
        Integer userId = PoetryUtil.getUserId();
        if (userId == null) {
            return PoetryResult.fail("用户未登录");
        }
        
        lambdaUpdate().eq(Comment::getId, id)
                .eq(Comment::getUserId, userId)
                .remove();
        return PoetryResult.success();
    }

    @Override
    public PoetryResult<BaseRequestVO> listComment(BaseRequestVO baseRequestVO) {
        // 扁平化处理：参数校验提前返回
        if (baseRequestVO.getSource() == null) {
            return PoetryResult.fail(CodeMsg.PARAMETER_ERROR);
        }
        
        // 扁平化处理：评论状态检查提前返回
        if (!checkCommentStatus(baseRequestVO.getSource())) {
            return PoetryResult.fail("评论功能已关闭！");
        }

        // 扁平化处理：根据是否有 floorCommentId 分别处理
        if (baseRequestVO.getFloorCommentId() == null) {
            return listTopLevelComments(baseRequestVO);
        } else {
            return listChildComments(baseRequestVO);
        }
    }
    
    /**
     * 检查评论状态（扁平化、低耦合）
     */
    private boolean checkCommentStatus(Integer source) {
        LambdaQueryChainWrapper<Article> articleWrapper = new LambdaQueryChainWrapper<>(articleMapper);
        Article article = articleWrapper.eq(Article::getId, source)
                .select(Article::getCommentStatus)
                .one();
        return article == null || article.getCommentStatus();
    }
    
    /**
     * 查询一级评论（扁平化、低耦合）
     */
    private PoetryResult<BaseRequestVO> listTopLevelComments(BaseRequestVO baseRequestVO) {
        // 查询一级评论
        lambdaQuery()
                .eq(Comment::getSource, baseRequestVO.getSource())
                .eq(Comment::getParentCommentId, CommonConst.FIRST_COMMENT)
                .orderByAsc(Comment::getCreateTime)
                .page((Page) baseRequestVO);
        
        List<Comment> comments = baseRequestVO.getRecords();
        if (CollectionUtils.isEmpty(comments)) {
            return PoetryResult.success(baseRequestVO);
        }
        
        // 扁平化处理：批量查询子评论
        Map<Integer, List<Comment>> childCommentsMap = batchQueryChildComments(baseRequestVO.getSource(), comments);
        
        // 扁平化处理：构建评论VO
        List<CommentVO> commentVOs = buildCommentVOsWithChildren(comments, childCommentsMap);
        baseRequestVO.setRecords(commentVOs);
        
        return PoetryResult.success(baseRequestVO);
    }
    
    /**
     * 批量查询子评论（扁平化、低耦合）
     */
    private Map<Integer, List<Comment>> batchQueryChildComments(Integer source, List<Comment> parentComments) {
        List<Integer> parentIds = parentComments.stream()
                .map(Comment::getId)
                .collect(Collectors.toList());
        
        if (CollectionUtils.isEmpty(parentIds)) {
            return new HashMap<>();
        }
        
        // 批量查询所有子评论
        List<Comment> allChildComments = lambdaQuery()
                .eq(Comment::getSource, source)
                .in(Comment::getFloorCommentId, parentIds)
                .orderByAsc(Comment::getCreateTime)
                .list();
        
        // 按父评论ID分组，并限制每个父评论最多5条子评论
        return allChildComments.stream()
                .collect(Collectors.groupingBy(Comment::getFloorCommentId))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .limit(5)
                                .collect(Collectors.toList())
                ));
    }
    
    /**
     * 构建评论VO（带子评论）（扁平化、低耦合）
     */
    private List<CommentVO> buildCommentVOsWithChildren(List<Comment> comments, Map<Integer, List<Comment>> childCommentsMap) {
        return comments.stream().map(parentComment -> {
            CommentVO commentVO = VoBuilderUtil.buildCommentVO(parentComment, commonQuery);
            
            List<Comment> childComments = childCommentsMap.getOrDefault(parentComment.getId(), Collections.emptyList());
            if (!CollectionUtils.isEmpty(childComments)) {
                List<CommentVO> childCommentVOs = childComments.stream()
                        .map(childComment -> VoBuilderUtil.buildCommentVO(childComment, commonQuery))
                        .collect(Collectors.toList());
                
                Page<CommentVO> childPage = new Page<>(1, 5);
                childPage.setRecords(childCommentVOs);
                childPage.setTotal(childComments.size());
                commentVO.setChildComments(childPage);
            }
            
            return commentVO;
        }).collect(Collectors.toList());
    }
    
    /**
     * 查询子评论（扁平化、低耦合）
     */
    private PoetryResult<BaseRequestVO> listChildComments(BaseRequestVO baseRequestVO) {
        IPage<Comment> result = page(
                new Page<>(baseRequestVO.getCurrent(), baseRequestVO.getSize()),
                lambdaQuery()
                        .eq(Comment::getSource, baseRequestVO.getSource())
                        .eq(Comment::getFloorCommentId, baseRequestVO.getFloorCommentId())
                        .orderByAsc(Comment::getCreateTime)
                        .getWrapper()
        );
        
        List<Comment> childComments = result.getRecords();
        if (CollectionUtils.isEmpty(childComments)) {
            return PoetryResult.success(baseRequestVO);
        }
        
        List<CommentVO> childCommentVOs = childComments.stream()
                .map(comment -> VoBuilderUtil.buildCommentVO(comment, commonQuery))
                .collect(Collectors.toList());
        baseRequestVO.setRecords(childCommentVOs);
        
        return PoetryResult.success(baseRequestVO);
    }

    @Override
    public PoetryResult<Page> listAdminComment(BaseRequestVO baseRequestVO, Boolean isBoss) {
        // 扁平化处理：根据 isBoss 分别处理
        if (isBoss) {
            return listAdminCommentForBoss(baseRequestVO);
        } else {
            return listAdminCommentForUser(baseRequestVO);
        }
    }
    
    /**
     * 管理员查询评论（扁平化、低耦合）
     */
    private PoetryResult<Page> listAdminCommentForBoss(BaseRequestVO baseRequestVO) {
        LambdaQueryChainWrapper<Comment> wrapper = lambdaQuery();
        
        if (baseRequestVO.getSource() != null) {
            wrapper.eq(Comment::getSource, baseRequestVO.getSource());
        }
        
        IPage<Comment> result = page(
                new Page<>(baseRequestVO.getCurrent(), baseRequestVO.getSize()),
                wrapper.getWrapper()
        );
        wrapper.orderByDesc(Comment::getCreateTime).page(result);
        
        return PoetryResult.success(baseRequestVO);
    }
    
    /**
     * 普通用户查询评论（扁平化、低耦合）
     */
    private PoetryResult<Page> listAdminCommentForUser(BaseRequestVO baseRequestVO) {
        List<Integer> userArticleIds = commonQuery.getUserArticleIds(PoetryUtil.getUserId());
        
        // 扁平化处理：权限检查提前返回
        if (!hasCommentPermission(userArticleIds, baseRequestVO.getSource())) {
            baseRequestVO.setTotal(0);
            baseRequestVO.setRecords(new ArrayList<>());
            return PoetryResult.success(baseRequestVO);
        }
        
        LambdaQueryChainWrapper<Comment> wrapper = lambdaQuery();
        if (baseRequestVO.getSource() != null) {
            wrapper.eq(Comment::getSource, baseRequestVO.getSource());
        } else {
            wrapper.in(Comment::getSource, userArticleIds);
        }
        
        IPage<Comment> result = page(
                new Page<>(baseRequestVO.getCurrent(), baseRequestVO.getSize()),
                wrapper.getWrapper()
        );
        wrapper.orderByDesc(Comment::getCreateTime).page(result);
        
        return PoetryResult.success(baseRequestVO);
    }
    
    /**
     * 检查评论权限（扁平化、低耦合）
     */
    private boolean hasCommentPermission(List<Integer> userArticleIds, Integer source) {
        if (CollectionUtils.isEmpty(userArticleIds)) {
            return false;
        }
        
        if (source != null && !userArticleIds.contains(source)) {
            return false;
        }
        
        return true;
    }
}
