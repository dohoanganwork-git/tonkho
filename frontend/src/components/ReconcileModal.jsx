import { useEffect, useState } from 'react';
import { App, Form, Input, Modal, Radio, Upload, Typography } from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import api, { errorMessage } from '../api/client';
import { money } from '../format';

/** Ke toan doi soat cong no cua mot phieu nhap voi hoa don / minh chung. */
export default function ReconcileModal({ open, receipt, onClose, onSaved }) {
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const [file, setFile] = useState(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (open) {
      setFile(null);
      form.setFieldsValue({ result: 'reconciled', note: receipt?.reconcileNote || '' });
    }
  }, [open, receipt, form]);

  const save = async () => {
    const v = await form.validateFields();
    if (v.result === 'reconciled' && !receipt.invoiceFileUrl && !file) {
      message.error('Cần tải file hóa đơn trước khi xác nhận khớp');
      return;
    }
    const fd = new FormData();
    fd.append('result', v.result);
    if (v.note) fd.append('note', v.note);
    if (file) fd.append('file', file);
    setSaving(true);
    try {
      await api.post(`/receipts/${receipt.id}/reconcile`, fd);
      message.success(v.result === 'reconciled' ? 'Đã đối soát khớp và khóa phiếu' : 'Đã ghi nhận chênh lệch');
      onSaved?.();
      onClose();
    } catch (e) {
      message.error(errorMessage(e));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal open={open} title={`Đối soát công nợ – ${receipt?.code || ''}`} okText="Lưu kết quả" cancelText="Hủy"
      confirmLoading={saving} onOk={save} onCancel={onClose} destroyOnClose>
      {receipt && (
        <Typography.Paragraph>
          Tổng phiếu: <b>{money(receipt.totalAmount)}</b> · Đã thanh toán: <b>{money(receipt.paidAmount)}</b> · Còn nợ: <b>{money(receipt.remainingAmount)}</b>
        </Typography.Paragraph>
      )}
      <Form form={form} layout="vertical">
        <Form.Item name="result" label="Kết quả đối chiếu với hóa đơn và minh chứng">
          <Radio.Group>
            <Radio value="reconciled">Khớp – khóa phiếu</Radio>
            <Radio value="has_difference">Có chênh lệch</Radio>
          </Radio.Group>
        </Form.Item>
        <Form.Item noStyle shouldUpdate>
          {({ getFieldValue }) => (
            <Form.Item name="note" label="Ghi chú xử lý"
              rules={getFieldValue('result') === 'has_difference' ? [{ required: true, whitespace: true, message: 'Ghi rõ nội dung chênh lệch' }] : []}>
              <Input.TextArea rows={3} maxLength={255} showCount />
            </Form.Item>
          )}
        </Form.Item>
        <Form.Item label={receipt?.invoiceFileUrl ? 'Thay file hóa đơn (không bắt buộc)' : 'File hóa đơn'}>
          <Upload beforeUpload={(f) => { setFile(f); return false; }} onRemove={() => setFile(null)} maxCount={1}
            fileList={file ? [{ uid: '1', name: file.name, status: 'done' }] : []} accept=".pdf,.png,.jpg,.jpeg,.webp,.xlsx,.xls,.csv">
            <a><UploadOutlined /> Chọn file</a>
          </Upload>
        </Form.Item>
      </Form>
    </Modal>
  );
}
