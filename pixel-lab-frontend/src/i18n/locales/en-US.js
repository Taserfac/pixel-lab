/**
 * 【文件路径】src/i18n/locales/en-US.js
 * 【文件功能说明】英文语言包
 * - 包含所有组件的英文翻译
 * - 按功能模块分组组织
 */

export default {
  // Navigation
  nav: {
    home: 'Home',
    community: 'Community',
    personal: 'Profile',
    admin: 'Admin',
    settings: 'Settings',
    workbench: 'Image Studio',
    draw: 'Creative Canvas',
    create: 'Create',
    dashboard: 'Discover'
  },

  // Common Actions
  action: {
    search: 'Search',
    upload: 'Upload',
    cancel: 'Cancel',
    confirm: 'Confirm',
    save: 'Save',
    delete: 'Delete',
    edit: 'Edit',
    share: 'Share',
    follow: 'Follow',
    unfollow: 'Unfollow',
    like: 'Like',
    collect: 'Collect',
    comment: 'Comment',
    viewMore: 'View More',
    loadMore: 'Load More',
    browseCommunity: 'Browse Community',
    goToUpload: 'Go to Upload',
    goToCommunity: 'Browse Community'
  },

  // Status
  status: {
    loading: 'Loading...',
    empty: 'No data available',
    noData: 'No more data',
    success: 'Operation successful',
    error: 'Operation failed',
    retry: 'Retry'
  },

  // Dashboard
  dashboard: {
    title: 'Discover Great Creations',
    subtitle: 'Share Works & Get Inspired',
    uploadWork: 'Upload Work',
    featuredWorks: 'Featured Works',
    viewMore: 'View More',
    creationCenter: 'Creation Center',
    hotTags: 'Popular Tags',
    recommendedCreators: 'Recommended Creators',
    communityStats: 'Community Stats',
    todayHot: "Today's Hot",
    activeCreators: 'Active Creators',
    hotWorks: 'Hot Works',
    communityDiscussion: 'Community Discussion',
    more: 'More',
    noRecommendedCreators: 'No recommended creators',
    noPublicWorks: 'No public works',
    noPublicWorksDesc: 'Upload and publish your works to create a community feed here.',
    previewLabel: 'Work Preview',
    worksCount: '{count} works'
  },

  // Creation Center
  creation: {
    uploadWork: 'Upload Work',
    uploadWorkDesc: 'Share your creation with the community',
    onlineCreate: 'Creative Canvas',
    onlineCreateDesc: 'Quickly start creating a new work',
    myWorks: 'My Works',
    myWorksDesc: 'Manage all your works',
    workbench: 'Image Studio',
    workbenchDesc: 'Import images for editing',
    creationCenter: 'Creation Center',
    myWorkFlow: 'My Works Feed',
    worksShowing: '{count} works displayed'
  },

  // Community
  community: {
    searchPlaceholder: 'Search works, tags...',
    searchPlaceholderFull: 'Search works, authors, tags',
    latest: 'Latest',
    hottest: 'Popular',
    all: 'All',
    recommended: 'Recommended',
    following: 'Following',
    noWorks: 'No works yet',
    noWorksDesc: 'No public works available. Be the first to upload!',
    loadMore: 'Load More',
    comments: 'Comments',
    noComments: 'No comments yet',
    noCommentsDesc: 'Be the first to leave a comment!',
    commentPlaceholder: 'Write your comment...',
    publish: 'Post',
    communityTag: 'Work Sharing Community'
  },

  // Tags
  tag: {
    illustration: 'Illustration',
    photography: 'Photography',
    design: 'Design',
    uiDesign: 'UI Design',
    pixelArt: 'Pixel Art',
    aiArt: 'AI Art'
  },

  // Image Studio
  workbench: {
    import: 'Import',
    filters: 'Filters',
    adjustments: 'Adjustments',
    crop: 'Crop',
    transform: 'Transform',
    pixelArt: 'Pixel Art',
    layers: 'Layers',
    templates: 'Templates',
    brightness: 'Brightness',
    contrast: 'Contrast',
    saturation: 'Saturation',
    exportPNG: 'Export PNG',
    exportJPG: 'Export JPG',
    exportWebP: 'Export WebP',
    quality: 'Quality',
    undo: 'Undo',
    redo: 'Redo',
    zoomIn: 'Zoom In',
    zoomOut: 'Zoom Out',
    resetView: 'Reset View'
  },

  // Authentication
  auth: {
    login: 'Login',
    register: 'Register',
    username: 'Username',
    password: 'Password',
    confirmPassword: 'Confirm Password',
    email: 'Email',
    phone: 'Phone',
    loginTitle: 'Login',
    registerTitle: 'Register',
    noAccount: "Don't have an account?",
    hasAccount: 'Already have an account?',
    rememberMe: 'Remember me',
    forgotPassword: 'Forgot password?',
    logout: 'Logout',
    usernamePlaceholder: 'Enter username',
    passwordPlaceholder: 'Enter password',
    platformDesc: 'Digital Image Creation & Management Platform',
    usernameLength: 'Length must be 4-20 characters',
    passwordLength: 'Length must be 6-20 characters'
  },

  // Personal Center
  personal: {
    works: 'Works',
    collections: 'Collections',
    likes: 'Likes',
    insights: 'Insights',
    uploadImage: 'Upload Image',
    editProfile: 'Edit Profile',
    public: 'Public',
    private: 'Private',
    setToPrivate: 'Set to Private',
    setToPublic: 'Set to Public',
    myProfile: 'My Profile',
    profileDesc: 'Capture inspiration, organize works, and share creations. Let every pixel experiment become your visual homepage.',
    noWorks: 'No works yet',
    noWorksDesc: 'Upload images and they will automatically appear on your profile.',
    noCollections: 'No collections yet',
    noCollectionsDesc: 'Discover works worth revisiting in the community.',
    view: 'View',
    delete: 'Delete',
    workActions: 'Work actions'
  },

  // Admin
  admin: {
    stats: 'Statistics',
    users: 'Users',
    images: 'Works',
    totalUsers: 'Total Users',
    totalWorks: 'Total Works',
    dashboard: 'Admin Dashboard'
  },

  // Settings
  settings: {
    title: 'Settings',
    profile: 'Profile',
    password: 'Change Password',
    theme: 'Theme Settings',
    darkMode: 'Dark Mode',
    lightMode: 'Light Mode',
    language: 'Language'
  },

  // Notifications
  notification: {
    title: 'Notifications',
    markAllRead: 'Mark all as read',
    noNotifications: 'No notifications',
    liked: '{user} liked your work',
    commented: '{user} commented on your work',
    followed: '{user} followed you',
    system: 'System Notification'
  },

  // Work Detail
  workDetail: {
    views: 'views',
    likes: 'likes',
    collects: 'collects',
    liked: 'Liked',
    unliked: 'Like',
    collected: 'Collected',
    uncollected: 'Collect',
    anonymous: 'Anonymous',
    details: 'Work Details'
  },

  // Form Validation
  validation: {
    required: 'This field is required',
    emailInvalid: 'Please enter a valid email address',
    phoneInvalid: 'Please enter a valid phone number',
    passwordMismatch: 'Passwords do not match'
  },

  // Common
  common: {
    cancel: 'Cancel',
    confirm: 'OK',
    save: 'Save',
    close: 'Close',
    back: 'Back',
    next: 'Next',
    previous: 'Previous',
    done: 'Done',
    all: 'All',
    more: 'More',
    less: 'Less',
    noMore: 'No more data',
    copySuccess: 'Copied successfully',
    copyFail: 'Copy failed',
    networkError: 'Network error, please try again later',
    serverError: 'Server error, please try again later'
  }
}
