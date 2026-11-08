import { BrowserRouter, Routes, Route, Navigate, useParams } from 'react-router-dom';
import { CartProvider } from './context/CartContext';
import { Layout } from './components/Layout';
import { ProduktyList } from './components/ProduktyList';
import { ProduktDetails } from './components/ProduktDetails';
import { CartPage } from './components/CartPage';

function LegacyProduktRedirect() {
  const { id } = useParams();
  return <Navigate to={`/produkty/${id}`} replace />;
}

function App() {
  return (
    <CartProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<Layout />}>
            <Route path="/" element={<ProduktyList />} />
            <Route path="/produkty/:id" element={<ProduktDetails />} />
            <Route path="/leki/:id" element={<LegacyProduktRedirect />} />
            <Route path="/koszyk" element={<CartPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </CartProvider>
  );
}

export default App;