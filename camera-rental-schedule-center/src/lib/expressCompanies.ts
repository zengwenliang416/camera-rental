export interface ExpressCompany {
  code: string;
  expressName: string;
  expressAlias?: string;
  hot?: boolean;
}

export const DEFAULT_EXPRESS_COMPANIES: ExpressCompany[] = [
  { code: 'shunfeng', expressName: '顺丰速运', expressAlias: '顺丰', hot: true },
  { code: 'jd', expressName: '京东物流', expressAlias: '京东' },
  { code: 'debangwuliu', expressName: '德邦物流' },
  { code: 'jtexpress', expressName: '极兔速递', expressAlias: '极兔' },
  { code: 'other', expressName: '同城闪送/自提' },
];

export function expressCodeFromName(name: string) {
  if (name.includes('顺丰')) return 'shunfeng';
  if (name.includes('京东')) return 'jd';
  if (name.includes('德邦物流')) return 'debangwuliu';
  if (name.includes('德邦')) return 'debangkuaidi';
  if (name.includes('极兔')) return 'jtexpress';
  return 'other';
}
