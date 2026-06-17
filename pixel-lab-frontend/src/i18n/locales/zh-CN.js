/**
 * 【文件路径】src/i18n/locales/zh-CN.js
 * 【文件功能说明】中文语言包
 * - 包含所有组件的中文翻译
 * - 按功能模块分组组织
 */

export default {
  // 导航相关
  nav: {
    home: '首页',
    community: '社区',
    personal: '个人中心',
    admin: '后台管理',
    settings: '设置',
    workbench: '工作台',
    dashboard: '发现'
  },

  // 通用操作
  action: {
    search: '搜索',
    upload: '上传',
    cancel: '取消',
    confirm: '确认',
    save: '保存',
    delete: '删除',
    edit: '编辑',
    share: '分享',
    follow: '关注',
    unfollow: '取消关注',
    like: '点赞',
    collect: '收藏',
    comment: '评论',
    viewMore: '查看更多',
    loadMore: '加载更多',
    browseCommunity: '浏览社区',
    goToUpload: '去上传',
    goToCommunity: '逛逛社区'
  },

  // 状态信息
  status: {
    loading: '加载中...',
    empty: '暂无数据',
    noData: '没有更多数据了',
    success: '操作成功',
    error: '操作失败',
    retry: '重试'
  },

  // 首页/Dashboard
  dashboard: {
    title: '发现优秀创作',
    subtitle: '作品分享与灵感社区',
    uploadWork: '上传作品',
    featuredWorks: '精选作品',
    viewMore: '查看更多',
    creationCenter: '创作中心',
    hotTags: '热门标签',
    recommendedCreators: '推荐创作者',
    communityStats: '社区动态',
    todayHot: '今日热门',
    activeCreators: '活跃创作者',
    hotWorks: '热门作品',
    communityDiscussion: '社区讨论',
    more: '更多',
    noRecommendedCreators: '暂无推荐创作者',
    noPublicWorks: '暂无公开作品',
    noPublicWorksDesc: '上传并公开你的作品后，这里会形成社区作品流。',
    previewLabel: '作品预览',
    worksCount: '{count} 件作品'
  },

  // 创作中心
  creation: {
    uploadWork: '上传作品',
    uploadWorkDesc: '分享你的创作到社区',
    onlineCreate: '在线创作',
    onlineCreateDesc: '快速开始创作新作品',
    myWorks: '我的作品',
    myWorksDesc: '管理你的全部作品',
    workbench: '工作台',
    workbenchDesc: '导入图片进行编辑处理',
    creationCenter: '创作中心',
    myWorkFlow: '我的作品流',
    worksShowing: '{count} 个作品正在展示'
  },

  // 社区相关
  community: {
    searchPlaceholder: '搜索作品、标签...',
    searchPlaceholderFull: '搜索作品、作者、标签',
    latest: '最新',
    hottest: '最热',
    all: '全部',
    recommended: '推荐',
    following: '关注',
    noWorks: '暂无作品',
    noWorksDesc: '还没有公开的作品，快来上传你的第一个作品吧！',
    loadMore: '加载更多',
    comments: '评论',
    noComments: '暂无评论',
    noCommentsDesc: '快来发表第一条评论吧！',
    commentPlaceholder: '写下你的评论...',
    publish: '发表',
    communityTag: '作品分享社区'
  },

  // 标签
  tag: {
    illustration: '插画',
    photography: '摄影',
    design: '设计',
    uiDesign: 'UI设计',
    pixelArt: '像素艺术',
    aiArt: 'AI艺术'
  },

  // 工作台
  workbench: {
    import: '导入',
    filters: '滤镜',
    adjustments: '调整',
    crop: '裁剪',
    transform: '变换',
    pixelArt: '像素化',
    layers: '图层',
    templates: '模板',
    brightness: '亮度',
    contrast: '对比度',
    saturation: '饱和度',
    exportPNG: '导出 PNG',
    exportJPG: '导出 JPG',
    exportWebP: '导出 WebP',
    quality: '质量',
    undo: '撤销',
    redo: '重做',
    zoomIn: '放大',
    zoomOut: '缩小',
    resetView: '重置视图'
  },

  // 认证相关
  auth: {
    login: '登录',
    register: '注册',
    username: '用户名',
    password: '密码',
    confirmPassword: '确认密码',
    email: '邮箱',
    phone: '手机号',
    loginTitle: '登录',
    registerTitle: '注册',
    noAccount: '还没有账号？',
    hasAccount: '已有账号？',
    rememberMe: '记住账号',
    forgotPassword: '忘记密码？',
    logout: '退出登录',
    usernamePlaceholder: '请输入用户名',
    passwordPlaceholder: '请输入密码',
    platformDesc: '数字图像创作与管理平台',
    usernameLength: '长度在 4 到 20 个字符',
    passwordLength: '长度在 6 到 20 个字符'
  },

  // 个人中心
  personal: {
    works: '作品',
    collections: '收藏',
    likes: '点赞',
    insights: '洞察',
    uploadImage: '上传图片',
    editProfile: '编辑资料',
    public: '公开',
    private: '私有',
    setToPrivate: '设为私有',
    setToPublic: '设为公开',
    myProfile: '个人中心',
    profileDesc: '记录灵感、整理作品、分享创作，让每一次像素实验都沉淀成你的视觉主页。',
    noWorks: '还没有作品',
    noWorksDesc: '上传图片后，作品会自动沉淀到你的个人主页。',
    noCollections: '暂无收藏',
    noCollectionsDesc: '去社区发现值得反复看的作品。',
    view: '查看',
    delete: '删除',
    workActions: '作品操作'
  },

  // 管理后台
  admin: {
    stats: '统计',
    users: '用户',
    images: '作品',
    totalUsers: '用户总数',
    totalWorks: '作品总数',
    dashboard: '管理后台'
  },

  // 设置
  settings: {
    title: '设置',
    profile: '个人资料',
    password: '修改密码',
    theme: '主题设置',
    darkMode: '深色模式',
    lightMode: '浅色模式',
    language: '语言设置'
  },

  // 通知
  notification: {
    title: '消息通知',
    markAllRead: '全部标为已读',
    noNotifications: '暂无通知',
    liked: '{user} 赞了你的作品',
    commented: '{user} 评论了你的作品',
    followed: '{user} 关注了你',
    system: '系统通知'
  },

  // 作品详情
  workDetail: {
    views: '浏览',
    likes: '点赞',
    collects: '收藏',
    liked: '已点赞',
    unliked: '点赞',
    collected: '已收藏',
    uncollected: '收藏',
    anonymous: '匿名',
    details: '作品详情'
  },

  // 表单验证
  validation: {
    required: '此项为必填项',
    emailInvalid: '请输入有效的邮箱地址',
    phoneInvalid: '请输入有效的手机号',
    passwordMismatch: '两次输入的密码不一致'
  },

  // 通用
  common: {
    cancel: '取消',
    confirm: '确定',
    save: '保存',
    close: '关闭',
    back: '返回',
    next: '下一步',
    previous: '上一步',
    done: '完成',
    all: '全部',
    more: '更多',
    less: '收起',
    noMore: '没有更多了',
    copySuccess: '复制成功',
    copyFail: '复制失败',
    networkError: '网络异常，请稍后重试',
    serverError: '服务器异常，请稍后重试'
  }
}
