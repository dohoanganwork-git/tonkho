import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { App, Button, DatePicker, Form, Modal, Select, Table, Tag, Typography, Upload } from 'antd';
import { DownloadOutlined, UploadOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import api, { errorDetails, errorMessage } from '../api/client';
import { useAuth } from '../auth';
import { STOCKTAKE_STATUS, date, dateTime } from '../format';
import ErrorList from '../components/ErrorList';

/** Hop thoai tai file kiem ke; dung chung cho lan dau va kiem ke lai (parentId). */
export function StocktakeUploadModal({ open, parent, onClose, onDone }) {
  const { message } = App.useApp();
  const [countFile, setCountFile] = useState(null);
  const [damageFile, setDamageFile] = useState(null);
  const [day, setDay] = useState(dayjs());
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (open) {
      setCountFile(null);
      setDamageFile(null);
      setDay(dayjs());
      setError(null);
    }
  }, [open]);

  const submit = async () => {
    if (!countFile) return message.error('Chọn file tồn thực tế');
    const fd = new FormData();
    fd.append('stocktakeDate', day.format('YYYY-MM-DD'));
    fd.append('countFile', countFile);
    if (damageFile) fd.append('damageFile', damageFile);
    if (parent) fd.append('parentId', parent.id);
    setSaving(true);
    setError(null);
    try {
      const { data } = await api.post('/stocktakes', fd);
      message.success(data.summary.stocktake.status === 'no_variance'
        ? 'Không có chênh lệch, đợt kiểm kê đã hoàn tất'
        : 'Đã gửi kết quả kiểm kê, chờ Quản lý xem xét chênh lệch');
      onDone(data.summary.stocktake.id);
    } catch (e) {
      setError({ message: errorMessage(e), details: errorDetails(e) });
    } finally {
      setSaving(false);
    }
  };

  const picker = (file, setFile) => (
    <Upload beforeUpload={(f) => { setFile(f); return false; }} onRemove={() => setFile(null)} maxCount={1}
      fileList={file ? [{ uid: '1', name: file.name, status: 'done' }] : []} accept=".csv,.xlsx,.xls">
      <Button icon={<UploadOutlined />}>Chọn file</Button>
    </Upload>
  );

  return (
    <Modal open={open} title={parent ? 'Gửi kết quả kiểm kê lại' : 'Tải file kiểm kê'} okText="Gửi đối soát" cancelText="Hủy"
      confirmLoading={saving} onOk={submit} onCancel={onClose} width={620} destroyOnClose>
      <ErrorList error={error} />
      <Typography.Paragraph type="secondary">
        {parent
          ? 'Kiểm kê lại các mặt hàng bị lệch ở đợt trước. File cần có đủ các mặt hàng lệch.'
          : 'Đếm hàng thực tế, ghi vào file theo mẫu rồi tải lên. Hệ thống so với tồn sổ sách và đánh dấu chênh lệch; tồn kho chỉ thay đổi khi Quản lý đồng ý.'}
      </Typography.Paragraph>
      <Typography.Paragraph>
        <a href="/mau/kiem-ke-ton-thuc-te-mau.csv" download><DownloadOutlined /> File mẫu tồn thực tế</a>
        {'  ·  '}
        <a href="/mau/kiem-ke-hong-huy-mau.csv" download><DownloadOutlined /> File mẫu hỏng/hủy</a>
      </Typography.Paragraph>
      <Form layout="vertical">
        <Form.Item label="Ngày kiểm kê" required>
          <DatePicker value={day} onChange={setDay} format="DD/MM/YYYY" allowClear={false} disabledDate={(d) => d.isAfter(dayjs(), 'day')} />
        </Form.Item>
        <Form.Item label="File tồn thực tế (Mã hàng, Tồn thực tế)" required>{picker(countFile, setCountFile)}</Form.Item>
        <Form.Item label="File hỏng/hủy (Mã hàng, Số lượng, Lý do) – nếu có">{picker(damageFile, setDamageFile)}</Form.Item>
      </Form>
    </Modal>
  );
}

export default function StocktakesPage() {
  const { can } = useAuth();
  const navigate = useNavigate();
  const [rows, setRows] = useState([]);
  const [status, setStatus] = useState();
  const [open, setOpen] = useState(false);

  useEffect(() => {
    api.get('/stocktakes', { params: { status } }).then((r) => setRows(r.data));
  }, [status]);

  return (
    <>
      <div className="page-header">
        <h2>Kiểm kê và đối soát tồn kho</h2>
        {can('staff', 'manager') && <Button type="primary" icon={<UploadOutlined />} onClick={() => setOpen(true)}>Tải file kiểm kê</Button>}
      </div>
      <div className="toolbar">
        <Select placeholder="Trạng thái" allowClear style={{ width: 220 }} onChange={setStatus}
          options={Object.entries(STOCKTAKE_STATUS).map(([value, o]) => ({ value, label: o.text }))} />
      </div>
      <Table
        rowKey={(r) => r.stocktake.id}
        dataSource={rows}
        scroll={{ x: 900 }}
        onRow={(r) => ({ onClick: () => navigate(`/kiem-ke/${r.stocktake.id}`), style: { cursor: 'pointer' } })}
        columns={[
          { title: 'Ngày kiểm kê', render: (_, r) => date(r.stocktake.stocktakeDate) },
          { title: 'Loại', render: (_, r) => (r.stocktake.parentId ? <Tag color="purple">Kiểm kê lại</Tag> : <Tag>Định kỳ</Tag>) },
          { title: 'Số mặt hàng', dataIndex: 'itemCount', align: 'right' },
          { title: 'Lệch', dataIndex: 'varianceCount', align: 'right', render: (v) => (v > 0 ? <Tag color="red">{v}</Tag> : 0) },
          { title: 'Hỏng/hủy', dataIndex: 'damagedTotal', align: 'right' },
          { title: 'Trạng thái', render: (_, r) => <Tag color={STOCKTAKE_STATUS[r.stocktake.status]?.color}>{STOCKTAKE_STATUS[r.stocktake.status]?.text}</Tag> },
          { title: 'Người kiểm kê', dataIndex: 'createdByName' },
          { title: 'Gửi lúc', render: (_, r) => dateTime(r.stocktake.createdAt) },
        ]}
      />
      <StocktakeUploadModal open={open} onClose={() => setOpen(false)} onDone={(id) => { setOpen(false); navigate(`/kiem-ke/${id}`); }} />
    </>
  );
}
