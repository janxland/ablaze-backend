package com.ld.poetry.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ld.poetry.config.PoetryResult;
import com.ld.poetry.dao.ArticleMapper;
import com.ld.poetry.dao.DiaryMapper;
import com.ld.poetry.entity.Article;
import com.ld.poetry.entity.Label;
import com.ld.poetry.entity.Sort;
import com.ld.poetry.entity.User;
import com.ld.poetry.entity.UserArticleAuth;
import com.ld.poetry.service.ArticleService;
import com.ld.poetry.service.UserArticleAuthService;
import com.ld.poetry.utils.*;
import com.ld.poetry.vo.ArticleVO;
import com.ld.poetry.vo.BaseRequestVO;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * <p>
 * 文章表 服务实现类
 * </p>
 *
 * @author sara
 * @since 2021-08-13
 */
@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {
    @Autowired
    private UserArticleAuthService userArticleAuthService;
    
    @Autowired
    private DiaryMapper diaryMapper;
    
    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private CommonQuery commonQuery;

    @Override
    public PoetryResult saveArticle(ArticleVO articleVO) {
        if (articleVO.getViewStatus() != null && !articleVO.getViewStatus() && !StringUtils.hasText(articleVO.getPassword())) {
            return PoetryResult.fail("请设置文章密码！");
        }
        Article article = new Article();
        if (StringUtils.hasText(articleVO.getArticleCover())) {
            article.setArticleCover(articleVO.getArticleCover());
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
        lambdaUpdate().eq(Article::getId, id)
                .eq(Article::getUserId, userId)
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
        LambdaUpdateChainWrapper<Article> updateChainWrapper = lambdaUpdate()
                .eq(Article::getId, articleVO.getId())
                .eq(Article::getUserId, userId)
                .set(Article::getLabelId, articleVO.getLabelId())
                .set(Article::getSortId, articleVO.getSortId())
                .set(Article::getCollectionId, articleVO.getCollectionId())
                .set(Article::getArticleTitle, articleVO.getArticleTitle())
                .set(Article::getUpdateBy, PoetryUtil.getUsername())
                .set(Article::getArticleContent, articleVO.getArticleContent())
                .set(Article::getKeywords, articleVO.getKeywords());

        if (StringUtils.hasText(articleVO.getArticleCover())) {
            updateChainWrapper.set(Article::getArticleCover, articleVO.getArticleCover());
        }
        if (articleVO.getCommentStatus() != null) {
            updateChainWrapper.set(Article::getCommentStatus, articleVO.getCommentStatus());
        }

        if (articleVO.getRecommendStatus() != null) {
            updateChainWrapper.set(Article::getRecommendStatus, articleVO.getRecommendStatus());
        }

        if (articleVO.getViewStatus() != null && !articleVO.getViewStatus() && StringUtils.hasText(articleVO.getPassword())) {
            updateChainWrapper.set(Article::getPassword, articleVO.getPassword());
        }
        if (articleVO.getViewStatus() != null) {
            updateChainWrapper.set(Article::getViewStatus, articleVO.getViewStatus());
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
        LambdaQueryChainWrapper<Article> lambdaQuery = lambdaQuery();
        lambdaQuery.eq(Article::getViewStatus, PoetryEnum.STATUS_ENABLE.getCode());
        // .select(Article.class, a -> ((!a.getColumn().equals("article_content"))));
        if (StringUtils.hasText(baseRequestVO.getKeywords())) {
            lambdaQuery
            .like(Article::getArticleTitle, baseRequestVO.getKeywords())
            .or().like(Article::getKeywords, baseRequestVO.getKeywords());
        }
        if (baseRequestVO.getRecommendStatus() != null && baseRequestVO.getRecommendStatus()) {
            lambdaQuery.eq(Article::getRecommendStatus, PoetryEnum.STATUS_ENABLE.getCode());
        }

        if (baseRequestVO.getLabelId() != null) {
            lambdaQuery.eq(Article::getLabelId, baseRequestVO.getLabelId());
        } else if (baseRequestVO.getSortId() != null) {
            lambdaQuery.eq(Article::getSortId, baseRequestVO.getSortId());
        }

        lambdaQuery.orderByDesc(Article::getCreateTime).page((Page)baseRequestVO);

        List<Article> records = baseRequestVO.getRecords();
        if (!CollectionUtils.isEmpty(records)) {
            List<ArticleVO> collect = records.stream().map(article -> {
                article.setPassword(null);
                if (article.getArticleContent().length() > CommonConst.SUMMARY) {
                    article.setArticleContent(article.getArticleContent().substring(0, CommonConst.SUMMARY).replace("`", "").replace("#", "").replace(">", ""));
                }
                ArticleVO articleVO = buildArticleVO(article, false);
                return articleVO;
            }).collect(Collectors.toList());
            baseRequestVO.setRecords(collect);
        }
        return PoetryResult.success(baseRequestVO);
    }
    private static final Pattern HIDE_CONTENT_PATTERN = Pattern.compile(
        "(\\[hidecontent\\s+type=[\"']?(\\w+)[\"']?[^\\]]*\\])(.*?)(\\[/hidecontent\\])",
        Pattern.DOTALL
    );
    /**
     * 如果未登录或文章ID为空时，统一将所有 [hidecontent type="xxx"]...[/hidecontent]
     * 的内容清空，但保留开、闭标签。
     * 可选：给标签加 isshow="false" 标记
     */
    private String removeAllHiddenContent(String content) {
        Matcher matcher = HIDE_CONTENT_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            // group(1) = 原始开标签
            // group(2) = 标签中的 type 值 (可以不用)
            // group(3) = 中间内容
            // group(4) = 闭标签

            String openTag       = matcher.group(1); // [hidecontent type="xxx" ...]
            String typeValue     = matcher.group(2); // 比如 "vip1"
            String hiddenContent = matcher.group(3); // 中间要清空的部分
            String closeTag      = matcher.group(4); // [/hidecontent]

            // 这里要把中间内容移除，所以 finalContent = ""
            String finalContent  = "";
            String openTagWithAttr;
            if (openTag.endsWith("]")) {
                openTagWithAttr = openTag.substring(0, openTag.length() - 1)
                        + " isshow=\"false\"]";
            } else {
                openTagWithAttr = openTag + " isshow=\"false\"]";
            }
            String replacement = openTagWithAttr + finalContent + closeTag;
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        // 将剩余未匹配的部分拼接上
        matcher.appendTail(result);

        return result.toString();
    }
    private String processHiddenContent(String content, Integer userId, Integer articleId) {
        // 如果用户/文章信息为空，则直接清理所有隐藏内容（但保留标签）
        if (userId == null || articleId == null) {
            return removeAllHiddenContent(content);
        }
    
        // 示例：查询当前用户是否拥有各项权限
        UserArticleAuth auth = userArticleAuthService.findByUserAndArticle(userId, articleId);
        int vip1  = (auth != null && auth.getVip1()  != null) ? auth.getVip1()  : 0;
        int pay   = (auth != null && auth.getPay()   != null) ? auth.getPay()   : 0;
        int reply = (auth != null && auth.getReply() != null) ? auth.getReply() : 0;
    
        Matcher matcher = HIDE_CONTENT_PATTERN.matcher(content);
        StringBuffer result = new StringBuffer();
    
        while (matcher.find()) {
            // group(1) = 原始开标签, 例如 [hidecontent type="vip1" xxx...]
            // group(2) = 具体的 type 值 (因为上面的正则里捕获了 (\\w+) )
            // group(3) = 中间隐藏内容
            // group(4) = 闭标签 [/hidecontent]
            String openTag = matcher.group(1);
            String type    = matcher.group(2).toLowerCase();
            String hiddenContent = matcher.group(3);
            String closeTag = matcher.group(4);
    
            // 判断权限
            boolean hasPermission;
            switch (type) {
                case "logged":
                    hasPermission = (userId != null);
                    break;
                case "reply":
                    hasPermission = (reply == 1);
                    break;
                case "vip1":
                    hasPermission = (vip1 == 1);
                    break;
                case "pay":
                    hasPermission = (pay == 1);
                    break;
                default:
                    // 对于未知类型，直接当没权限处理
                    hasPermission = false;
            }
    
            // 根据权限决定 isshow 的值，以及是否保留隐藏内容
            String isShowValue = hasPermission ? "true" : "false";
            String finalContent = hasPermission ? hiddenContent : "";
    
            // 在开标签里额外加一个属性 isshow="true|false"
            // 这里演示简单追加（如果项目中已有别的属性需要更精细处理）
            // 以免出现形如 `[hidecontent type="vip1" issome="1"isshow="true"]` 的情况
            // 可以在插入前先判断 openTag 是否带空格，必要时再加一个空格
            // 下面是一个简易写法：
            String openTagWithAttr;
            if (openTag.endsWith("]")) {
                // 去掉最后一个 `]`
                openTagWithAttr = openTag.substring(0, openTag.length() - 1)
                        + " isshow=\"" + isShowValue + "\"]";
            } else {
                // 理论上不会出现这种情况，除非开标签不规范
                openTagWithAttr = openTag + " isshow=\"" + isShowValue + "\"]";
            }
    
            // 重新组装：开标签(加了 isshow) + (保留或去掉内容) + 闭标签
            String replacement = openTagWithAttr + finalContent + closeTag;
    
            // 把 replacement 填入结果中
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        // 处理剩余没有匹配到的字符串
        matcher.appendTail(result);
    
        return result.toString();
    }
    

    /**
     * 获取文章详情
     * @param id 文章ID
     * @param flag 是否某种标志(示例保留)
     * @param password 用户输入的密码(如果文章加密)
     */
    public PoetryResult<ArticleVO> getArticleById(Integer id, Boolean flag, String password) {
        // 1) 查找数据库中的 Article
        LambdaQueryChainWrapper<Article> lambdaQuery = lambdaQuery();
        lambdaQuery.eq(Article::getId, id);

        Article article = lambdaQuery.one();
        if (article == null) {
            return PoetryResult.fail("文章不存在");
        }
        if (!article.getViewStatus()) {
            return PoetryResult.fail("该文章已关闭");
        }

        // 2) 判断当前用户是否为管理员
        //    （需要你在 PoetryUtil.getCurrentUser() 返回的对象里有 userType 字段）
        Integer currentUserId = PoetryUtil.getUserId();
        boolean isAdmin = false;
        if (currentUserId != null && PoetryUtil.getCurrentUser() != null) {
            Integer userType = PoetryUtil.getCurrentUser().getUserType();
            if (userType != null && userType.equals(PoetryEnum.USER_TYPE_ADMIN.getCode())) {
                isAdmin = true;
            }
        }

        // 3) 如果文章有密码保护，且不是管理员时，需要校验密码
        //    管理员可绕过密码校验
        if (!isAdmin && StringUtils.hasText(article.getPassword())) {
            if (!StringUtils.hasText(password) || !password.equals(article.getPassword())) {
                return PoetryResult.fail("密码错误" + (StringUtils.hasText(article.getTips()) ? article.getTips() : "请联系作者获取密码"));
            }
        }

        // 4) 处理隐藏内容
        String processedContent;
        if (isAdmin) {
            // 管理员：直接查看全部内容，无需隐藏
            processedContent = article.getArticleContent();
        } else {
            // 非管理员
            if (currentUserId == null) {
                // 未登录时，移除所有隐藏内容，也可改为统一提示
                processedContent = removeAllHiddenContent(article.getArticleContent());
            } else {
                // 已登录时，根据 user_article_auth 表判断权限
                processedContent = processHiddenContent(article.getArticleContent(), currentUserId, article.getId());
            }
        }

        // 5) 更新浏览次数(若访问者不是作者本人)
        articleMapper.updateViewCount(id);

        // 6) 管理员不需要隐藏密码，普通用户将密码清空
        if (!isAdmin) {
            article.setPassword(null);
        }
        // 将处理后的内容设置回 article
        article.setArticleContent(processedContent);

        // 7) 构建 VO 返回
        ArticleVO articleVO = buildArticleVO(article, false);
        return PoetryResult.success(articleVO);
    }



    @Override
    public PoetryResult<Page> listAdminArticle(BaseRequestVO baseRequestVO, Boolean isBoss) {
        LambdaQueryChainWrapper<Article> lambdaQuery = lambdaQuery();
        lambdaQuery.select(Article.class, a -> !a.getColumn().equals("article_content"));
        if (!isBoss) {
            lambdaQuery.eq(Article::getUserId, PoetryUtil.getUserId());
        } else {
            if (baseRequestVO.getUserId() != null) {
                lambdaQuery.eq(Article::getUserId, baseRequestVO.getUserId());
            }
        }
        if (StringUtils.hasText(baseRequestVO.getKeywords())) {
            lambdaQuery.like(Article::getArticleTitle, baseRequestVO.getKeywords());
        }
        if (baseRequestVO.getRecommendStatus() != null && baseRequestVO.getRecommendStatus()) {
            lambdaQuery.eq(Article::getRecommendStatus, PoetryEnum.STATUS_ENABLE.getCode());
        }

        if (baseRequestVO.getLabelId() != null) {
            lambdaQuery.eq(Article::getLabelId, baseRequestVO.getLabelId());
        }

        if (baseRequestVO.getSortId() != null) {
            lambdaQuery.eq(Article::getSortId, baseRequestVO.getSortId());
        }

        lambdaQuery.orderByDesc(Article::getCreateTime).page((Page)baseRequestVO);

        List<Article> records = baseRequestVO.getRecords();
        if (!CollectionUtils.isEmpty(records)) {
            List<ArticleVO> collect = records.stream().map(article -> {
                article.setPassword(null);
                ArticleVO articleVO = buildArticleVO(article, true);
                return articleVO;
            }).collect(Collectors.toList());
            baseRequestVO.setRecords(collect);
        }
        return PoetryResult.success(baseRequestVO);
    }

    @Override
    public PoetryResult<ArticleVO> getArticleByIdForUser(Integer id) {
        LambdaQueryChainWrapper<Article> lambdaQuery = lambdaQuery();
        lambdaQuery.eq(Article::getId, id).eq(Article::getUserId, PoetryUtil.getUserId());
        Article article = lambdaQuery.one();
        if (article == null) {
            return PoetryResult.fail("文章不存在！");
        }
        ArticleVO articleVO = new ArticleVO();
        BeanUtils.copyProperties(article, articleVO);
        return PoetryResult.success(articleVO);
    }

    private ArticleVO buildArticleVO(Article article, Boolean isAdmin) {
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
