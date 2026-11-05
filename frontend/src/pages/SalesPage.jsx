import { useEffect, useState } from 'react';
import { App, Alert, Button, Card, DatePicker, Drawer, Form, Modal, Table, Tag, Typography, Upload } from 'antd';
import { DownloadOutlined, UploadOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import api, { errorDetails, errorMessage } from '../api/client';
import { useAuth } from '../auth';
import { date, dateTime, money, num } from '../format';
import FileLink from '../components/FileLink';
import ErrorList from '../components/ErrorList';

const lineColumns = [
  { title: 'Mã', dataIndex: 'itemCode', width: 90 },
  { title: 'Mặt hàng', dataIndex: 'itemName' },
  { title: 'Bán', dataIndex: 'quantity', align: 'right', render: num },
  { title: 'Đơn giá', dataIndex: 'unitPrice', align: 'right', render: money },
  { title: 'Thành tiền', dataIndex: 'amount', align: 'right', render: money },
  { title: 'Tồn trước', dataIndex: 'stockBefore', align: 'right', render: num },
  { title: 'Tồn sau', dataIndex: 'stockAfter', align: 'right', render: num },
  { title: 'Vượt tồn', dataIndex: 'overQuantity', align: 'right', render: (v) => (v > 0 ? <Tag color="red">{num(v)}</Tag> : '') },
];

export default function SalesPage() {
  const { can } = useAuth();
  const { message } = App.useApp();
  const [imports, setImports] = useState([]);
  const [open, setOpen] = useState(false);
  const [file, setFile] = useState(null);
  const [salesDate, setSalesDate] = useState(dayjs());
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [result, setResult] = useState(null);
  const [viewing, setViewing] = useState(null);

  const load = () => api.get('/sales/imports').then((r) => setImports(r.data));
  useEffect(() => { load(); }, []);

  const submit = async () => {
    if (!file) return message.error('Chọn file doanh số xuất từ POS');
    const fd = new FormData();
    fd.append('salesDate', salesDate.format('YYYY-MM-DD'));
    fd.append('file', file);
    setSaving(true);
    setError(null);
    try {
      const { data } = await api.post('/sales/import', fd);
      setResult(data);
      setOpen(false);
      setFile(null);
      message.success('Đã nhập doanh số và trừ tồn kho');
      load();
    } catch (e) {
      setError({ message: errorMessage(e), details: errorDetails(e) });
    } finally {
      setSaving(false);
    }
  };

  const view = async (d) => {
    const { data } = await api.get(`/sales/imports/${d}`);
    setViewing({ date: d, lines: data });
  };

  return (
    <>
      <div className="page-header">
        <h2>Doanh số bán hàng từ POS</h2>
        {can('manager') && (
          <Button type="primary" icon={<UploadOutlined />} onClick={() => { setOpen(true); setError(null); setSalesDate(dayjs()); }}>
            Nhập file POS cuối ngày
          </Button>
        )}
      </div>

      {result && (
        <Card title={`Kết quả nhập ngày ${date(result.summary.salesDate)}`} style={{ marginBottom: 16 }}
          extra={<Button size="small" onClick={() => setResult(null)}>Đóng</Button>}>
          {result.summary.overCount > 0 && (
            <Alert type="warning" showIcon style={{ marginBottom: 12 }}
              message={`${result.summary.overCount} mặt hàng bán vượt tồn sổ sách – tồn đã được giữ ở 0, cần kiểm tra lại`} />
          )}
          {result.lowStock.length > 0 && (
            <Alert type="error" showIcon style={{ marginBottom: 12 }}
              message="Mặt hàng xuống dưới mốc cảnh báo – cần đặt thêm hàng"
              description={result.lowStock.map((i) => `${i.code} – ${i.name} (còn ${i.currentStock} ${i.unit})`).join('; ')} />
          )}
          <Table rowKey="id" size="small" pagination={false} dataSource={result.lines} columns={lineColumns} scroll={{ x: 800 }} />
        </Card>
      )}

      <Table
        rowKey="salesDate"
        dataSource={imports}
        scroll={{ x: 800 }}
        columns={[
          { title: 'Ngày bán', dataIndex: 'salesDate', render: (v) => <a onClick={() => view(v)}>{date(v)}</a> },
          { title: 'Số mặt hàng', dataIndex: 'lineCount', align: 'right' },
          { title: 'Tổng số lượng', dataIndex: 'totalQuantity', align: 'right', render: num },
          { title: 'Doanh số', dataIndex: 'totalAmount', align: 'right', render: money },
          { title: 'Vượt tồn', dataIndex: 'overCount', align: 'right', render: (v) => (v > 0 ? <Tag color="red">{v}</Tag> : 0) },
          { title: 'File gốc', dataIndex: 'originalFileUrl', render: (v) => <FileLink path={v}>Tải</FileLink> },
          { title: 'Người nhập', dataIndex: 'importedByName' },
          { title: 'Thời điểm', dataIndex: 'importedAt', render: dateTime },
        ]}
      />

      <Modal open={open} title="Nhập doanh số POS cuối ngày" okText="Nhập và trừ tồn kho" cancelText="Hủy"
        confirmLoading={saving} onOk={submit} onCancel={() => setOpen(false)} width={620} destroyOnClose>
        <ErrorList error={error} />
        <Typography.Paragraph type="secondary">
          Xuất báo cáo bán hàng theo mặt hàng từ Sapo POS (file .xlsx hoặc .csv) với các cột <b>Mã POS</b>, <b>Số lượng</b>,
          và có thể có <b>Đơn giá</b>, <b>Ngày</b>. Hệ thống kiểm tra định dạng, ngày đã nhập và mã hàng chưa có trong danh mục
          trước khi ghi nhận.
        </Typography.Paragraph>
        <Typography.Paragraph>
          <a href="/mau/doanh-so-pos-mau.csv" download><DownloadOutlined /> Tải file mẫu doanh số POS</a>
        </Typography.Paragraph>
        <Form layout="vertical">
          <Form.Item label="Ngày bán" required>
            <DatePicker value={salesDate} onChange={setSalesDate} format="DD/MM/YYYY" allowClear={false}
              disabledDate={(d) => d.isAfter(dayjs(), 'day')} />
          </Form.Item>
          <Form.Item label="File doanh số POS" required>
            <Upload beforeUpload={(f) => { setFile(f); return false; }} onRemove={() => setFile(null)} maxCount={1}
              fileList={file ? [{ uid: '1', name: file.name, status: 'done' }] : []} accept=".csv,.xlsx,.xls">
              <Button icon={<UploadOutlined />}>Chọn file</Button>
            </Upload>
          </Form.Item>
        </Form>
      </Modal>

      <Drawer open={!!viewing} onClose={() => setViewing(null)} title={`Doanh số ngày ${date(viewing?.date)}`} width={Math.min(900, window.innerWidth)}>
        <Table rowKey="id" size="small" pagination={false} dataSource={viewing?.lines || []} columns={lineColumns} scroll={{ x: 800 }} />
      </Drawer>
    </>
  );
}
