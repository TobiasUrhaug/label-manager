import { Routes, Route } from 'react-router-dom';
import AppLayout from './layouts/AppLayout.jsx';
import HomePage from './pages/HomePage.jsx';

export default function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route path="/" element={<HomePage />} />
      </Route>
    </Routes>
  );
}
