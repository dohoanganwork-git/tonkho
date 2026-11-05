import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Alert, App, Button, Card, Descriptions, Space, Spin, Table, Tag } from 'antd';
import api, { errorMessage } from '../api/client';
import { useAuth } from '../auth';
import { STOCKTAKE_STATUS, date, dateTime, num } from '../format';
import FileLink from '../components/FileLink';
import ReasonModal from '../components/ReasonModal';
import { StocktakeUploadModal } from './StocktakesPage';

export default function StocktakeDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { can } = useAuth();
  const { message } = App.useApp();
  const [data, setData] = useState(null);
  const [modal, setModal] = useState(null);
  const [busy, setBusy] = useState(false);
  const [onlyVariance, setOnlyVariance] = useState(false);

  const load = useCallback(() => api.get(`/stocktakes/${id}`).then((r) => setData(r.data)), [id]);
  useEffect(() => { load(); }, [load]);
  if (!data) return <Spin />;

  const s = data.summary.stocktake;
  const pending = s.status === 'pending_review';

  const decide = async (action, reason) => {
    setBusy(true);
    try {
      await api.post(`/stocktakes/${id}/${action}`, { reason });
      message.success(action === 'approve' ? 'Đã điều chỉnh tồn kho theo kết quả kiểm kê' : 'Đã yêu cầu kiểm kê lại các mặt hàng lệch');
      setModal(null);
      load();
    } catch (e) {
      message.error(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const items = onlyVariance ? data.items.filter((i) => i.varianceQty !== 0) : data.items;

  return (
    <>
      <div className="page-header">
        <h2>Đợt kiểm kê ngày {date(s.stocktakeDate)}</h2>
        <Space wrap>
          <Button onClick={() => navigate('/kiem-ke')}>Danh sách</Button>
          {s.parentId && <Button onClick={() => navigate(`/kiem-ke/${s.parentId}`)}>Xem đợt trước</Button>}
          {can('manager') && pending && (
            <>
              <Button type="primary" onClick={() => setModal('approve')}>Đồng ý điều chỉnh tồn</Button>
              <Button danger onClick={() => setModal('recount')}>Yêu cầu kiểm kê lại</Button>
            </>
          )}
          {can('staff', 'manager') && s.status === 'recount_requested' && !data.recountSubmitted && (
            <Button type="primary" onClick={() => setModal('upload')}>Gửi kết quả kiểm kê lại</Button>
          )}
        </Space>
      </div>
      {pending && (
        <Alert type="warning" showIcon style={{ marginBottom: 16 }}
          message={`Có ${data.summary.varianceCount} mặt hàng chênh lệch giữa tồn thực tế và tồn sổ sách`}
          description="Kết quả kiểm kê chưa được ghi nhận vào tồn kho. Quản lý xem xét và đồng ý điều chỉnh, hoặc yêu cầu kiểm kê lại." />
      )}
      {s.status === 'recount_requested' && (
        <Alert type="error" showIcon style={{ marginBottom: 16 }} message="Quản lý yêu cầu kiểm kê lại các mặt hàng lệch"
          description={`Lý do: ${s.decisionReason}${data.recountSubmitted ? ' · Đã gửi kết quả kiểm kê lại' : ''}`} />
      )}
      <Card>
        <Descriptions bordered size="small" column={{ xs: 1, md: 2 }}>
          <Descriptions.Item label="Trạng thái"><Tag color={STOCKTAKE_STATUS[s.status]?.color}>{STOCKTAKE_STATUS[s.status]?.text}</Tag></Descriptions.Item>
          <Descriptions.Item label="Người kiểm kê">{data.summary.createdByName} · {dateTime(s.createdAt)}</Descriptions.Item>
          <Descriptions.Item label="File tồn thực tế"><FileLink path={s.countFileUrl} /></Descriptions.Item>
          <Descriptions.Item label="File hỏng/hủy"><FileLink path={s.damageFileUrl} /></Descriptions.Item>
          <Descriptions.Item label="Người quyết định">{data.summary.reviewedByName || '—'} {s.decidedAt ? `· ${dateTime(s.decidedAt)}` : ''}</Descriptions.Item>
          <Descriptions.Item label="Lý do quyết định">{s.decisionReason || '—'}</Descriptions.Item>
        </Descriptions>
      </Card>
      <Card title="Kết quả đối soát" style={{ marginTop: 16 }}
        extra={<Button size="small" onClick={() => setOnlyVariance((v) => !v)}>{onlyVariance ? 'Hiện tất cả' : 'Chỉ mặt hàng lệch'}</Button>}>
        <Table rowKey="id" size="small" pagination={false} dataSource={items} scroll={{ x: 800 }}
          rowClassName={(r) => (r.varianceQty !== 0 ? 'row-variance' : '')}
          columns={[
            { title: 'Mã', dataIndex: 'itemCode', width: 90 },
            { title: 'Mặt hàng', dataIndex: 'itemName' },
            { title: 'ĐVT', dataIndex: 'unit', width: 70 },
            { title: 'Tồn sổ sách', dataIndex: 'bookQty', align: 'right', render: num },
            { title: 'Tồn thực tế', dataIndex: 'countedQty', align: 'right', render: num },
            {
              title: 'Chênh lệch', dataIndex: 'varianceQty', align: 'right',
              render: (v) => (v === 0 ? <Tag color="green">Khớp</Tag> : <Tag color={v < 0 ? 'red' : 'blue'}>{v > 0 ? '+' : ''}{num(v)}</Tag>),
            },
            { title: 'Hỏng/hủy', dataIndex: 'damagedQty', align: 'right', render: (v) => (v ? num(v) : '') },
            { title: 'Lý do hỏng/hủy', dataIndex: 'damageReason' },
          ]} />
      </Card>
      <ReasonModal open={modal === 'approve'} title="Đồng ý điều chỉnh tồn kho" label="Lý do đồng ý" okText="Điều chỉnh tồn"
        loading={busy} onOk={(r) => decide('approve', r)} onCancel={() => setModal(null)} />
      <ReasonModal open={modal === 'recount'} title="Yêu cầu kiểm kê lại" label="Lý do không đồng ý" okText="Yêu cầu kiểm kê lại" danger
        loading={busy} onOk={(r) => decide('recount', r)} onCancel={() => setModal(null)} />
      <StocktakeUploadModal open={modal === 'upload'} parent={s} onClose={() => setModal(null)}
        onDone={(newId) => { setModal(null); navigate(`/kiem-ke/${newId}`); }} />
    </>
  );
}
