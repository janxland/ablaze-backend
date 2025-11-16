package com.ld.poetry.utils;

public class CommonConst {

    /**
     * JWT Token相关常量
     */
    public static final String TOKEN_HEADER = "Authorization";
    
    /**
     * JWT Token前缀
     */
    public static final String TOKEN_PREFIX = "Bearer ";
    
    /**
     * JWT Token过期时间：24小时（秒）
     */
    public static final long JWT_TOKEN_EXPIRE = 86400;
    
    /**
     * 缓存过期时间：6小时（秒）- 保持向后兼容
     */
    public static final long TOKEN_EXPIRE = 21600;
    
    /**
     * 用户类型标识
     */
    public static final String USER_TYPE_NORMAL = "NORMAL";
    public static final String USER_TYPE_ADMIN = "ADMIN";
    public static final String USER_TYPE_SUPER_ADMIN = "SUPER_ADMIN";
    
    /**
     * 旧版Token常量 - 保持向后兼容（已弃用，建议使用JWT）
     */
    @Deprecated
    public static final String USER_TOKEN = "user_token_";
    @Deprecated
    public static final String ADMIN_TOKEN = "admin_token_";

    /**
     * Boss信息
     */
    public static final String ADMIN = "admin";
    public static final int ADMIN_ID = 983341575;
    /**
     * 评论和IM邮件
     */
    public static final String COMMENT_IM_MAIL = "comment_im_mail_";

    /**
     * 评论和IM邮件发送次数
     */
    public static final int COMMENT_IM_MAIL_COUNT = 1;

    /**
     * 验证码
     */
    public static final String USER_CODE = "user_code_";

    /**
     * 忘记密码时获取验证码用于找回密码
     */
    public static final String FORGET_PASSWORD = "forget_password_";

    /**
     * 网站信息
     */
    public static final String WEB_INFO = "webInfo";

    /**
     * 分类信息
     */
    public static final String SORT_INFO = "sortInfo";

    /**
     * 密钥
     */
    public static final String CRYPOTJS_KEY = "J$A@N#X$L%A&N*D^";
    /**
     * JWT密钥
     */
    public static final String JWT_KEY = "J$A@N#X$L%A&N*D";
    /**
     * 根据用户ID获取用户信息
     */
    public static final String USER_CACHE = "user_";

    /**
     * 根据文章ID获取评论数量
     */
    public static final String COMMENT_COUNT_CACHE = "comment_count_";

    /**
     * 根据用户ID获取该用户所有文章ID
     */
    public static final String USER_ARTICLE_LIST = "user_article_list_";

    /**
     * 默认缓存过期时间
     */
    public static final long EXPIRE = 1800;

    /**
     * 树洞一次最多查询条数
     */
    public static final int TREE_HOLE_COUNT = 200;

    /**
     * 顶层评论ID
     */
    public static final int FIRST_COMMENT = 0;

    /**
     * 文章摘要默认字数
     */
    public static final int SUMMARY = 80;

    /**
     * 留言的源
     */
    public static final int TREE_HOLE_COMMENT_SOURCE = 0;

    /**
     * 七牛云
     */
    public static final String ACCESS_KEY = "O5u9pDpH4cq2DzN0j2fuFTB21EncLgxQGa1bKiQm";

    public static final String SECRET_KEY = "m1P1uLViSiXyLP0qqXepKNj7VEpZG87BM4jrGj6i";

    public static final String BUCKET = "roginx";

    /**
     * 资源类型
     */
    public static final String PATH_TYPE_GRAFFITI = "graffiti";

    public static final String PATH_TYPE_ARTICLE_PICTURE = "articlePicture";

    public static final String PATH_TYPE_USER_AVATAR = "userAvatar";

    public static final String PATH_TYPE_ARTICLE_COVER = "articleCover";

    public static final String PATH_TYPE_WEB_BACKGROUND_IMAGE = "webBackgroundImage";

    public static final String PATH_TYPE_WEB_AVATAR = "webAvatar";

    public static final String PATH_TYPE_RANDOM_AVATAR = "randomAvatar";

    public static final String PATH_TYPE_RANDOM_COVER = "randomCover";

    public static final String PATH_TYPE_COMMENT_PICTURE = "commentPicture";

    public static final String PATH_TYPE_INTERNET_MEME = "internetMeme";

    public static final String PATH_TYPE_IM_GROUP_AVATAR = "im/groupAvatar";

    public static final String PATH_TYPE_IM_GROUP_MESSAGE = "im/groupMessage";

    public static final String PATH_TYPE_IM_FRIEND_MESSAGE = "im/friendMessage";

    /**
     * 资源路径
     */
    public static final String RESOURCE_PATH_TYPE_FRIEND = "friendUrl";

    /**
     * 微言
     */
    public static final String WEIYAN_TYPE_FRIEND = "friend";

    public static final String WEIYAN_TYPE_NEWS = "news";
}
