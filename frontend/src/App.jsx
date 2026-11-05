import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth';
import AppLayout from './components/AppLayout';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import ItemsPage from './pages/ItemsPage';
import SuppliersPage from './pages/SuppliersPage';
import ReceiptsPage from './pages/ReceiptsPage';
import ReceiptFormPage from './pages/ReceiptFormPage';
import ReceiptDetailPage from './pages/ReceiptDetailPage';
import DebtsPage from './pages/DebtsPage';
import SalesPage from './pages/SalesPage';
import StocktakesPage from './pages/StocktakesPage';
import StocktakeDetailPage from './pages/StocktakeDetailPage';
import ReportsPage from './pages/ReportsPage';
import UsersPage from './pages/UsersPage';
import ProfilePage from './pages/ProfilePage';

function Guard({ roles, children }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />;
  return children;
}

export default function App() {
  const { user } = useAuth();
  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to="/" replace /> : <LoginPage />} />
      <Route
        element={
          <Guard>
            <AppLayout />
          </Guard>
        }
      >
        <Route index element={<DashboardPage />} />
        <Route path="mat-hang" element={<ItemsPage />} />
        <Route path="nha-cung-cap" element={<SuppliersPage />} />
        <Route path="nhap-hang" element={<ReceiptsPage />} />
        <Route path="nhap-hang/moi" element={<Guard roles={['staff', 'manager']}><ReceiptFormPage /></Guard>} />
        <Route path="nhap-hang/:id" element={<ReceiptDetailPage />} />
        <Route path="nhap-hang/:id/sua" element={<Guard roles={['manager']}><ReceiptFormPage /></Guard>} />
        <Route path="cong-no" element={<Guard roles={['manager', 'accountant']}><DebtsPage /></Guard>} />
        <Route path="doanh-so" element={<SalesPage />} />
        <Route path="kiem-ke" element={<StocktakesPage />} />
        <Route path="kiem-ke/:id" element={<StocktakeDetailPage />} />
        <Route path="bao-cao" element={<ReportsPage />} />
        <Route path="tai-khoan" element={<Guard roles={['manager']}><UsersPage /></Guard>} />
        <Route path="ca-nhan" element={<ProfilePage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
