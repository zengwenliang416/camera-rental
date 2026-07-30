import type {
  RentalDeviceVO,
  RentalManualReviewVO,
  RentalScheduleVO,
  SnapshotAccess,
  XianyuOrderVO,
  XianyuPendingShipOrderVO,
} from './rental';
import type { PageResult } from './client';

export interface SnapshotLoaders {
  devices: () => Promise<PageResult<RentalDeviceVO>>;
  schedules: () => Promise<PageResult<RentalScheduleVO>>;
  orders: () => Promise<PageResult<XianyuOrderVO>>;
  pendingShipOrders: () => Promise<PageResult<XianyuPendingShipOrderVO>>;
  reviews: () => Promise<PageResult<RentalManualReviewVO>>;
}

function isAuthenticationFailure(error: unknown) {
  return error instanceof Error &&
    (error.message === 'AUTH_REQUIRED' || error.message === 'NO_REFRESH_TOKEN');
}

async function loadSnapshotPart<T>(
  source: keyof SnapshotLoaders,
  enabled: boolean,
  loader: () => Promise<PageResult<T>>
) {
  if (!enabled) {
    return { page: { list: [], total: 0 }, failure: null };
  }
  try {
    return { page: await loader(), failure: null };
  } catch (error) {
    if (isAuthenticationFailure(error)) throw error;
    return { page: { list: [], total: 0 }, failure: source };
  }
}

export async function loadAuthorizedSnapshot(
  access: SnapshotAccess,
  loaders: SnapshotLoaders
) {
  const [deviceResult, scheduleResult, orderResult, pendingShipResult, reviewResult] =
    await Promise.all([
      loadSnapshotPart('devices', access.devices, loaders.devices),
      loadSnapshotPart('schedules', access.schedules, loaders.schedules),
      loadSnapshotPart('orders', access.orders, loaders.orders),
      loadSnapshotPart('pendingShipOrders', access.pendingShipOrders, loaders.pendingShipOrders),
      loadSnapshotPart('reviews', access.reviews, loaders.reviews),
    ]);

  return {
    devices: deviceResult.page.list || [],
    schedules: scheduleResult.page.list || [],
    channelOrders: orderResult.page.list || [],
    pendingShipOrders: pendingShipResult.page.list || [],
    reviews: reviewResult.page.list || [],
    totals: {
      devices: deviceResult.page.total || 0,
      schedules: scheduleResult.page.total || 0,
      channelOrders: orderResult.page.total || 0,
      pendingShipOrders: pendingShipResult.page.total || 0,
      reviews: reviewResult.page.total || 0,
    },
    failures: [
      deviceResult.failure,
      scheduleResult.failure,
      orderResult.failure,
      pendingShipResult.failure,
      reviewResult.failure,
    ].filter((source): source is keyof SnapshotLoaders => Boolean(source)),
  };
}
