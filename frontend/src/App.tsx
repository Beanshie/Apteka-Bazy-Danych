import { BrowserRouter, Routes, Route, Navigate, useParams } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { CartProvider } from './context/CartContext';
import { Layout } from './components/Layout';
import { ProduktyList } from './components/ProduktyList';
import { ProduktDetails } from './components/ProduktDetails';
import { CartPage } from './components/CartPage';
import { CheckoutPage } from './components/CheckoutPage';
import { LoginPage } from './components/LoginPage';
import { RegisterPage } from './components/RegisterPage';

function LegacyProduktRedirect() {
  const { id } = useParams();
  return <Navigate to={`/produkty/${id}`} replace />;
}

function App() {
  return (
    <AuthProvider>
      <CartProvider>
        <BrowserRouter>
          <Routes>
            <Route element={<Layout />}>
              <Route path="/" element={<ProduktyList />} />
              <Route path="/produkty/:id" element={<ProduktDetails />} />
              <Route path="/leki/:id" element={<LegacyProduktRedirect />} />
              <Route path="/koszyk" element={<CartPage />} />
              <Route path="/zamowienie/nowe" element={<CheckoutPage />} />
              <Route path="/login" element={<LoginPage />} />
              <Route path="/rejestracja" element={<RegisterPage />} />
            </Route>
          </Routes>
        </BrowserRouter>
      </CartProvider>
    </AuthProvider>
  );
}

export default App;