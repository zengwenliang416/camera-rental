import type { PageResult } from './client';

type PageFetcher<T> = (pageNo: number, pageSize: number) => Promise<PageResult<T>>;

export async function fetchAllPages<T>(
  fetchPage: PageFetcher<T>,
  pageSize: number,
  concurrency = 4
): Promise<PageResult<T>> {
  const firstPage = await fetchPage(1, pageSize);
  const firstList = firstPage.list || [];
  const total = firstPage.total || 0;

  if (firstList.length < pageSize) {
    return {
      list: firstList,
      total: Math.max(total, firstList.length),
    };
  }

  if (total <= 0) {
    const list = [...firstList];
    let pageNo = 2;
    while (true) {
      const page = await fetchPage(pageNo, pageSize);
      const pageList = page.list || [];
      list.push(...pageList);
      if (pageList.length < pageSize) break;
      pageNo += 1;
    }
    return { list, total: list.length };
  }

  const totalPages = Math.ceil(total / pageSize);
  if (totalPages <= 1) {
    return {
      list: firstList,
      total: Math.max(total, firstList.length),
    };
  }

  const remainingPages = Array.from(
    { length: totalPages - 1 },
    (_, index) => index + 2
  );
  const pageLists: T[][] = new Array(remainingPages.length);
  let nextIndex = 0;

  async function worker() {
    while (nextIndex < remainingPages.length) {
      const index = nextIndex;
      nextIndex += 1;
      const page = await fetchPage(remainingPages[index], pageSize);
      pageLists[index] = page.list || [];
    }
  }

  const workerCount = Math.min(
    Math.max(1, Math.floor(concurrency)),
    remainingPages.length
  );
  await Promise.all(Array.from({ length: workerCount }, () => worker()));

  const list = firstList.concat(...pageLists);
  return {
    list,
    total: Math.max(total, list.length),
  };
}
