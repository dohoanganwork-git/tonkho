import { Button, App } from 'antd';
import { PaperClipOutlined } from '@ant-design/icons';
import { openFile, errorMessage } from '../api/client';

/** Nut mo file goc luu tren may chu. */
export default function FileLink({ path, children }) {
  const { message } = App.useApp();
  if (!path) return <span className="muted">Chưa có</span>;
  return (
    <Button
      type="link"
      size="small"
      icon={<PaperClipOutlined />}
      style={{ padding: 0 }}
      onClick={() => openFile(path).catch((e) => message.error(errorMessage(e)))}
    >
      {children || path.split('/').pop()}
    </Button>
  );
}
