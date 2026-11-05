import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, Col, Row, Statistic, Table, Tag, Typography, Button, Space } from 'antd';
import { WarningOutlined } from '@ant-design/icons';
import api from '../api/client';
import { useAuth } from '../auth';
import { date, money, num } from '../format';

export default function DashboardPage() {
  const { user, can } = useAuth();
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [alerts, setAlerts] = useState({ lowStock: [], overSold: [] });

  useEffect(() => {
    api.get('/reports/dashboard').then((r) => setData(r.data));
    api.get('/alerts').then((r) => setAlerts(r.data));
  }, []);

  return (
    <>
      <div className="page-header">
        <h2>Xin chào, {user.fullName}</h2>
        <Space wrap>
          {can('staff', 'manager') && <Button type="primary" onClick={() => navigate('/nhap-hang/moi')}>Lập phiếu nhập</Button>}
          {can('manager') && <Button onClick={() => navigate('/doanh-so')}>Nhập doanh số POS</Button>}
          {can('staff', 'manager') && <Button onClick={() => navigate('/kiem-ke')}>Kiểm kê</Button>}
        </Space>
      </div>
      <Row gutter={[16, 16]}>
        <Col xs={12} md={6}><Card><Statistic title="Mặt hàng đang kinh doanh" value={data?.itemCount ?? '—'} /></Card></Col>
        <Col xs={12} md={6}>
          <Card><Statistic title="Mặt hàng tồn thấp" value={data?.lowStockCount ?? '—'} valueStyle={{ color: data?.lowStockCount ? '#cf1322' : undefined }} prefix={data?.lowStockCount ? <WarningOutlined /> : null} /></Card>
        </Col>
        {can('manager', 'accountant') && (
          <Col xs={12} md={6}><Card><Statistic title={`Công nợ còn lại (${data?.unpaidReceipts ?? 0} phiếu)`} value={money(data?.debtRemaining)} /></Card></Col>
        )}
        <Col xs={12} md={6}><Card><Statistic title="Đợt kiểm kê chờ duyệt" value={data?.pendingStocktakes ?? '—'} /></Card></Col>
      </Row>
      <Typography.Paragraph type="secondary" style={{ marginTop: 12 }}>
        Doanh số POS gần nhất: {date(data?.lastSalesDate)}
      </Typography.Paragraph>
      <Card title="Mặt hàng tồn thấp – cần đặt thêm hàng" style={{ marginTop: 8 }}>
        <Table
          rowKey="id"
          size="small"
          pagination={false}
          dataSource={alerts.lowStock}
          locale={{ emptyText: 'Không có mặt hàng nào dưới mốc cảnh báo' }}
          scroll={{ x: 520 }}
          columns={[
            { title: 'Mã', dataIndex: 'code', width: 90 },
            { title: 'Mặt hàng', dataIndex: 'name' },
            { title: 'Loại', dataIndex: 'category', responsive: ['md'] },
            { title: 'Tồn', dataIndex: 'currentStock', align: 'right', render: (v, r) => <Tag color={v === 0 ? 'red' : 'orange'}>{num(v)} {r.unit}</Tag> },
            { title: 'Mốc cảnh báo', dataIndex: 'minStock', align: 'right', render: num },
          ]}
        />
      </Card>
      {alerts.overSold.length > 0 && (
        <Card title="Bán vượt tồn sổ sách (7 ngày gần nhất)" style={{ marginTop: 16 }}>
          <Table
            rowKey="id"
            size="small"
            pagination={false}
            dataSource={alerts.overSold}
            scroll={{ x: 520 }}
            columns={[
              { title: 'Ngày', dataIndex: 'salesDate', render: date },
              { title: 'Mặt hàng', render: (_, r) => `${r.itemCode} – ${r.itemName}` },
              { title: 'Bán', dataIndex: 'quantity', align: 'right', render: num },
              { title: 'Tồn trước', dataIndex: 'stockBefore', align: 'right', render: num },
              { title: 'Vượt', dataIndex: 'overQuantity', align: 'right', render: (v) => <Tag color="red">{num(v)}</Tag> },
            ]}
          />
        </Card>
      )}
    </>
  );
}
