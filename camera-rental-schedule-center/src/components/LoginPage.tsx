import { UnifiedAdminLogin } from '../features/auth/UnifiedAdminLogin';

export function LoginPage({ isModal = false }: { isModal?: boolean }) {
  return <UnifiedAdminLogin isModal={isModal} />;
}
