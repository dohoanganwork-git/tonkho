import { Alert } from 'antd';

/** Hien danh sach loi chi tiet tra ve tu backend (vd. loi tung dong trong file). */
export default function ErrorList({ error }) {
  if (!error) return null;
  return (
    <Alert
      type="error"
      showIcon
      style={{ marginBottom: 12 }}
      message={error.message}
      description={
        error.details?.length ? (
          <ul style={{ margin: 0, paddingLeft: 18 }}>
            {error.details.map((d, i) => (
              <li key={i}>{d}</li>
            ))}
          </ul>
        ) : null
      }
    />
  );
}
