export const SCHEDULE_CENTER_ACCESS_PERMISSIONS = [
  'rental:schedule-center:access',
  'rental:device:query',
  'rental:schedule:query',
  'rental:xianyu:query',
  'rental:xianyu:ship',
  'rental:review:query',
] as const;

export function permissionAllows(
  permissions: string[],
  required: string | string[]
) {
  const requiredList = Array.isArray(required) ? required : [required];
  return permissions.includes('*:*:*')
    || requiredList.some((permission) => permissions.includes(permission));
}

export function hasScheduleCenterAccess(permissions: string[]) {
  return permissionAllows(permissions, [...SCHEDULE_CENTER_ACCESS_PERMISSIONS]);
}
