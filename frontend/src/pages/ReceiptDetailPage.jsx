import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Alert, App, Button, Card, Descriptions, Space, Table, Tag, Upload, Spin } from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import api, { errorMessage } from '../api/client';
import { useAuth } from '../auth';
import { DEBT_STATUS, RECON_STATUS, date, dateTime, money, num } from '../format';
import FileLink from '../components/FileLink';
import ReasonModal from '../components/ReasonModal';
import PaymentModal from '../components/PaymentModal';
import ReconcileModal from '../components/ReconcileModal';

export default function ReceiptDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { can } = useAuth();
  const { message } = App.useApp();
  const [data, setData] = useState(null);
  const [modal, setModal] = useState(null); // 'cancel' | 'pay' | 'reconcile' | {cancelPayment} | {editPayment}
  const [busy, setBusy] = useState(false);

  const load = useCallback(() => api.get(`/receipts/${id}`).then((r) => setData(r.data)), [id]);
  useEffect(() => { load(); }, [load]);

  if (!data) return <Spin />;
  const r = data.receipt;
  const cancelled = r.status === 'cancelled';
  const locked = r.reconcileStatus === 'reconciled';

  const cancelReceipt = async (reason) => {
    setBusy(true);
    try {
      await api.post(`/receipts/${id}/cancel`, { reason });
      message.success('Đã hủy phiếu nhập và trừ lại tồn kho');
      setModal(null);
      load();
    } catch (e) {
      message.error(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const cancelPayment = async (reason) => {
    setBusy(true);
    try {
      await api.post(`/payments/${modal.cancelPayment.id}/cancel`, { reason });
      message.success('Đã hủy khoản thanh toán');
      setModal(null);
      load();
    } catch (e) {
      message.error(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const uploadInvoice = async (file) => {
    const fd = new FormData();
    fd.append('file', file);
    try {
      await api.post(`/receipts/${id}/invoice-file`, fd);
      message.success('Đã tải file hóa đơn');
      load();
    } catch (e) {
      message.error(errorMessage(e));
    }
    return false;
  };

  return (
    <>
      <div className="page-header">
        <h2>Phiếu nhập {r.code}</h2>
        <Space wrap>
          <Button onClick={() => navigate('/nhap-hang')}>Danh sách</Button>
          {can('manager') && !cancelled && !locked && (
            <>
              <Button onClick={() => navigate(`/nhap-hang/${id}/sua`)}>Sửa phiếu</Button>
              <Button danger onClick={() => setModal('cancel')}>Hủy phiếu</Button>
            </>
          )}
          {can('accountant') && !cancelled && !locked && Number(r.remainingAmount) > 0 && (
            <Button type="primary" onClick={() => setModal('pay')}>Thanh toán</Button>
          )}
          {can('accountant') && !cancelled && !locked && (
            <Button onClick={() => setModal('reconcile')}>Đối soát</Button>
          )}
        </Space>
      </div>
      {cancelled && <Alert type="warning" showIcon style={{ marginBottom: 16 }} message={`Phiếu đã bị hủy bởi ${data.cancelledByName || ''}`} description={`Lý do: ${r.cancelReason}`} />}
      {locked && <Alert type="success" showIcon style={{ marginBottom: 16 }} message={`Đã đối soát khớp lúc ${dateTime(r.reconciledAt)} – phiếu đã khóa`} />}
      <Card>
        <Descriptions bordered size="small" column={{ xs: 1, md: 2 }}>
          <Descriptions.Item label="Nhà cung cấp">{data.supplierName}</Descriptions.Item>
          <Descriptions.Item label="Ngày nhập">{date(r.receiptDate)}</Descriptions.Item>
          <Descriptions.Item label="Số hóa đơn">{r.invoiceNo || '—'}</Descriptions.Item>
          <Descriptions.Item label="File hóa đơn">
            <FileLink path={r.invoiceFileUrl} />
            {!cancelled && !locked && (
              <Upload beforeUpload={uploadInvoice} showUploadList={false} accept=".pdf,.png,.jpg,.jpeg,.webp">
                <Button size="small" icon={<UploadOutlined />} style={{ marginLeft: 8 }}>Tải lên</Button>
              </Upload>
            )}
          </Descriptions.Item>
          <Descriptions.Item label="Người lập">{data.createdByName} · {dateTime(r.createdAt)}</Descriptions.Item>
          <Descriptions.Item label="Tổng tiền"><b>{money(r.totalAmount)}</b></Descriptions.Item>
          <Descriptions.Item label="Đã thanh toán">{money(r.paidAmount)}</Descriptions.Item>
          <Descriptions.Item label="Còn nợ"><b>{money(r.remainingAmount)}</b></Descriptions.Item>
          <Descriptions.Item label="Công nợ"><Tag color={DEBT_STATUS[r.debtStatus]?.color}>{DEBT_STATUS[r.debtStatus]?.text}</Tag></Descriptions.Item>
          <Descriptions.Item label="Đối soát">
            <Tag color={RECON_STATUS[r.reconcileStatus]?.color}>{RECON_STATUS[r.reconcileStatus]?.text}</Tag>
            {r.reconcileNote && <span className="muted">{r.reconcileNote}</span>}
          </Descriptions.Item>
        </Descriptions>
      </Card>
      <Card title="Mặt hàng" style={{ marginTop: 16 }}>
        <Table rowKey="id" size="small" pagination={false} dataSource={data.lines} scroll={{ x: 640 }}
          columns={[
            { title: 'Mã', dataIndex: 'itemCode', width: 90 },
            { title: 'Mặt hàng', dataIndex: 'itemName' },
            { title: 'ĐVT', dataIndex: 'unit', width: 70 },
            { title: 'Số lượng', dataIndex: 'quantity', align: 'right', render: num },
            { title: 'Đơn giá', dataIndex: 'unitPrice', align: 'right', render: money },
            { title: 'Thành tiền', dataIndex: 'lineAmount', align: 'right', render: money },
          ]} />
      </Card>
      {can('manager', 'accountant') && (
        <Card title="Lịch sử thanh toán" style={{ marginTop: 16 }}>
          <Table rowKey={(p) => p.payment.id} size="small" pagination={false} dataSource={data.payments} scroll={{ x: 760 }}
            locale={{ emptyText: 'Chưa có khoản thanh toán nào' }}
            columns={[
              { title: 'Ngày', render: (_, p) => dateTime(p.payment.paidAt), width: 140 },
              { title: 'Số tiền', align: 'right', render: (_, p) => money(p.payment.amount) },
              { title: 'Hình thức', render: (_, p) => p.payment.method },
              { title: 'Minh chứng', render: (_, p) => <FileLink path={p.payment.evidenceFileUrl}>Xem</FileLink> },
              { title: 'Trạng thái', render: (_, p) => (p.payment.status === 'cancelled' ? <Tag>Đã hủy</Tag> : <Tag color="green">Hiệu lực</Tag>) },
              { title: 'Ghi chú', render: (_, p) => p.payment.note || '' },
              { title: 'Kế toán', dataIndex: 'createdByName' },
              ...(can('accountant') && !locked
                ? [{
                  title: '', width: 120,
                  render: (_, p) => p.payment.status === 'active' && (
                    <Space size={0}>
                      <Button size="small" type="link" onClick={() => setModal({ editPayment: p.payment })}>Sửa</Button>
                      <Button size="small" type="link" danger onClick={() => setModal({ cancelPayment: p.payment })}>Hủy</Button>
                    </Space>
                  ),
                }]
                : []),
            ]} />
        </Card>
      )}

      <ReasonModal open={modal === 'cancel'} title="Hủy phiếu nhập" label="Lý do hủy phiếu" okText="Hủy phiếu" danger loading={busy}
        onOk={cancelReceipt} onCancel={() => setModal(null)} />
      <ReasonModal open={!!modal?.cancelPayment} title="Hủy khoản thanh toán" label="Lý do hủy" okText="Hủy thanh toán" danger loading={busy}
        onOk={cancelPayment} onCancel={() => setModal(null)} />
      <PaymentModal open={modal === 'pay' || !!modal?.editPayment} receipt={r} payment={modal?.editPayment}
        onClose={() => setModal(null)} onSaved={load} />
      <ReconcileModal open={modal === 'reconcile'} receipt={r} onClose={() => setModal(null)} onSaved={load} />
    </>
  );
}
