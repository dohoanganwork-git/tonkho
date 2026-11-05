import { useEffect, useState } from 'react';
import { App, DatePicker, Form, Input, InputNumber, Modal, Select, Upload, Typography } from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import api, { errorMessage } from '../api/client';
import { money } from '../format';

/**
 * Ghi nhan hoac sua khoan thanh toan cong no (Ke toan).
 * receipt: phieu nhap (de biet so con no). payment: khoan dang sua (neu co).
 */
export default function PaymentModal({ open, receipt, payment, onClose, onSaved }) {
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const [file, setFile] = useState(null);
  const [saving, setSaving] = useState(false);
  const max = receipt ? Number(receipt.remainingAmount) + (payment ? Number(payment.amount) : 0) : 0;

  useEffect(() => {
    if (!open) return;
    setFile(null);
    form.setFieldsValue(
      payment
        ? { amount: Number(payment.amount), paidAt: dayjs(payment.paidAt), method: payment.method, note: payment.note }
        : { amount: max, paidAt: dayjs(), method: 'Chuyển khoản', note: '' },
    );
  }, [open, payment, form, max]);

  const save = async () => {
    const v = await form.validateFields();
    if (!payment && !file) {
      message.error('Cần đính kèm file minh chứng thanh toán');
      return;
    }
    const fd = new FormData();
    fd.append('amount', v.amount);
    fd.append('paidAt', v.paidAt.format('YYYY-MM-DDTHH:mm:ss'));
    fd.append('method', v.method);
    if (v.note) fd.append('note', v.note);
    if (file) fd.append('file', file);
    setSaving(true);
    try {
      if (payment) {
        await api.put(`/payments/${payment.id}`, fd);
      } else {
        fd.append('receiptId', receipt.id);
        await api.post('/payments', fd);
      }
      message.success('Đã lưu khoản thanh toán');
      onSaved?.();
      onClose();
    } catch (e) {
      message.error(errorMessage(e));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal open={open} title={payment ? 'Sửa khoản thanh toán' : `Thanh toán phiếu ${receipt?.code || ''}`}
      okText="Lưu" cancelText="Hủy" confirmLoading={saving} onOk={save} onCancel={onClose} destroyOnClose>
      <Typography.Paragraph type="secondary">Số còn nợ có thể thanh toán: <b>{money(max)}</b></Typography.Paragraph>
      <Form form={form} layout="vertical">
        <Form.Item name="amount" label="Số tiền (đ)" rules={[
          { required: true, message: 'Nhập số tiền' },
          { validator: (_, v) => (v > 0 && v <= max ? Promise.resolve() : Promise.reject(new Error('Số tiền phải lớn hơn 0 và không vượt số còn nợ'))) },
        ]}>
          <InputNumber min={0} step={10000} style={{ width: '100%' }}
            formatter={(v) => `${v ?? ''}`.replace(/\B(?=(\d{3})+(?!\d))/g, '.')} parser={(v) => (v || '').replace(/\./g, '')} />
        </Form.Item>
        <Form.Item name="paidAt" label="Ngày thanh toán" rules={[{ required: true }]}>
          <DatePicker showTime={{ format: 'HH:mm' }} format="DD/MM/YYYY HH:mm" style={{ width: '100%' }} disabledDate={(d) => d.isAfter(dayjs(), 'day')} />
        </Form.Item>
        <Form.Item name="method" label="Hình thức" rules={[{ required: true }]}>
          <Select options={['Chuyển khoản', 'Tiền mặt', 'Khác'].map((v) => ({ value: v, label: v }))} />
        </Form.Item>
        <Form.Item name="note" label="Ghi chú"><Input /></Form.Item>
        <Form.Item label={payment ? 'Thay file minh chứng (không bắt buộc)' : 'File minh chứng (bắt buộc)'} required={!payment}>
          <Upload beforeUpload={(f) => { setFile(f); return false; }} onRemove={() => setFile(null)} maxCount={1}
            fileList={file ? [{ uid: '1', name: file.name, status: 'done' }] : []} accept=".pdf,.png,.jpg,.jpeg,.webp">
            <a><UploadOutlined /> Chọn file ảnh / PDF</a>
          </Upload>
        </Form.Item>
      </Form>
    </Modal>
  );
}
