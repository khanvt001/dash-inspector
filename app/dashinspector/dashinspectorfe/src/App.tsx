import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout/Layout';
import SharedPreferencePage from './pages/SharedPreferencePage';
import DatabasePage from './pages/DatabasePage';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Navigate to="/shared-preference" replace />} />
          <Route path="shared-preference" element={<SharedPreferencePage />} />
          <Route path="database" element={<DatabasePage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
