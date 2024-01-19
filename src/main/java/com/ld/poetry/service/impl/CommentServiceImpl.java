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
import com.ld.poetry.entity.User;
import com.ld.poetry.service.CommentService;
import com.ld.poetry.utils.*;
import com.ld.poetry.vo.BaseRequestVO;
import com.ld.poetry.vo.CommentVO;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
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
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

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

        return PoetryResult.success();
    }

    @Override
    public PoetryResult deleteComment(Integer id) {
        Integer userId = PoetryUtil.getUserId();
        lambdaUpdate().eq(Comment::getId, id)
                .eq(Comment::getUserId, userId)
                .remove();
        return PoetryResult.success();
    }

    @Override
    public PoetryResult<BaseRequestVO> listComment(BaseRequestVO baseRequestVO) {
        if (baseRequestVO.getSource() == null) {
            return PoetryResult.fail(CodeMsg.PARAMETER_ERROR);
        }
        LambdaQueryChainWrapper<Article> articleWrapper = new LambdaQueryChainWrapper<>(articleMapper);
        Article one = articleWrapper.eq(Article::getId, baseRequestVO.getSource()).select(Article::getCommentStatus).one();

        if (one != null && !one.getCommentStatus()) {
            return PoetryResult.fail("评论功能已关闭！");
        }

        if (baseRequestVO.getFloorCommentId() == null) {
            lambdaQuery().eq(Comment::getSource, baseRequestVO.getSource()).eq(Comment::getParentCommentId, CommonConst.FIRST_COMMENT).orderByAsc(Comment::getCreateTime).page((Page)baseRequestVO);
            List<Comment> comments = baseRequestVO.getRecords();
            if (CollectionUtils.isEmpty(comments)) {
                return PoetryResult.success(baseRequestVO);
            }
            List<CommentVO> commentVOs = comments.stream().map(c -> {
                CommentVO commentVO = buildCommentVO(c);
                Page page = new Page(1, 5);
                lambdaQuery().eq(Comment::getSource, baseRequestVO.getSource()).eq(Comment::getFloorCommentId, c.getId()).orderByAsc(Comment::getCreateTime).page(page);
                List<Comment> childComments = page.getRecords();
                if (childComments != null) {
                    List<CommentVO> ccVO = childComments.stream().map(cc -> buildCommentVO(cc)).collect(Collectors.toList());
                    page.setRecords(ccVO);
                }
                commentVO.setChildComments(page);
                return commentVO;
            }).collect(Collectors.toList());
            baseRequestVO.setRecords(commentVOs);
        } else {
                IPage result = page(new Page<>(baseRequestVO.getCurrent(),baseRequestVO.getSize()),lambdaQuery().getWrapper());
            lambdaQuery().eq(Comment::getSource, baseRequestVO.getSource()).eq(Comment::getFloorCommentId, baseRequestVO.getFloorCommentId()).orderByAsc(Comment::getCreateTime).page(result);
            List<Comment> childComments = baseRequestVO.getRecords();
            if (CollectionUtils.isEmpty(childComments)) {
                return PoetryResult.success(baseRequestVO);
            }
            List<CommentVO> ccVO = childComments.stream().map(cc -> buildCommentVO(cc)).collect(Collectors.toList());
            baseRequestVO.setRecords(ccVO);
        }
        return PoetryResult.success(baseRequestVO);
    }

    @Override
    public PoetryResult<Page> listAdminComment(BaseRequestVO baseRequestVO, Boolean isBoss) {
        LambdaQueryChainWrapper<Comment> wrapper = lambdaQuery();
        if (isBoss) {
            if (baseRequestVO.getSource() != null) {
                wrapper.eq(Comment::getSource, baseRequestVO.getSource());
            }
            IPage result = page(new Page<>(baseRequestVO.getCurrent(),baseRequestVO.getSize()),wrapper.getWrapper());
            wrapper.orderByDesc(Comment::getCreateTime).page(result);
        } else {
            List<Integer> userArticleIds = commonQuery.getUserArticleIds(PoetryUtil.getUserId());
            if (CollectionUtils.isEmpty(userArticleIds) ||
                    (!CollectionUtils.isEmpty(userArticleIds) &&
                            baseRequestVO.getSource() != null &&
                            !userArticleIds.contains(baseRequestVO.getSource()))) {
                baseRequestVO.setTotal(0);
                baseRequestVO.setRecords(new ArrayList());
            } else {
                if (baseRequestVO.getSource() != null) {
                    wrapper.eq(Comment::getSource, baseRequestVO.getSource());
                } else {
                    wrapper.in(Comment::getSource, userArticleIds);
                }
                    IPage result = page(new Page<>(baseRequestVO.getCurrent(),baseRequestVO.getSize()),wrapper.getWrapper());
                wrapper.orderByDesc(Comment::getCreateTime).page(result);
            }
        }
        return PoetryResult.success(baseRequestVO);
    }

    private CommentVO buildCommentVO(Comment c) {
        CommentVO commentVO = new CommentVO();
        BeanUtils.copyProperties(c, commentVO);

        User user = commonQuery.getUser(commentVO.getUserId());
        if (user != null) {
            commentVO.setAvatar(user.getAvatar());
            commentVO.setUsername(user.getUsername());
        }

        if (!StringUtils.hasText(commentVO.getUsername())) {
            commentVO.setUsername(PoetryUtil.getRandomName(commentVO.getUserId().toString()));
        }

        if (commentVO.getParentUserId() != null) {
            User u = commonQuery.getUser(commentVO.getParentUserId());
            if (u != null) {
                commentVO.setParentUsername(u.getUsername());
            }
            if (!StringUtils.hasText(commentVO.getParentUsername())) {
                commentVO.setParentUsername(PoetryUtil.getRandomName(commentVO.getParentUserId().toString()));
            }
        }
        return commentVO;
    }
}
