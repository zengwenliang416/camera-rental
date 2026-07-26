import dayjs from 'dayjs';

export const rentalProducts = [
  {
    id: 'p4p',
    title: 'DJI Phantom 4 Pro',
    category: '无人机',
    subtitle: '航拍巡检、活动记录、短片拍摄',
    rentPerDay: 16800,
    deposit: 300000,
    stockText: '3 台可约',
    status: '可租',
    tags: ['4K 航拍', '三电套装', '含收纳箱'],
  },
  {
    id: 'a7m4',
    title: '索尼 A7M4',
    category: '相机',
    subtitle: '全画幅混合旗舰，适合婚礼和商业拍摄',
    rentPerDay: 12800,
    deposit: 500000,
    stockText: '5 台可约',
    status: '热门',
    tags: ['全画幅', '4K 60P', '双卡槽'],
  },
  {
    id: 'fe2470',
    title: '索尼 FE 24-70mm F2.8 GM II',
    category: '镜头',
    subtitle: '标准变焦工作镜，活动和人像通用',
    rentPerDay: 9800,
    deposit: 420000,
    stockText: '2 支可约',
    status: '可租',
    tags: ['F2.8', 'G Master', '轻量化'],
  },
  {
    id: 'rs3',
    title: 'DJI RS 3 Pro 稳定器',
    category: '配件',
    subtitle: '单兵视频拍摄、跟拍和直播稳定方案',
    rentPerDay: 6800,
    deposit: 180000,
    stockText: '4 台可约',
    status: '可租',
    tags: ['三轴稳定', '快拆', '长续航'],
  },
];

export const rentalOrders = [
  {
    id: 'R20260726001',
    title: '索尼 A7M4 + FE 24-70 GM II',
    status: '待支付',
    dateRange: '2026-07-28 至 2026-07-31',
    amount: 90400,
    logistics: '待支付后生成发货计划',
  },
  {
    id: 'R20260725008',
    title: 'DJI Phantom 4 Pro',
    status: '待发货',
    dateRange: '2026-07-29 至 2026-08-01',
    amount: 67200,
    logistics: '仓库复核设备 SN 后发货',
  },
  {
    id: 'R20260722003',
    title: 'DJI RS 3 Pro 稳定器',
    status: '租用中',
    dateRange: '2026-07-23 至 2026-07-26',
    amount: 27200,
    logistics: '顺丰运输中，等待客户发回',
  },
];

export const rentalAddresses = [
  {
    id: 'addr-1',
    name: '测试客户',
    mobile: '138****0000',
    detail: '上海市 浦东新区 相机租赁测试地址',
    isDefault: true,
  },
];

export const rentalFavorites = rentalProducts.slice(0, 2);

export const getRentalProduct = (id) => {
  return rentalProducts.find((item) => item.id === id) || rentalProducts[0];
};

export const formatCent = (value) => {
  return `¥${(Number(value || 0) / 100).toFixed(2)}`;
};

export const createDefaultRentalDates = () => {
  const start = dayjs().add(1, 'day');
  return {
    startDate: start.format('YYYY-MM-DD'),
    endDate: start.add(3, 'day').format('YYYY-MM-DD'),
  };
};
