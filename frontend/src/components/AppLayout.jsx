import { useEffect, useState } from 'react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { Avatar, Badge, Button, Drawer, Dropdown, Grid, Layout, Menu, Popover, List, Typography } from 'antd';
import {
  AppstoreOutlined, AuditOutlined, BarChartOutlined, BellOutlined, DashboardOutlined, DollarOutlined,
  ImportOutlined, LogoutOutlined, MenuOutlined, ShopOutlined, TeamOutlined, UserOutlined, LineChartOutlined,
} from '@ant-design/icons';
import { useAuth } from '../auth';
import api from '../api/client';
import { ROLE_LABEL, num } from '../format';

const { Header, Sider, Content } = Layout;

function menuItems(role) {
  const items = [
    { key: '/', icon: <DashboardOutlined />, label: 'Tổng quan' },
    {
      key: 'danh-muc', icon: <AppstoreOutlined />, label: 'Danh mục',
      children: [
        { key: '/mat-hang', label: 'Mặt hàng' },
        { key: '/nha-cung-cap', label: 'Nhà cung cấp' },
      ],
    },
    { key: '/nhap-hang', icon: <ImportOutlined />, label: 'Nhập hàng' },
  ];
  if (role === 'manager' || role === 'accountant') {
    items.push({ key: '/cong-no', icon: <DollarOutlined />, label: 'Công nợ' });
  }
  items.push(
    { key: '/doanh-so', icon: <LineChartOutlined />, label: 'Doanh số POS' },
    { key: '/kiem-ke', icon: <AuditOutlined />, label: 'Kiểm kê' },
    { key: '/bao-cao', icon: <BarChartOutlined />, label: 'Báo cáo' },
  );
  if (role === 'manager') items.push({ key: '/tai-khoan', icon: <TeamOutlined />, label: 'Tài khoản' });
  return items;
}

export default function AppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const screens = Grid.useBreakpoint();
  const mobile = !screens.lg;
  const [drawer, setDrawer] = useState(false);
  const [alerts, setAlerts] = useState({ lowStock: [], overSold: [] });

  useEffect(() => {
    api.get('/alerts').then((r) => setAlerts(r.data)).catch(() => {});
  }, [location.pathname]);

  const selected = '/' + (location.pathname.split('/')[1] || '');
  const alertCount = alerts.lowStock.length + alerts.overSold.length;

  const menu = (
    <>
      <div className="brand">
        <ShopOutlined style={{ fontSize: 22 }} />
        <div>
          Kho LongWaits
          <small>Tồn kho & nhà cung cấp</small>
        </div>
      </div>
      <Menu
        theme="dark"
        mode="inline"
        selectedKeys={[selected]}
        defaultOpenKeys={['danh-muc']}
        items={menuItems(user.role)}
        onClick={({ key }) => {
          navigate(key);
          setDrawer(false);
        }}
      />
    </>
  );

  const alertContent = (
    <div style={{ width: 320, maxHeight: 380, overflow: 'auto' }}>
      {alertCount === 0 && <Typography.Text type="secondary">Không có cảnh báo</Typography.Text>}
      {alerts.lowStock.length > 0 && (
        <List
          size="small"
          header={<b>Tồn thấp – cần đặt thêm hàng</b>}
          dataSource={alerts.lowStock}
          renderItem={(i) => (
            <List.Item>
              {i.code} – {i.name}
              <Typography.Text type="danger">
                {num(i.currentStock)}/{num(i.minStock)} {i.unit}
              </Typography.Text>
            </List.Item>
          )}
        />
      )}
      {alerts.overSold.length > 0 && (
        <List
          size="small"
          header={<b>Bán vượt tồn (7 ngày) – cần kiểm tra</b>}
          dataSource={alerts.overSold}
          renderItem={(s) => (
            <List.Item>
              {s.itemCode} – {s.itemName}
              <Typography.Text type="warning">vượt {num(s.overQuantity)}</Typography.Text>
            </List.Item>
          )}
        />
      )}
    </div>
  );

  return (
    <Layout style={{ minHeight: '100vh' }}>
      {!mobile && (
        <Sider width={230} style={{ position: 'sticky', top: 0, height: '100vh', overflow: 'auto' }}>
          {menu}
        </Sider>
      )}
      {mobile && (
        <Drawer placement="left" open={drawer} onClose={() => setDrawer(false)} width={240}
          styles={{ body: { padding: 0, background: '#1f2a44' }, header: { display: 'none' } }}>
          {menu}
        </Drawer>
      )}
      <Layout>
        <Header style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 16px', borderBottom: '1px solid #eee' }}>
          <div>{mobile && <Button icon={<MenuOutlined />} onClick={() => setDrawer(true)} />}</div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            <Popover content={alertContent} title="Cảnh báo" trigger="click" placement="bottomRight">
              <Badge count={alertCount} size="small">
                <Button shape="circle" icon={<BellOutlined />} aria-label="Cảnh báo" />
              </Badge>
            </Popover>
            <Dropdown
              menu={{
                items: [
                  { key: 'me', icon: <UserOutlined />, label: 'Thông tin cá nhân', onClick: () => navigate('/ca-nhan') },
                  { key: 'out', icon: <LogoutOutlined />, label: 'Đăng xuất', onClick: () => { logout(); navigate('/login'); } },
                ],
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', lineHeight: 1.2 }}>
                <Avatar style={{ background: '#b7791f' }}>{user.fullName?.[0] || 'U'}</Avatar>
                {!mobile && (
                  <div>
                    <div style={{ fontWeight: 600 }}>{user.fullName}</div>
                    <div className="muted" style={{ fontSize: 12 }}>{ROLE_LABEL[user.role]}</div>
                  </div>
                )}
              </div>
            </Dropdown>
          </div>
        </Header>
        <Content className="content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
}
