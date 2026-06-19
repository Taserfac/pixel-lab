import { mkdir, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';

const categories = [
  {
    category: '造型基础',
    keywords: ['绘画 造型 基础 教程', '美术 造型能力 教程', '结构素描 造型 教程']
  },
  {
    category: '素描观察',
    keywords: ['素描 观察方法 教程', '素描 基础 观察 教程', '美术 观察 训练 教程']
  },
  {
    category: '色彩修养',
    keywords: ['绘画 色彩理论 教程', '色彩关系 绘画 教程', '色彩美学 美术 教程']
  },
  {
    category: '构图审美',
    keywords: ['绘画 构图 原理 教程', '画面构图 美术 教程', '构图审美 绘画 教程']
  },
  {
    category: '光影关系',
    keywords: ['绘画 光影关系 教程', '明暗关系 素描 教程', '光影塑造 美术 教程']
  },
  {
    category: '透视空间',
    keywords: ['绘画 透视 原理 教程', '空间关系 绘画 教程', '一点透视 两点透视 教程']
  },
  {
    category: '人体结构',
    keywords: ['绘画 人体结构 教程', '人体比例 绘画 教程', '艺用解剖 绘画 教程']
  },
  {
    category: '动态速写',
    keywords: ['速写 动态 绘画 教程', '人物动态 速写 教程', '动态线 绘画 教程']
  },
  {
    category: '材质肌理',
    keywords: ['绘画 材质 表现 教程', '质感表现 美术 教程', '肌理表现 绘画 教程']
  },
  {
    category: '大师临摹',
    keywords: ['大师作品 临摹 教程', '名画 临摹 绘画 教程', '大师色彩 临摹 教程']
  },
  {
    category: '艺术史鉴赏',
    keywords: ['艺术史 美术 鉴赏 课程', '西方美术史 课程', '中国美术史 课程']
  },
  {
    category: '视觉叙事',
    keywords: ['视觉叙事 绘画 教程', '插画 叙事 构图 教程', '画面叙事 美术 教程']
  }
];

const outputSql = resolve('pixel-lab-java-backend/src/main/resources/tutorial_videos_seed.sql');
const outputJson = resolve('pixel-lab-java-backend/src/main/resources/tutorial_videos_seed.json');

const maxPerKeyword = Number(process.env.MAX_PER_KEYWORD || 8);
const sleepMs = Number(process.env.CRAWL_DELAY_MS || 1200);

const excludedTerms = [
  'ps',
  'photoshop',
  '修图',
  '滤镜',
  '通道',
  '蒙版',
  '抠图',
  '照片',
  '摄影',
  '相机',
  '后期',
  '软件',
  '插件',
  'stable diffusion',
  'midjourney',
  'aigc',
  'ai绘画',
  'ai 绘画',
  '游戏',
  '外挂',
  '自瞄'
];

const sleep = (ms) => new Promise((resolveSleep) => setTimeout(resolveSleep, ms));

const text = (value = '') => String(value)
  .replace(/<[^>]+>/g, '')
  .replace(/&lt;/g, '<')
  .replace(/&gt;/g, '>')
  .replace(/&quot;/g, '"')
  .replace(/&#39;/g, "'")
  .replace(/&amp;/g, '&')
  .replace(/\s+/g, ' ')
  .trim();

const sqlString = (value) => {
  if (value === null || value === undefined || value === '') return 'NULL';
  return `'${String(value).replace(/\\/g, '\\\\').replace(/'/g, "''")}'`;
};

const normalizeCover = (value = '') => {
  if (!value) return '';
  if (value.startsWith('//')) return `https:${value}`;
  return value;
};

const normalizeDuration = (duration) => {
  if (typeof duration === 'string') return duration;
  if (!Number.isFinite(Number(duration))) return '';
  const seconds = Number(duration);
  const minute = Math.floor(seconds / 60);
  const second = String(seconds % 60).padStart(2, '0');
  return `${minute}:${second}`;
};

const isAllowedArtVideo = (video) => {
  const haystack = [
    video.title,
    video.description,
    video.author_name,
    video.source_name
  ].join(' ').toLowerCase();
  return !excludedTerms.some((term) => haystack.includes(term));
};

async function searchBilibili(keyword, page = 1) {
  const params = new URLSearchParams({
    search_type: 'video',
    keyword,
    page: String(page),
    order: 'totalrank'
  });
  const url = `https://api.bilibili.com/x/web-interface/search/type?${params}`;
  const response = await fetch(url, {
    headers: {
      'User-Agent': 'PixelLabArtAcademyCrawler/1.0 (+metadata only)',
      'Referer': 'https://www.bilibili.com/'
    }
  });
  if (!response.ok) {
    throw new Error(`Bilibili search failed ${response.status} ${response.statusText}`);
  }
  const json = await response.json();
  return json?.data?.result || [];
}

function mapBilibiliVideo(item, category, sortOrder) {
  const bvid = item.bvid || '';
  if (!bvid) return null;
  return {
    content_type: 'video',
    category,
    title: text(item.title),
    description: text(item.description),
    cover_url: normalizeCover(item.pic),
    source_url: `https://www.bilibili.com/video/${bvid}`,
    embed_url: `https://player.bilibili.com/player.html?bvid=${encodeURIComponent(bvid)}&page=1&autoplay=0`,
    source_name: 'Bilibili',
    author_name: text(item.author || item.mid || ''),
    duration: normalizeDuration(item.duration),
    view_count: Number(item.play || 0),
    published_at: item.pubdate ? new Date(Number(item.pubdate) * 1000).toISOString().slice(0, 10) : '',
    sort_order: sortOrder
  };
}

async function crawl() {
  const seen = new Set();
  const videos = [];

  for (const [categoryIndex, group] of categories.entries()) {
    for (const [keywordIndex, keyword] of group.keywords.entries()) {
      console.log(`Searching videos ${group.category}: ${keyword}`);
      let results = [];
      try {
        results = await searchBilibili(keyword);
      } catch (error) {
        console.warn(`Skip video keyword "${keyword}": ${error.message}`);
        await sleep(sleepMs * 2);
        continue;
      }
      for (const result of results.slice(0, maxPerKeyword)) {
        const video = mapBilibiliVideo(result, group.category, categoryIndex * 30 + keywordIndex);
        if (!video || !isAllowedArtVideo(video) || seen.has(video.source_url)) continue;
        seen.add(video.source_url);
        videos.push(video);
      }
      await sleep(sleepMs);
    }
  }

  const header = [
    '-- Pixel Lab art academy video seed generated by scripts/crawl-tutorial-videos.mjs',
    '-- Metadata only: titles, summaries, covers, authors, source links, and embed links. Video files are not downloaded.',
    ''
  ].join('\n');

  const statements = videos.map((video) => (
    'INSERT INTO `tutorial_videos` '
    + '(`content_type`, `category`, `title`, `description`, `cover_url`, `source_url`, `embed_url`, `source_name`, `author_name`, `duration`, `view_count`, `published_at`, `sort_order`, `status`) VALUES '
    + `('video', ${sqlString(video.category)}, ${sqlString(video.title)}, ${sqlString(video.description)}, ${sqlString(video.cover_url)}, ${sqlString(video.source_url)}, ${sqlString(video.embed_url)}, ${sqlString(video.source_name)}, ${sqlString(video.author_name)}, ${sqlString(video.duration)}, ${Number(video.view_count || 0)}, ${sqlString(video.published_at)}, ${Number(video.sort_order || 100)}, 1) `
    + 'ON DUPLICATE KEY UPDATE '
    + '`content_type` = VALUES(`content_type`), `category` = VALUES(`category`), `title` = VALUES(`title`), '
    + '`description` = VALUES(`description`), `cover_url` = VALUES(`cover_url`), `embed_url` = VALUES(`embed_url`), '
    + '`source_name` = VALUES(`source_name`), `author_name` = VALUES(`author_name`), `duration` = VALUES(`duration`), '
    + '`view_count` = VALUES(`view_count`), `published_at` = VALUES(`published_at`), `sort_order` = VALUES(`sort_order`), `status` = 1;'
  ));

  await mkdir(dirname(outputSql), { recursive: true });
  await writeFile(outputSql, `${header}${statements.join('\n')}\n`, 'utf8');
  await writeFile(outputJson, `${JSON.stringify(videos, null, 2)}\n`, 'utf8');
  console.log(`Wrote ${videos.length} art academy videos`);
  console.log(outputSql);
  console.log(outputJson);
}

crawl().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
