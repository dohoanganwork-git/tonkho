import { useEffect, useState } from 'react';
import { Button, Card, Col, DatePicker, Row, Select, Statistic, Table, Tabs, Tag } from 'antd';
import { DownloadOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import api from '../api/client';
import { useAuth } from '../auth';
import { STOCKTAKE_STATUS, date, exportCsv, money, num } from '../format';

function InventoryTab() {
  const { can } = useAuth();
  const showValue = can('manager', 'accountant');
  const [category, setCategory] = useState();
  const [categories, setCategories] = useState([]);
  const [data, setData] = useState(null);

  useEffect(() => { api.get('/items/categories').then((r) => setCategories(r.data)); }, []);
  useEffect(() => { api.get('/reports/inventory', { params: { category } }).then((r) => setData(r.data)); }, [category]);

  const columns = [
    { title: 'Mã', dataIndex: 'code', width: 90 },
    { title: 'Mặt hàng', dataIndex: 'name' },
    { title: 'Loại', dataIndex: 'category' },
    { title: 'ĐVT', dataIndex: 'unit', width: 70 },
    { title: 'Tồn', dataIndex: 'currentStock', align: 'right', render: (v, r) => (r.lowStock ? <Tag color="red">{num(v)}</Tag> : num(v)) },
    { title: 'Mốc cảnh báo', dataIndex: 'minStock', align: 'right', render: num },
    ...(showValue
      ? [
        { title: 'Giá nhập gần nhất', dataIndex: 'lastCost', align: 'right', render: money },
        { title: 'Giá trị tồn', dataIndex: 'stockValue', align: 'right', render: money },
      ]
      : []),
  ];

  return (
    <>
      <div className="toolbar">
        <Select placeholder="Loại hàng" allowClear style={{ width: 220 }} onChange={setCategory}
          options={categories.map((c) => ({ value: c, label: c }))} />
        <Button icon={<DownloadOutlined />} disabled={!data} onClick={() => exportCsv(`bao-cao-ton-kho-${dayjs().format('YYYYMMDD')}.csv`, [
          { title: 'Mã', value: (r) => r.code }, { title: 'Mặt hàng', value: (r) => r.name }, { title: 'Loại', value: (r) => r.category },
          { title: 'ĐVT', value: (r) => r.unit }, { title: 'Tồn', value: (r) => r.currentStock }, { title: 'Mốc cảnh báo', value: (r) => r.minStock },
          ...(showValue ? [{ title: 'Giá nhập', value: (r) => r.lastCost }, { title: 'Giá trị tồn', value: (r) => r.stockValue }] : []),
        ], data.rows)}>Xuất CSV</Button>
      </div>
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col xs={12} md={6}><Card size="small"><Statistic title="Tổng số lượng tồn" value={num(data?.totalQuantity)} /></Card></Col>
        <Col xs={12} md={6}><Card size="small"><Statistic title="Mặt hàng tồn thấp" value={data?.lowStockCount ?? '—'} /></Card></Col>
        {showValue && <Col xs={24} md={8}><Card size="small"><Statistic title="Tổng giá trị tồn (theo giá nhập gần nhất)" value={money(data?.totalValue)} /></Card></Col>}
      </Row>
      <Table rowKey="itemId" size="small" dataSource={data?.rows || []} columns={columns} scroll={{ x: 800 }} pagination={{ pageSize: 50 }} />
    </>
  );
}

function ConsumptionTab() {
  const [range, setRange] = useState([dayjs().startOf('month'), dayjs()]);
  const [data, setData] = useState(null);

  useEffect(() => {
    api.get('/reports/consumption', { params: { from: range[0].format('YYYY-MM-DD'), to: range[1].format('YYYY-MM-DD') } })
      .then((r) => setData(r.data));
  }, [range]);

  return (
    <>
      <div className="toolbar">
        <DatePicker.RangePicker value={range} format="DD/MM/YYYY" allowClear={false} onChange={(v) => v && setRange(v)} />
        <Button icon={<DownloadOutlined />} disabled={!data} onClick={() => exportCsv(`bao-cao-tieu-thu-${range[0].format('YYYYMMDD')}-${range[1].format('YYYYMMDD')}.csv`, [
          { title: 'Mã', value: (r) => r.code }, { title: 'Mặt hàng', value: (r) => r.name }, { title: 'Loại', value: (r) => r.category },
          { title: 'Số lượng bán', value: (r) => r.quantity }, { title: 'Doanh số', value: (r) => r.amount },
        ], data.rows)}>Xuất CSV</Button>
      </div>
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col xs={12} md={6}><Card size="small"><Statistic title="Tổng số lượng bán" value={num(data?.totalQuantity)} /></Card></Col>
        <Col xs={12} md={8}><Card size="small"><Statistic title="Tổng doanh số" value={money(data?.totalAmount)} /></Card></Col>
      </Row>
      <Table rowKey="itemId" size="small" dataSource={data?.rows || []} scroll={{ x: 700 }} pagination={{ pageSize: 50 }}
        locale={{ emptyText: 'Chưa có doanh số POS trong khoảng thời gian này' }}
        columns={[
          { title: '#', width: 50, render: (_, __, i) => i + 1 },
          { title: 'Mã', dataIndex: 'code', width: 90 },
          { title: 'Mặt hàng', dataIndex: 'name' },
          { title: 'Loại', dataIndex: 'category' },
          { title: 'Số lượng bán', dataIndex: 'quantity', align: 'right', render: (v, r) => `${num(v)} ${r.unit}` },
          { title: 'Doanh số', dataIndex: 'amount', align: 'right', render: money },
        ]} />
    </>
  );
}

function AccuracyTab() {
  const [rows, setRows] = useState([]);
  useEffect(() => { api.get('/reports/stocktakes').then((r) => setRows(r.data)); }, []);
  const avg = rows.length ? (rows.reduce((s, r) => s + r.accuracyPercent, 0) / rows.length).toFixed(1) : null;
  return (
    <>
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col xs={12} md={6}><Card size="small"><Statistic title="Số đợt kiểm kê" value={rows.length} /></Card></Col>
        <Col xs={12} md={6}><Card size="small"><Statistic title="Tỉ lệ khớp trung bình" value={avg ?? '—'} suffix={avg ? '%' : ''} /></Card></Col>
      </Row>
      <Table rowKey="stocktakeId" size="small" dataSource={rows} scroll={{ x: 800 }}
        columns={[
          { title: 'Ngày kiểm kê', dataIndex: 'stocktakeDate', render: date },
          { title: 'Trạng thái', dataIndex: 'status', render: (v) => <Tag color={STOCKTAKE_STATUS[v]?.color}>{STOCKTAKE_STATUS[v]?.text}</Tag> },
          { title: 'Số mặt hàng', dataIndex: 'itemCount', align: 'right' },
          { title: 'Khớp', dataIndex: 'matchedCount', align: 'right' },
          { title: 'Tỉ lệ chính xác', dataIndex: 'accuracyPercent', align: 'right', render: (v) => <Tag color={v >= 95 ? 'green' : v >= 80 ? 'orange' : 'red'}>{v}%</Tag> },
          { title: 'Tổng lệch (tuyệt đối)', dataIndex: 'totalAbsVariance', align: 'right', render: num },
          { title: 'Hỏng/hủy', dataIndex: 'damagedTotal', align: 'right', render: num },
        ]} />
    </>
  );
}

export default function ReportsPage() {
  return (
    <>
      <div className="page-header"><h2>Báo cáo</h2></div>
      <Card>
        <Tabs items={[
          { key: 'inv', label: 'Tồn kho', children: <InventoryTab /> },
          { key: 'cons', label: 'Tiêu thụ (theo POS)', children: <ConsumptionTab /> },
          { key: 'acc', label: 'Kiểm kê & độ chính xác kho', children: <AccuracyTab /> },
        ]} />
      </Card>
    </>
  );
}
